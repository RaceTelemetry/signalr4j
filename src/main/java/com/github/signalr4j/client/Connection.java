/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

import com.github.signalr4j.client.http.Request;
import com.github.signalr4j.client.transport.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a basic SingalR connection
 */
public class Connection implements ConnectionBase {

    public static final Version PROTOCOL_VERSION = new Version("1.5");

    private final Logger logger;

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

    protected JsonParser jsonParser;

    protected Gson gson;

    private final Object stateLock = new Object();

    private final Object startLock = new Object();

    private boolean reconnectOnError = true;

    /**
     * Initializes the connection with an URL
     * 
     * @param url
     *            The connection URL
     */
    public Connection(String url) {
        this(url, (String) null);
    }

    /**
     * Initializes the connection with an URL and a query string
     * 
     * @param url
     *            The connection URL
     * @param queryString
     *            The connection query string
     */
    public Connection(String url, String queryString) {
        this(url, queryString, new NullLogger());
    }

    /**
     * Initializes the connection with an URL and a logger
     * 
     * @param url
     *            The connection URL
     * @param logger
     *            The connection logger
     */
    public Connection(String url, Logger logger) {
        this(url, null, logger);
    }

    /**
     * Initializes the connection with an URL, a query string and a Logger
     * 
     * @param url
     *            The connection URL
     * @param queryString
     *            The connection query string
     * @param logger
     *            The connection logger
     */
    public Connection(String url, String queryString, Logger logger) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null");
        }

        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        log("Initialize the connection", LogLevel.INFORMATION);
        log("Connection data: " + url + " - " + (queryString == null ? "" : queryString), LogLevel.VERBOSE);

        this.url = url;
        this.queryString = queryString;
        this.logger = logger;
        jsonParser = new JsonParser();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new DateSerializer());
        gson = gsonBuilder.create();
        state = ConnectionState.DISCONNECTED;
    }

    @Override
    public Logger getLogger() {
        return logger;
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
        return start(new AutomaticTransport(logger));
    }

    /**
     * Sends a serialized object
     * 
     * @param object
     *            The object to send. If the object is a JsonElement, its string
     *            representation is sent. Otherwise, the object is serialized to
     *            Json.
     * @return A Future for the operation
     */
    public SignalRFuture<Void> send(Object object) {
        String data = null;
        if (object != null) {
            if (object instanceof JsonElement) {
                data = object.toString();
            } else {
                data = gson.toJson(object);
            }
        }

        return send(data);
    }

    @Override
    public SignalRFuture<Void> send(String data) {
        log("Sending: " + data, LogLevel.INFORMATION);

        if (state == ConnectionState.DISCONNECTED || state == ConnectionState.CONNECTING) {
            onError(new InvalidStateException(state), false);
            return new SignalRFuture<>();
        }

        final Connection that = this;

        log("Invoking send on transport", LogLevel.VERBOSE);
        SignalRFuture<Void> future = transport.send(this, data, that::processReceivedData);

        handleFutureError(future, false);
        return future;
    }

    /**
     * Handles a Future error, invoking the connection onError event
     * 
     * @param future
     *            The future to handle
     * @param mustCleanCurrentConnection
     *            True if the connection must be cleaned when an error happens
     */
    private void handleFutureError(SignalRFuture<?> future, final boolean mustCleanCurrentConnection) {
        final Connection that = this;

        future.onError(error -> that.onError(error, mustCleanCurrentConnection));
    }

    @Override
    public SignalRFuture<Void> start(final ClientTransport transport) {
        synchronized (startLock) {
            log("Entered startLock in start", LogLevel.VERBOSE);
            if (!changeState(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING)) {
                log("Couldn't change state from disconnected to connecting.", LogLevel.VERBOSE);
                return connectionFuture;
            }

            log("Start the connection, using " + transport.getName() + " transport", LogLevel.INFORMATION);

            this.transport = transport;
            connectionFuture = new UpdateableCancellableFuture<>(null);
            handleFutureError(connectionFuture, true);

            log("Start negotiation", LogLevel.VERBOSE);
            SignalRFuture<NegotiationResponse> negotiationFuture = transport.negotiate(this);

            try {
                negotiationFuture.done(negotiationResponse -> {
                    log("Negotiation completed", LogLevel.INFORMATION);
                    if (!verifyProtocolVersion(negotiationResponse.getProtocolVersion())) {
                        Exception err = new InvalidProtocolVersionException(negotiationResponse.getProtocolVersion());
                        onError(err, true);
                        connectionFuture.triggerError(err);
                        return;
                    }

                    connectionId = negotiationResponse.getConnectionId();
                    connectionToken = negotiationResponse.getConnectionToken();
                    log("ConnectionId: " + connectionId, LogLevel.VERBOSE);
                    log("ConnectionToken: " + connectionToken, LogLevel.VERBOSE);

                    KeepAliveData keepAliveData = null;
                    if (negotiationResponse.getKeepAliveTimeout() > 0) {
                        log("Keep alive timeout: " + negotiationResponse.getKeepAliveTimeout(), LogLevel.VERBOSE);
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
     * @param oldState
     *            The expected old state
     * @param newState
     *            The new state
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
            log("Preparing request with credentials data", LogLevel.INFORMATION);
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
            log("Entered startLock in stop", LogLevel.VERBOSE);
            if (aborting) {
                log("Abort already started.", LogLevel.VERBOSE);
                return;
            }

            if (state == ConnectionState.DISCONNECTED) {
                log("Connection already in disconnected state. Exiting abort", LogLevel.VERBOSE);
                return;
            }

            log("Stopping the connection", LogLevel.INFORMATION);
            aborting = true;

            log("Starting abort operation", LogLevel.VERBOSE);
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
                    log("Abort cancelled", LogLevel.VERBOSE);
                    aborting = false;
                }
            });

            abortFuture.done(obj -> {
                synchronized (startLock) {
                    log("Abort completed", LogLevel.INFORMATION);
                    disconnect();
                    aborting = false;
                }
            });
        }
    }

    @Override
    public void disconnect() {
        synchronized (stateLock) {
            log("Entered stateLock in disconnect", LogLevel.VERBOSE);

            if (state == ConnectionState.DISCONNECTED) {
                return;
            }

            log("Disconnecting", LogLevel.INFORMATION);
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
                log("Stopping Heartbeat monitor", LogLevel.VERBOSE);
                heartbeatMonitor.stop();
            }

            heartbeatMonitor = null;

            if (connectionFuture != null) {
                log("Stopping the connection", LogLevel.VERBOSE);
                connectionFuture.cancel();
                connectionFuture = new UpdateableCancellableFuture<>(null);
            }

            if (abortFuture != null) {
                log("Cancelling abort", LogLevel.VERBOSE);
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

    @Override
    public Gson getGson() {
        return gson;
    }

    @Override
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    @Override
    public JsonParser getJsonParser() {
        return jsonParser;
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
     * @param versionString
     *            String representing a Version
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
     * @param keepAliveData
     *            Keep Alive data for heartbeat monitor
     * @param isReconnecting
     *            True if is reconnecting
     */
    private void startTransport(KeepAliveData keepAliveData, final boolean isReconnecting) {
        synchronized (startLock) {
            log("Entered startLock in startTransport", LogLevel.VERBOSE);
            // if the connection was closed before this callback, just return;
            if (transport == null) {
                log("Transport is null. Exiting startTransport", LogLevel.VERBOSE);
                return;
            }

            log("Starting the transport", LogLevel.INFORMATION);
            if (isReconnecting) {
                if (heartbeatMonitor != null) {
                    log("Stopping heartbeat monitor", LogLevel.VERBOSE);
                    heartbeatMonitor.stop();
                }

                transport.abort(this);

                onReconnecting();
            }

            heartbeatMonitor = new HeartbeatMonitor();

            heartbeatMonitor.setOnWarning(() -> {
                log("Slow connection detected", LogLevel.INFORMATION);
                if (onConnectionSlow != null) {
                    onConnectionSlow.run();
                }
            });

            heartbeatMonitor.setOnTimeout(() -> {
                log("Timeout", LogLevel.INFORMATION);
                if (reconnectOnError)
                    reconnect();
                else
                    disconnect();
            });

            final Connection that = this;

            ConnectionType connectionType = isReconnecting ? ConnectionType.RECONNECTION : ConnectionType.INITIAL_CONNECTION;

            log("Starting transport for " + connectionType, LogLevel.VERBOSE);
            SignalRFuture<Void> future = transport.start(this, connectionType, data -> {
                log("Received data: ", LogLevel.VERBOSE);
                processReceivedData(data);
            });

            handleFutureError(future, true);

            connectionFuture.setFuture(future);
            future.onError(error -> connectionFuture.triggerError(error));

            this.keepAliveData = keepAliveData;

            try {
                future.done(obj -> {
                    synchronized (startLock) {
                        log("Entered startLock after transport was started", LogLevel.VERBOSE);
                        log("Current state: " + state, LogLevel.VERBOSE);
                        if (changeState(ConnectionState.RECONNECTING, ConnectionState.CONNECTED)) {

                            log("Starting Heartbeat monitor", LogLevel.VERBOSE);
                            heartbeatMonitor.start(this.keepAliveData, that);

                            log("Reconnected", LogLevel.INFORMATION);
                            onReconnected();

                        } else if (changeState(ConnectionState.CONNECTING, ConnectionState.CONNECTED)) {

                            log("Starting Heartbeat monitor", LogLevel.VERBOSE);
                            heartbeatMonitor.start(this.keepAliveData, that);

                            log("Connected", LogLevel.INFORMATION);
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
     * @param data
     *            The received data
     */
    private void processReceivedData(String data) {
        if (heartbeatMonitor != null) {
            heartbeatMonitor.beat();
        }

        MessageResult result = TransportHelper.processReceivedData(data, this);

        if (result.disconnect()) {
            disconnect();
            return;
        }

        if (result.reconnect()) {
            reconnect();
        }
    }

    /**
     * Processes a received message
     * 
     * @param message
     *            The message to process
     * @return The processed message
     * @throws Exception
     *             An exception could be thrown if there an error while
     *             processing the message
     */
    protected JsonElement processMessage(JsonElement message) throws Exception {
        return message;
    }

    @Override
    public void onError(Throwable error, boolean mustCleanCurrentConnection) {

        if(error != null)
            log(error);

        if (mustCleanCurrentConnection) {
            if ((state == ConnectionState.CONNECTED || state == ConnectionState.RECONNECTING) && reconnectOnError) {
                log("Triggering reconnect", LogLevel.VERBOSE);
                reconnect();
            } else {
                log("Triggering disconnect", LogLevel.VERBOSE);
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
            log("Stopping Heartbeat monitor", LogLevel.VERBOSE);
            heartbeatMonitor.stop();
            log("Restarting the transport", LogLevel.INFORMATION);

            // if it is reconnecting and the connection cannot yet be established
            // therefore the mHeartbeatMonitor instance wouldn't be initialized with the KeepAliveData value
            KeepAliveData keepAliveData = heartbeatMonitor.getKeepAliveData();
            if (keepAliveData == null && this.keepAliveData != null)
                keepAliveData = this.keepAliveData;

            startTransport(keepAliveData, true);
        }
    }

    protected void log(String message, LogLevel level) {
        if (message != null & logger != null) {
            logger.log(getSourceNameForLog() + " - " + message, level);
        }
    }

    protected void log(Throwable error) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        logger.log(getSourceNameForLog() + " - Error: \n" + sw, LogLevel.CRITICAL);
    }

    protected String getSourceNameForLog() {
        return "Connection";
    }

    @Override
    public void onReceived(JsonElement message) {
        if (onReceived != null && getState() == ConnectionState.CONNECTED) {
            log("Invoking messageReceived with: " + message, LogLevel.VERBOSE);
            try {
                onReceived.onMessageReceived(message);
            } catch (Throwable error) {
                onError(error, false);
            }
        }
    }
}
