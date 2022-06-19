/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tel.race.signalr.client.http.Request;
import tel.race.signalr.client.transport.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a basic SingalR connection
 */
public class Connection implements ConnectionBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

    public static final JsonMapper MAPPER = new JsonMapper();

    public static final Version PROTOCOL_VERSION = new Version("1.5");

    private final String url;

    private String connectionToken;

    private String connectionId;

    private String messageId;

    private String groupsToken;

    private Credentials credentials;

    private final String queryString;

    private final Map<String, String> headers = new HashMap<>();

    private UpdateableCancellableFuture<Void> connectionFuture;

    private boolean aborting = false;

    private SignalRFuture<Void> abortFuture = new SignalRFuture<>();

    private Runnable onReconnecting;

    private Runnable onReconnected;

    private Runnable onConnected;

    private MessageReceivedHandler onReceived;

    private ErrorCallback onError;

    private Runnable onConnectionSlow;

    private Runnable onClosed;

    private StateChangedCallback onStateChanged;

    private ClientTransport transport;

    private HeartbeatMonitor heartbeatMonitor;

    private KeepAliveData keepAliveData;

    protected ConnectionState state;

    private final Object stateLock = new Object();

    private final Object startLock = new Object();

    private boolean reconnectOnError = true;

    /**
     * Initializes the connection with an URL
     *
     * @param url The connection URL
     */
    public Connection(String url) {
        this(url, null);
    }

    /**
     * Initializes the connection with an URL, a query string and a Logger
     *
     * @param url         The connection URL
     * @param queryString The connection query string
     */
    public Connection(String url, String queryString) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null");
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        LOGGER.debug("Initialize the connection");
        LOGGER.trace("Connection data: {} - {}", url, queryString == null ? "" : queryString);

        this.url = url;
        this.queryString = queryString;

        state = ConnectionState.DISCONNECTED;
    }

    @Override
    public ConnectionState getState() {
        return state;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getConnectionToken() {
        return connectionToken;
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public void setReconnectOnError(boolean reconnectOnError) {
        this.reconnectOnError = reconnectOnError;
    }

    @Override
    public String getGroupsToken() {
        return groupsToken;
    }

    @Override
    public void setGroupsToken(String groupsToken) {
        this.groupsToken = groupsToken;
    }

    @Override
    public void addHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void reconnecting(Runnable handler) {
        onReconnecting = handler;
    }

    @Override
    public void reconnected(Runnable handler) {
        onReconnected = handler;
    }

    @Override
    public void connected(Runnable handler) {
        onConnected = handler;
    }

    @Override
    public void error(ErrorCallback handler) {
        onError = handler;
    }

    @Override
    public void received(MessageReceivedHandler handler) {
        onReceived = handler;
    }

    @Override
    public void connectionSlow(Runnable handler) {
        onConnectionSlow = handler;
    }

    @Override
    public void closed(Runnable handler) {
        onClosed = handler;
    }

    @Override
    public void stateChanged(StateChangedCallback handler) {
        onStateChanged = handler;
    }

    /**
     * Starts the connection using the best available transport
     *
     * @return A Future for the operation
     */
    public SignalRFuture<Void> start() {
        return start(new AutomaticTransport());
    }

    /**
     * Sends a serialized object
     *
     * @param object The object to send. If the object is a JsonNode, its string
     *               representation is sent. Otherwise, the object is serialized to
     *               Json.
     * @return A Future for the operation
     */
    public SignalRFuture<Void> send(Object object) {
        String data = null;
        if (object != null) {
            if (object instanceof JsonNode) {
                data = object.toString();
            } else {
                data = MAPPER.valueToTree(object).toString();
            }
        }

        return send(data);
    }

    @Override
    public SignalRFuture<Void> send(String data) {
        LOGGER.debug("Sending: {}", data);

        if (state == ConnectionState.DISCONNECTED || state == ConnectionState.CONNECTING) {
            onError(new InvalidStateException(state), false);
            return new SignalRFuture<>();
        }

        final Connection that = this;

        LOGGER.trace("Invoking send on transport");
        SignalRFuture<Void> future = transport.send(this, data, that::processReceivedData);

        handleFutureError(future, false);
        return future;
    }

    /**
     * Handles a Future error, invoking the connection onError event
     *
     * @param future                     The future to handle
     * @param mustCleanCurrentConnection True if the connection must be cleaned when an error happens
     */
    private void handleFutureError(SignalRFuture<?> future, final boolean mustCleanCurrentConnection) {
        final Connection that = this;

        future.onError(error -> that.onError(error, mustCleanCurrentConnection));
    }

    @Override
    public SignalRFuture<Void> start(final ClientTransport transport) {
        synchronized (startLock) {
            LOGGER.trace("Entered startLock in start");
            if (!changeState(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING)) {
                LOGGER.trace("Couldn't change state from disconnected to connecting.");
                return connectionFuture;
            }

            LOGGER.debug("Start the connection, using " + transport.getName() + " transport");

            this.transport = transport;
            connectionFuture = new UpdateableCancellableFuture<>(null);
            handleFutureError(connectionFuture, true);

            LOGGER.trace("Start negotiation");
            SignalRFuture<NegotiationResponse> negotiationFuture = transport.negotiate(this);

            try {
                negotiationFuture.done(negotiationResponse -> {
                    LOGGER.debug("Negotiation completed");
                    if (!verifyProtocolVersion(negotiationResponse.getProtocolVersion())) {
                        Exception err = new InvalidProtocolVersionException(negotiationResponse.getProtocolVersion());
                        onError(err, true);
                        connectionFuture.triggerError(err);
                        return;
                    }

                    connectionId = negotiationResponse.getConnectionId();
                    connectionToken = negotiationResponse.getConnectionToken();
                    LOGGER.trace("ConnectionId: {}", connectionId);
                    LOGGER.trace("ConnectionToken: {}", connectionToken);

                    KeepAliveData keepAliveData = null;
                    if (negotiationResponse.getKeepAliveTimeout() > 0) {
                        LOGGER.trace("Keep alive timeout: {}", negotiationResponse.getKeepAliveTimeout());
                        keepAliveData = new KeepAliveData((long) (negotiationResponse.getKeepAliveTimeout() * 1000));
                    }

                    startTransport(keepAliveData, false);
                });

                negotiationFuture.onError(error -> connectionFuture.triggerError(error));

            } catch (Exception e) {
                onError(e, true);
            }

            handleFutureError(negotiationFuture, true);
            connectionFuture.setFuture(negotiationFuture);

            return connectionFuture;
        }
    }

    /**
     * Changes the connection state
     *
     * @param oldState The expected old state
     * @param newState The new state
     * @return True, if the state was changed
     */
    private boolean changeState(ConnectionState oldState, ConnectionState newState) {
        synchronized (stateLock) {
            if (state == oldState) {
                state = newState;
                if (onStateChanged != null) {
                    try {
                        onStateChanged.stateChanged(oldState, newState);
                    } catch (Throwable e) {
                        onError(e, false);
                    }
                }
                return true;
            }

            return false;
        }
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void prepareRequest(Request request) {
        if (credentials != null) {
            LOGGER.debug("Preparing request with credentials data");
            credentials.prepareRequest(request);
        }
    }

    @Override
    public String getConnectionData() {
        return null;
    }

    @Override
    public void stop() {
        synchronized (startLock) {
            LOGGER.trace("Entered startLock in stop");
            if (aborting) {
                LOGGER.trace("Abort already started.");
                return;
            }

            if (state == ConnectionState.DISCONNECTED) {
                LOGGER.trace("Connection already in disconnected state. Exiting abort");
                return;
            }

            LOGGER.debug("Stopping the connection");
            aborting = true;

            LOGGER.trace("Starting abort operation");
            abortFuture = transport.abort(this);

            final Connection that = this;
            abortFuture.onError(error -> {
                synchronized (startLock) {
                    that.onError(error, false);
                    disconnect();
                    aborting = false;
                }
            });

            abortFuture.onCancelled(() -> {
                synchronized (startLock) {
                    LOGGER.trace("Abort cancelled");
                    aborting = false;
                }
            });

            abortFuture.done(obj -> {
                synchronized (startLock) {
                    LOGGER.debug("Abort completed");
                    disconnect();
                    aborting = false;
                }
            });
        }
    }

    @Override
    public void disconnect() {
        synchronized (stateLock) {
            LOGGER.trace("Entered stateLock in disconnect");

            if (state == ConnectionState.DISCONNECTED) {
                return;
            }

            LOGGER.debug("Disconnecting");
            ConnectionState oldState = state;
            state = ConnectionState.DISCONNECTED;
            if (onStateChanged != null) {
                try {
                    onStateChanged.stateChanged(oldState, ConnectionState.DISCONNECTED);
                } catch (Throwable e) {
                    onError(e, false);
                }
            }

            if (heartbeatMonitor != null) {
                LOGGER.trace("Stopping Heartbeat monitor");
                heartbeatMonitor.stop();
            }

            heartbeatMonitor = null;

            if (connectionFuture != null) {
                LOGGER.trace("Stopping the connection");
                connectionFuture.cancel();
                connectionFuture = new UpdateableCancellableFuture<>(null);
            }

            if (abortFuture != null) {
                LOGGER.trace("Cancelling abort");
                abortFuture.cancel();
            }

            connectionId = null;
            connectionToken = null;
            credentials = null;
            groupsToken = null;
            headers.clear();
            messageId = null;
            transport = null;

        }
        onClosed();
    }

    /**
     * Triggers the Reconnecting event
     */
    protected void onReconnecting() {
        changeState(ConnectionState.CONNECTED, ConnectionState.RECONNECTING);

        if (onReconnecting != null) {
            onReconnecting.run();
        }
    }

    /**
     * Triggers the Reconnected event
     */
    protected void onReconnected() {
        if (onReconnected != null) {
            onReconnected.run();
        }
    }

    /**
     * Triggers the Connected event
     */
    protected void onConnected() {
        if (onConnected != null) {
            onConnected.run();
        }
    }

    /**
     * Verifies the protocol version
     *
     * @param versionString String representing a Version
     * @return True if the version is supported.
     */
    private static boolean verifyProtocolVersion(String versionString) {
        try {
            if (versionString == null || versionString.equals("")) {
                return false;
            }

            Version version = new Version(versionString);

            return version.equals(PROTOCOL_VERSION);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Starts the transport
     *
     * @param keepAliveData  Keep Alive data for heartbeat monitor
     * @param isReconnecting True if is reconnecting
     */
    private void startTransport(KeepAliveData keepAliveData, final boolean isReconnecting) {
        synchronized (startLock) {
            LOGGER.trace("Entered startLock in startTransport");
            // if the connection was closed before this callback, just return;
            if (transport == null) {
                LOGGER.trace("Transport is null. Exiting startTransport");
                return;
            }

            LOGGER.debug("Starting the transport");
            if (isReconnecting) {
                if (heartbeatMonitor != null) {
                    LOGGER.trace("Stopping heartbeat monitor");
                    heartbeatMonitor.stop();
                }

                transport.abort(this);

                onReconnecting();
            }

            heartbeatMonitor = new HeartbeatMonitor();

            heartbeatMonitor.setOnWarning(() -> {
                LOGGER.debug("Slow connection detected");
                if (onConnectionSlow != null) {
                    onConnectionSlow.run();
                }
            });

            heartbeatMonitor.setOnTimeout(() -> {
                LOGGER.debug("Timeout");
                if (reconnectOnError)
                    reconnect();
                else
                    disconnect();
            });

            final Connection that = this;

            ConnectionType connectionType = isReconnecting ? ConnectionType.RECONNECTION : ConnectionType.INITIAL_CONNECTION;

            LOGGER.trace("Starting transport for {}", connectionType);
            SignalRFuture<Void> future = transport.start(this, connectionType, data -> {
                LOGGER.trace("Received data: ");
                processReceivedData(data);
            });

            handleFutureError(future, true);

            connectionFuture.setFuture(future);
            future.onError(error -> connectionFuture.triggerError(error));

            this.keepAliveData = keepAliveData;

            try {
                future.done(obj -> {
                    synchronized (startLock) {
                        LOGGER.trace("Entered startLock after transport was started");
                        LOGGER.trace("Current state: {}", state);
                        if (changeState(ConnectionState.RECONNECTING, ConnectionState.CONNECTED)) {

                            LOGGER.trace("Starting Heartbeat monitor");
                            heartbeatMonitor.start(this.keepAliveData, that);

                            LOGGER.debug("Reconnected");
                            onReconnected();

                        } else if (changeState(ConnectionState.CONNECTING, ConnectionState.CONNECTED)) {

                            LOGGER.trace("Starting Heartbeat monitor");
                            heartbeatMonitor.start(this.keepAliveData, that);

                            LOGGER.debug("Connected");
                            onConnected();
                            connectionFuture.setResult(null);
                        }
                    }
                });
            } catch (Exception e) {
                onError(e, false);
            }
        }
    }

    /**
     * Parses the received data and triggers the OnReceived event
     *
     * @param data The received data
     */
    private void processReceivedData(String data) {
        if (heartbeatMonitor != null) {
            heartbeatMonitor.beat();
        }

        MessageResult result;
        try {
            result = TransportHelper.processReceivedData(data, this);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialized received data", e);
        }

        if (result.disconnect()) {
            disconnect();
            return;
        }

        if (result.reconnect()) {
            reconnect();
        }
    }

    @Override
    public void onError(Throwable error, boolean mustCleanCurrentConnection) {
        if (error != null)
            LOGGER.debug("An error occurred", error);

        if (mustCleanCurrentConnection) {
            if ((state == ConnectionState.CONNECTED || state == ConnectionState.RECONNECTING) && reconnectOnError) {
                LOGGER.trace("Triggering reconnect");
                reconnect();
            } else {
                LOGGER.trace("Triggering disconnect");
                if (onError != null) {
                    onError.onError(error);
                }
                disconnect();
            }
        } else {
            if (onError != null) {
                onError.onError(error);
            }
        }
    }

    /**
     * Triggers the Closed event
     */
    protected void onClosed() {
        if (onClosed != null) {
            onClosed.run();
        }
    }

    /**
     * Stops the heartbeat monitor and re-starts the transport
     */
    private void reconnect() {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.RECONNECTING) {
            LOGGER.trace("Stopping Heartbeat monitor");
            heartbeatMonitor.stop();
            LOGGER.debug("Restarting the transport");

            // if it is reconnecting and the connection cannot yet be established
            // therefore the mHeartbeatMonitor instance wouldn't be initialized with the KeepAliveData value
            KeepAliveData keepAliveData = heartbeatMonitor.getKeepAliveData();
            if (keepAliveData == null && this.keepAliveData != null)
                keepAliveData = this.keepAliveData;

            startTransport(keepAliveData, true);
        }
    }

    protected String getSourceNameForLog() {
        return "Connection";
    }

    @Override
    public void onReceived(JsonNode message) throws JsonProcessingException {
        if (onReceived != null && getState() == ConnectionState.CONNECTED) {
            LOGGER.trace("Invoking messageReceived with: {}", message);
            try {
                onReceived.onMessageReceived(message);
            } catch (Throwable error) {
                onError(error, false);
            }
        }
    }
}
