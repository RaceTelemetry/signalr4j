/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.github.signalr4j.client.*;
import com.github.signalr4j.client.http.HttpConnection;
import com.github.signalr4j.client.http.HttpConnectionFuture;
import com.github.signalr4j.client.http.Request;

/**
 * HttpClientTransport implementation over long polling
 */
public class LongPollingTransport extends HttpClientTransport {
    private UpdateableCancellableFuture<Void> connectionFuture;
    private final Object pollSync = new Object();

    /**
     * Initializes the transport
     *
     * @param logger logger to log actions
     */
    public LongPollingTransport(Logger logger) {
        super(logger);
    }

    /**
     * Initializes the transport with a logger
     * 
     * @param logger
     *            Logger to log actions
     * @param httpConnection
     *            HttpConnection for the transport
     */
    public LongPollingTransport(Logger logger, HttpConnection httpConnection) {
        super(logger, httpConnection);
    }

    @Override
    public String getName() {
        return "longPolling";
    }

    @Override
    public boolean supportKeepAlive() {
        return false;
    }

    @Override
    public SignalRFuture<Void> start(ConnectionBase connection, ConnectionType connectionType, DataResultCallback callback) {
        return poll(connection, connectionType == ConnectionType.INITIAL_CONNECTION ? "connect" : "reconnect", callback);
    }

    /**
     * Polls the server
     * 
     * @param connection
     *            the implemented connection
     * @param connectionUrl
     *            the connection action url
     * @param callback
     *            callback to invoke when data is received
     * @return Future for the operation
     */
    private SignalRFuture<Void> poll(final ConnectionBase connection, final String connectionUrl, final DataResultCallback callback) {
        synchronized (pollSync) {
            log("Start the communication with the server", LogLevel.INFORMATION);
            String url = connection.getUrl() + connectionUrl + TransportHelper.getReceiveQueryString(this, connection);

            Request get = new Request(Constants.HTTP_GET);

            get.setUrl(url);
            get.setHeaders(connection.getHeaders());

            connection.prepareRequest(get);

            log("Execute the request", LogLevel.VERBOSE);
            connectionFuture = new UpdateableCancellableFuture<>(null);

            final HttpConnectionFuture future = httpConnection.execute(get, response -> {
                synchronized (pollSync) {
                    try {
                        throwOnInvalidStatusCode(response);

                        if (!"poll".equals(connectionUrl)) {
                            connectionFuture.setResult(null);
                        }
                        log("Response received", LogLevel.VERBOSE);

                        log("Read response to the end", LogLevel.VERBOSE);
                        String responseData = response.readToEnd();
                        if (responseData != null) {
                            responseData = responseData.trim();
                        }

                        log("Trigger onData with data: " + responseData, LogLevel.VERBOSE);
                        callback.onData(responseData);

                        if (!connectionFuture.isCancelled() && connection.getState() == ConnectionState.CONNECTED) {
                            log("Continue polling", LogLevel.VERBOSE);
                            connectionFuture.setFuture(poll(connection, "poll", callback));
                        }
                    } catch (Throwable e) {
                        if (!connectionFuture.isCancelled()) {
                            log(e);
                            connectionFuture.triggerError(e);
                        }
                    }
                }
            });

            future.onTimeout(error -> {
                synchronized (pollSync) {
                    if (connectionUrl.equals("poll")) {
                        // if the poll request timed out, it should re-poll
                        connectionFuture.setFuture(poll(connection, "poll", callback));
                    } else {
                        future.triggerError(error);
                    }
                }
            });

            future.onError(error -> {
                synchronized (pollSync) {
                    connectionFuture.triggerError(error);
                }
            });

            connectionFuture.setFuture(future);

            return connectionFuture;
        }
    }
}
