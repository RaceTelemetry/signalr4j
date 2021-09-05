/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.github.signalr4j.client.*;
import com.github.signalr4j.client.http.HttpConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * ClientTransport implementation that selects the best available transport
 */
public class AutomaticTransport extends HttpClientTransport {

    private List<ClientTransport> transports;
    private ClientTransport realTransport;

    /**
     * Initializes the transport with a NullLogger
     */
    public AutomaticTransport() {
        this(new NullLogger());
    }

    /**
     * Initializes the transport with a logger
     * 
     * @param logger
     *            logger to log actions
     */
    public AutomaticTransport(Logger logger) {
        super(logger);
        initialize(logger);
    }

    /**
     * Initializes the transport with a logger and an httpConnection
     * 
     * @param logger
     *            the logger
     * @param httpConnection
     *            the httpConnection
     */
    public AutomaticTransport(Logger logger, HttpConnection httpConnection) {
        super(logger, httpConnection);
        initialize(logger);
    }

    private void initialize(Logger logger) {
        transports = new ArrayList<>();
        transports.add(new WebsocketTransport(logger));
        transports.add(new ServerSentEventsTransport(logger));
        transports.add(new LongPollingTransport(logger));
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

            log(String.format("Auto: Faild to connect using transport %s. %s", currentTransport.getName(), error.toString()), LogLevel.INFORMATION);
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
