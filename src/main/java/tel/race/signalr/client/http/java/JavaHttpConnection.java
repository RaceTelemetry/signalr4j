/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.http.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tel.race.signalr.client.Platform;
import tel.race.signalr.client.http.HttpConnection;
import tel.race.signalr.client.http.HttpConnectionFuture;
import tel.race.signalr.client.http.Request;

/**
 * Java HttpConnection implementation, based on HttpURLConnection and threads
 * async operations
 */
public class JavaHttpConnection implements HttpConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaHttpConnection.class);

    /**
     * User agent header name
     */
    private static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * Initializes the JavaHttpConnection
     */
    public JavaHttpConnection() {
    }

    @Override
    public HttpConnectionFuture execute(final Request request, final HttpConnectionFuture.ResponseCallback callback) {

        if (request.getHeaderField(USER_AGENT_HEADER) == null) {
            request.addHeader(USER_AGENT_HEADER, Platform.getUserAgent());
        }

        LOGGER.trace("Create new thread for HTTP Connection");

        HttpConnectionFuture future = new HttpConnectionFuture();

        final NetworkRunnable target = new NetworkRunnable(request, future, callback);
        final NetworkThread networkThread = new NetworkThread(target) {
            @Override
            void releaseAndStop() {
                try {
                    target.closeStreamAndConnection();
                } catch (Throwable ignored) {
                }
            }
        };

        future.onCancelled(new Runnable() {

            @Override
            public void run() {
                networkThread.releaseAndStop();
            }
        });

        networkThread.start();

        return future;
    }
}
