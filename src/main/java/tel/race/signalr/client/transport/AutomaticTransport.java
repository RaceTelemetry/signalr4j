/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.transport;

import tel.race.signalr.client.ConnectionBase;
import tel.race.signalr.client.ErrorCallback;
import tel.race.signalr.client.SignalRFuture;
import tel.race.signalr.client.http.HttpConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * ClientTransport implementation that selects the best available transport
 */
public class AutomaticTransport extends HttpClientTransport {

    private final List<ClientTransport> transports = new ArrayList<>();
    private ClientTransport realTransport;


    /**
     * Initializes the transport with a logger
     */
    public AutomaticTransport() {
        super();
        initialize();
    }

    /**
     * Initializes the transport with a logger and an httpConnection
     *
     * @param httpConnection the httpConnection
     */
    public AutomaticTransport(HttpConnection httpConnection) {
        super(httpConnection);
        initialize();
    }

    private void initialize() {
        transports.add(new WebsocketTransport());
        transports.add(new ServerSentEventsTransport());
        transports.add(new LongPollingTransport());
    }

    @Override
    public String getName() {
        if (realTransport == null) {
            return "AutomaticTransport";
        }

        return realTransport.getName();
    }

    @Override
    public boolean supportKeepAlive() {
        if (realTransport != null) {
            return realTransport.supportKeepAlive();
        }

        return false;
    }

    private void resolveTransport(final ConnectionBase connection, final ConnectionType connectionType, final DataResultCallback callback,
                                  final int currentTransportIndex, final SignalRFuture<Void> startFuture) {
        final ClientTransport currentTransport = transports.get(currentTransportIndex);

        final SignalRFuture<Void> transportStart = currentTransport.start(connection, connectionType, callback);

        transportStart.done(obj -> {
            // set the real transport and trigger end the start future
            realTransport = currentTransport;
            startFuture.setResult(null);
        });

        final ErrorCallback handleError = error -> {

            // if the transport is already started, forward the error
            if (realTransport != null) {
                startFuture.triggerError(error);
                return;
            }

            LOGGER.debug("Auto: Failed to connect using transport {}. {}", currentTransport.getName(), error.toString());
            int next = currentTransportIndex + 1;
            if (next < transports.size()) {
                resolveTransport(connection, connectionType, callback, next, startFuture);
            } else {
                startFuture.triggerError(error);
            }
        };

        transportStart.onError(handleError);

        startFuture.onCancelled(() -> {
            // if the transport is already started, forward the cancellation
            if (realTransport != null) {
                transportStart.cancel();
                return;
            }

            handleError.onError(new Exception("Operation cancelled"));
        });
    }

    @Override
    public SignalRFuture<Void> start(final ConnectionBase connection, final ConnectionType connectionType, final DataResultCallback callback) {
        SignalRFuture<Void> startFuture = new SignalRFuture<>();

        resolveTransport(connection, connectionType, callback, 0, startFuture);

        return startFuture;
    }

    @Override
    public SignalRFuture<Void> send(ConnectionBase connection, String data, DataResultCallback callback) {
        if (realTransport != null) {
            return realTransport.send(connection, data, callback);
        }

        return null;
    }

    @Override
    public SignalRFuture<Void> abort(ConnectionBase connection) {
        if (realTransport != null) {
            return realTransport.abort(connection);
        }

        return null;
    }
}
