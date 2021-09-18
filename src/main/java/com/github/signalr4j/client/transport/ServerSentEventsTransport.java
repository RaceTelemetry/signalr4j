/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.github.signalr4j.client.ConnectionBase;
import com.github.signalr4j.client.Constants;
import com.github.signalr4j.client.SignalRFuture;
import com.github.signalr4j.client.http.HttpConnection;
import com.github.signalr4j.client.http.Request;

/**
 * HttpClientTransport implementation over Server Sent Events implementation
 */
public class ServerSentEventsTransport extends HttpClientTransport {

    private static final int SSE_DATA_PREFIX_LENGTH = 6;
    private static final String DATA_INITIALIZED = "data: initialized";
    private static final String END_OF_SSE_MESSAGE = "\n\n";

    private SignalRFuture<Void> connectionFuture;

    /**
     * Initializes the transport with a logger
     */
    public ServerSentEventsTransport() {
        super();
    }

    /**
     * Initializes the transport with a logger
     *
     * @param httpConnection HttpConnection for the transport
     */
    public ServerSentEventsTransport(HttpConnection httpConnection) {
        super(httpConnection);
    }

    @Override
    public String getName() {
        return "serverSentEvents";
    }

    @Override
    public boolean supportKeepAlive() {
        return true;
    }

    @Override
    public SignalRFuture<Void> start(ConnectionBase connection, ConnectionType connectionType, final DataResultCallback callback) {
        LOGGER.debug("Start the communication with the server");
        String url = connection.getUrl() + (connectionType == ConnectionType.INITIAL_CONNECTION ? "connect" : "reconnect")
                + TransportHelper.getReceiveQueryString(this, connection);

        Request get = new Request(Constants.HTTP_GET);

        get.setUrl(url);
        get.setHeaders(connection.getHeaders());
        get.addHeader("Accept", "text/event-stream");

        connection.prepareRequest(get);

        LOGGER.trace("Execute the request");
        connectionFuture = httpConnection.execute(get, response -> {
            try {
                LOGGER.trace("Response received");
                throwOnInvalidStatusCode(response);

                connectionFuture.setResult(null);

                StringBuilder buffer = new StringBuilder();
                String line;

                LOGGER.trace("Read the response content by line");
                while ((line = response.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                    String currentData = buffer.toString();
                    if (currentData.endsWith(END_OF_SSE_MESSAGE)) {
                        currentData = currentData.trim();
                        LOGGER.trace("Found new data: {}", currentData);
                        if (currentData.equals(DATA_INITIALIZED)) {
                            LOGGER.trace("Initialization message found");
                        } else {
                            String content = currentData.substring(SSE_DATA_PREFIX_LENGTH).trim();

                            LOGGER.trace("Trigger onData: {}", content);
                            callback.onData(content);
                        }

                        buffer = new StringBuilder();
                    }
                }

                // if the request finishes, it means the connection was finalized
            } catch (Throwable e) {
                if (!connectionFuture.isCancelled()) {
                    connectionFuture.triggerError(e);
                }
            }
        });

        return connectionFuture;
    }
}
