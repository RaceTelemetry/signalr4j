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
     */
    public LongPollingTransport() {
        super();
    }

    /**
     * Initializes the transport with a logger
     *
     * @param httpConnection HttpConnection for the transport
     */
    public LongPollingTransport(HttpConnection httpConnection) {
        super(httpConnection);
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
     * @param connection    the implemented connection
     * @param connectionUrl the connection action url
     * @param callback      callback to invoke when data is received
     * @return Future for the operation
     */
    private SignalRFuture<Void> poll(final ConnectionBase connection, final String connectionUrl, final DataResultCallback callback) {
        synchronized (pollSync) {
            LOGGER.debug("Start the communication with the server");
            String url = connection.getUrl() + connectionUrl + TransportHelper.getReceiveQueryString(this, connection);

            Request get = new Request(Constants.HTTP_GET);

            get.setUrl(url);
            get.setHeaders(connection.getHeaders());

            connection.prepareRequest(get);

            LOGGER.trace("Execute the request");
            connectionFuture = new UpdateableCancellableFuture<>(null);

            final HttpConnectionFuture future = httpConnection.execute(get, response -> {
                synchronized (pollSync) {
                    try {
                        throwOnInvalidStatusCode(response);

                        if (!"poll".equals(connectionUrl)) {
                            connectionFuture.setResult(null);
                        }
                        LOGGER.trace("Response received");

                        LOGGER.trace("Read response to the end");
                        String responseData = response.readToEnd();
                        if (responseData != null) {
                            responseData = responseData.trim();
                        }

                        LOGGER.trace("Trigger onData with data: {}", responseData);
                        callback.onData(responseData);

                        if (!connectionFuture.isCancelled() && connection.getState() == ConnectionState.CONNECTED) {
                            LOGGER.trace("Continue polling");
                            connectionFuture.setFuture(poll(connection, "poll", callback));
                        }
                    } catch (Throwable e) {
                        if (!connectionFuture.isCancelled()) {
                            LOGGER.debug("Error whilst polling", e);
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
