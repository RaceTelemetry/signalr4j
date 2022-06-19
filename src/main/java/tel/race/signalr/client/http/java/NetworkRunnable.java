/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.http.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tel.race.signalr.client.http.HttpConnectionFuture;
import tel.race.signalr.client.http.Request;
import tel.race.signalr.client.http.StreamResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Runnable that executes a network operation
 */
class NetworkRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkRunnable.class);

    HttpURLConnection connection = null;
    InputStream responseStream = null;
    Request request;
    HttpConnectionFuture future;
    HttpConnectionFuture.ResponseCallback callback;

    /**
     * Initializes the network runnable
     *
     * @param request  The request to execute
     * @param future   Future for the operation
     * @param callback Callback to invoke after the request execution
     */
    public NetworkRunnable(Request request, HttpConnectionFuture future, HttpConnectionFuture.ResponseCallback callback) {
        this.request = request;
        this.future = future;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            int responseCode = -1;
            if (!future.isCancelled()) {
                if (request == null) {
                    future.triggerError(new IllegalArgumentException("request"));
                    return;
                }

                LOGGER.trace("Execute the HTTP Request");
                LOGGER.trace("Request: {}", request);
                connection = createHttpURLConnection(request);

                LOGGER.trace("Request executed");

                responseCode = connection.getResponseCode();

                if (responseCode < 400) {
                    responseStream = connection.getInputStream();
                } else {
                    responseStream = connection.getErrorStream();
                }
            }

            if (responseStream != null && !future.isCancelled()) {
                callback.onResponse(new StreamResponse(responseStream, responseCode, connection.getHeaderFields()));
                future.setResult(null);
            }
        } catch (Throwable e) {
            if (!future.isCancelled()) {
                if (connection != null) {
                    connection.disconnect();
                }

                LOGGER.debug("Error executing request: ", e);
                future.triggerError(e);
            }
        } finally {
            closeStreamAndConnection();
        }
    }

    /**
     * Closes the stream and connection, if possible
     */
    void closeStreamAndConnection() {

        try {

            if (connection != null) {
                connection.disconnect();
            }

            if (responseStream != null) {
                responseStream.close();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Creates an HttpURLConnection
     *
     * @param request The request info
     * @return An HttpURLConnection to execute the request
     */
    static HttpURLConnection createHttpURLConnection(Request request) throws IOException {
        URL url = new URL(request.getUrl());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // the timeout needs for right disconnection.
        // without it when disconnect() calls at NetworkRunnable.closeStreamAndConnection()
        // there is the deadlock occurred at StreamResponse.readLine()
        connection.setReadTimeout(15 * 1000);
        connection.setConnectTimeout(15 * 1000);
        connection.setRequestMethod(request.getVerb());

        Map<String, String> headers = request.getHeaders();

        for (String key : headers.keySet()) {
            connection.setRequestProperty(key, headers.get(key));
        }

        if (request.getContent() != null) {
            connection.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            String requestContent = request.getContent();
            out.write(requestContent);
            out.close();
        }

        return connection;
    }

}
