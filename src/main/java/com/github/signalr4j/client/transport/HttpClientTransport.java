/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.github.signalr4j.client.*;
import com.github.signalr4j.client.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ClientTransport base implementation over Http
 */
public abstract class HttpClientTransport implements ClientTransport {

    protected static final Logger LOGGER = LoggerFactory.getLogger(HttpClientTransport.class);
    protected static final int BUFFER_SIZE = 1024;

    protected HttpConnection httpConnection;
    protected boolean startedAbort = false;
    protected SignalRFuture<Void> abortFuture = null;

    /**
     * Initializes the HttpClientTransport
     */
    public HttpClientTransport() {
        this(Platform.createHttpConnection());
    }

    public HttpClientTransport(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }

    @Override
    public SignalRFuture<NegotiationResponse> negotiate(final ConnectionBase connection) {
        LOGGER.debug("Start the negotiation with the server");

        String url = connection.getUrl() + "negotiate" + TransportHelper.getNegotiateQueryString(connection);

        Request get = new Request(Constants.HTTP_GET);
        get.setUrl(url);
        get.setHeaders(connection.getHeaders());
        get.setVerb(Constants.HTTP_GET);

        connection.prepareRequest(get);

        final SignalRFuture<NegotiationResponse> negotiationFuture = new SignalRFuture<>();

        LOGGER.debug("Execute the request");
        HttpConnectionFuture connectionFuture = httpConnection.execute(get, response -> {
            try {
                LOGGER.trace("Response received");
                throwOnInvalidStatusCode(response);
                LOGGER.trace("Headers: {}", response.getHeaders());

                LOGGER.trace("Read response data to the end");
                String negotiationContent = response.readToEnd();

                LOGGER.trace("Trigger onSuccess with negotiation data: {}", negotiationContent);
                negotiationFuture.setResult(new NegotiationResponse(negotiationContent));

                // Set cookies, so we get sent to the right server.
                List<String> cookies = response.getHeader("Set-Cookie");
                if (cookies != null) {
                    cookies = new ArrayList<>(cookies);
                    cookies.removeIf("HttpOnly"::equals);
                    connection.getHeaders().put("Cookie", String.join("; ", cookies));
                }
            } catch (Throwable e) {
                LOGGER.debug("There was a problem in the negotiation with the server", e);
                negotiationFuture.triggerError(new NegotiationException("There was a problem in the negotiation with the server", e));
            }
        });

        FutureHelper.copyHandlers(connectionFuture, negotiationFuture);

        return negotiationFuture;
    }

    @Override
    public SignalRFuture<Void> send(ConnectionBase connection, String data, final DataResultCallback callback) {
        try {
            LOGGER.debug("Start sending data to the server: {}", data);

            Request post = new Request(Constants.HTTP_POST);
            post.setFormContent("data", data);
            post.setUrl(connection.getUrl() + "send" + TransportHelper.getSendQueryString(this, connection));
            post.setHeaders(connection.getHeaders());
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");

            connection.prepareRequest(post);

            LOGGER.debug("Execute the request");

            return httpConnection.execute(post, response -> {
                LOGGER.trace("Response received");
                throwOnInvalidStatusCode(response);

                LOGGER.trace("Read response to the end");
                String data1 = response.readToEnd();

                if (data1 != null) {
                    LOGGER.trace("Trigger onData with data: {}", data1);
                    callback.onData(data1);
                }
            });
        } catch (Throwable e) {
            LOGGER.debug("Unable to complete http request", e);

            SignalRFuture<Void> future = new SignalRFuture<>();
            future.triggerError(e);

            return future;
        }
    }

    @Override
    public SignalRFuture<Void> abort(ConnectionBase connection) {
        synchronized (this) {
            if (!startedAbort) {
                LOGGER.debug("Started aborting");
                startedAbort = true;
                try {
                    String url = connection.getUrl() + "abort" + TransportHelper.getSendQueryString(this, connection);

                    Request post = new Request(Constants.HTTP_POST);

                    post.setUrl(url);
                    post.setHeaders(connection.getHeaders());

                    connection.prepareRequest(post);

                    LOGGER.trace("Execute request");
                    abortFuture = httpConnection.execute(post, response -> {
                        LOGGER.trace("Finishing abort");
                        startedAbort = false;
                    });

                    return abortFuture;

                } catch (Throwable e) {
                    LOGGER.debug("Error whilst aborting", e);
                    LOGGER.trace("Finishing abort");
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
}
