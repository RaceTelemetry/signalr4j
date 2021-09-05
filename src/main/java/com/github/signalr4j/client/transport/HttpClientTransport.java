/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.github.signalr4j.client.*;
import com.github.signalr4j.client.http.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ClientTransport base implementation over Http
 */
public abstract class HttpClientTransport implements ClientTransport {
    protected static final int BUFFER_SIZE = 1024;

    protected HttpConnection httpConnection;
    protected boolean startedAbort = false;
    protected SignalRFuture<Void> abortFuture = null;

    private final Logger logger;

    /**
     * Initializes the HttpClientTransport with a logger
     *
     * @param logger logger to log actions
     */
    public HttpClientTransport(Logger logger) {
        this(logger, Platform.createHttpConnection(logger));
    }

    public HttpClientTransport(Logger logger, HttpConnection httpConnection) {
        if (logger == null) {
            throw new IllegalArgumentException("logger");
        }

        this.httpConnection = httpConnection;
        this.logger = logger;
    }

    @Override
    public SignalRFuture<NegotiationResponse> negotiate(final ConnectionBase connection) {
        log("Start the negotiation with the server", LogLevel.INFORMATION);

        String url = connection.getUrl() + "negotiate" + TransportHelper.getNegotiateQueryString(connection);

        Request get = new Request(Constants.HTTP_GET);
        get.setUrl(url);
        get.setHeaders(connection.getHeaders());
        get.setVerb(Constants.HTTP_GET);

        connection.prepareRequest(get);

        final SignalRFuture<NegotiationResponse> negotiationFuture = new SignalRFuture<>();

        log("Execute the request", LogLevel.VERBOSE);
        HttpConnectionFuture connectionFuture = httpConnection.execute(get, response -> {
            try {
                log("Response received", LogLevel.VERBOSE);
                throwOnInvalidStatusCode(response);
                log("Headers: " + response.getHeaders(), LogLevel.VERBOSE);

                log("Read response data to the end", LogLevel.VERBOSE);
                String negotiationContent = response.readToEnd();

                log("Trigger onSuccess with negotiation data: " + negotiationContent, LogLevel.VERBOSE);
                negotiationFuture.setResult(new NegotiationResponse(negotiationContent, connection.getJsonParser()));

                // Set cookies, so we get sent to the right server.
                List<String> cookies = response.getHeader("Set-Cookie");
                if (cookies != null) {
                    cookies = new ArrayList<>(cookies);
                    cookies.removeIf("HttpOnly"::equals);
                    connection.getHeaders().put("Cookie", String.join("; ", cookies));
                }
            } catch (Throwable e) {
                log(e);
                negotiationFuture.triggerError(new NegotiationException("There was a problem in the negotiation with the server", e));
            }
        });

        FutureHelper.copyHandlers(connectionFuture, negotiationFuture);

        return negotiationFuture;
    }

    @Override
    public SignalRFuture<Void> send(ConnectionBase connection, String data, final DataResultCallback callback) {
        try {
            log("Start sending data to the server: " + data, LogLevel.INFORMATION);

            Request post = new Request(Constants.HTTP_POST);
            post.setFormContent("data", data);
            post.setUrl(connection.getUrl() + "send" + TransportHelper.getSendQueryString(this, connection));
            post.setHeaders(connection.getHeaders());
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");

            connection.prepareRequest(post);

            log("Execute the request", LogLevel.VERBOSE);

            return httpConnection.execute(post, response -> {
                log("Response received", LogLevel.VERBOSE);
                throwOnInvalidStatusCode(response);

                log("Read response to the end", LogLevel.VERBOSE);
                String data1 = response.readToEnd();

                if (data1 != null) {
                    log("Trigger onData with data: " + data1, LogLevel.VERBOSE);
                    callback.onData(data1);
                }
            });
        } catch (Throwable e) {
            log(e);

            SignalRFuture<Void> future = new SignalRFuture<>();
            future.triggerError(e);

            return future;
        }
    }

    @Override
    public SignalRFuture<Void> abort(ConnectionBase connection) {
        synchronized (this) {
            if (!startedAbort) {
                log("Started aborting", LogLevel.INFORMATION);
                startedAbort = true;
                try {
                    String url = connection.getUrl() + "abort" + TransportHelper.getSendQueryString(this, connection);

                    Request post = new Request(Constants.HTTP_POST);

                    post.setUrl(url);
                    post.setHeaders(connection.getHeaders());

                    connection.prepareRequest(post);

                    log("Execute request", LogLevel.VERBOSE);
                    abortFuture = httpConnection.execute(post, response -> {
                        log("Finishing abort", LogLevel.VERBOSE);
                        startedAbort = false;
                    });

                    return abortFuture;

                } catch (Throwable e) {
                    log(e);
                    log("Finishing abort", LogLevel.VERBOSE);
                    startedAbort = false;

                    SignalRFuture<Void> future = new SignalRFuture<>();
                    future.triggerError(e);

                    return future;
                }
            } else {
                return abortFuture;
            }
        }
    }

    protected void throwOnInvalidStatusCode(Response response) throws InvalidHttpStatusCodeException {
        if (response.getStatus() < 200 || response.getStatus() > 299) {
            String responseContent;

            try {
                responseContent = response.readToEnd();
            } catch (IOException e) {
                responseContent = "";
            }

            StringBuilder headersString = new StringBuilder();

            for (String header : response.getHeaders().keySet()) {
                headersString.append("[");
                headersString.append(header);
                headersString.append(": ");
                for (String headerValue : response.getHeader(header)) {
                    headersString.append(headerValue);
                    headersString.append("; ");
                }
                headersString.append("]; ");
            }

            throw new InvalidHttpStatusCodeException(response.getStatus(), responseContent, headersString.toString());
        }

    }

    protected void log(String message, LogLevel level) {
        logger.log(getName() + " - " + message, level);
    }

    protected void log(Throwable error) {
        logger.log(getName() + " - Error: " + error.toString(), LogLevel.CRITICAL);
    }

}
