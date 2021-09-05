/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.github.signalr4j.client.*;
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
     * 
     * @param logger
     *            Logger to log actions
     */
    public ServerSentEventsTransport(Logger logger) {
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
    public ServerSentEventsTransport(Logger logger, HttpConnection httpConnection) {
        super(logger, httpConnection);
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
        log("Start the communication with the server", LogLevel.INFORMATION);
        String url = connection.getUrl() + (connectionType == ConnectionType.INITIAL_CONNECTION ? "connect" : "reconnect")
                + TransportHelper.getReceiveQueryString(this, connection);

        Request get = new Request(Constants.HTTP_GET);

        get.setUrl(url);
        get.setHeaders(connection.getHeaders());
        get.addHeader("Accept", "text/event-stream");

        connection.prepareRequest(get);

        log("Execute the request", LogLevel.VERBOSE);
        connectionFuture = httpConnection.execute(get, response -> {
            try {
                log("Response received", LogLevel.VERBOSE);
                throwOnInvalidStatusCode(response);

                connectionFuture.setResult(null);

                StringBuilder buffer = new StringBuilder();
                String line;

                log("Read the response content by line", LogLevel.VERBOSE);
                while ((line = response.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                    String currentData = buffer.toString();
                    if (currentData.endsWith(END_OF_SSE_MESSAGE)) {
                        currentData = currentData.trim();
                        log("Found new data: " + currentData, LogLevel.VERBOSE);
                        if (currentData.equals(DATA_INITIALIZED)) {
                            log("Initialization message found", LogLevel.VERBOSE);
                        } else {
                            String content = currentData.substring(SSE_DATA_PREFIX_LENGTH).trim();

                            log("Trigger onData: " + content, LogLevel.VERBOSE);
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
