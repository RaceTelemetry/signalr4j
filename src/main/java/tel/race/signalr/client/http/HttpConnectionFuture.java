/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.http;

import tel.race.signalr.client.ErrorCallback;
import tel.race.signalr.client.SignalRFuture;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A SinglaRFuture for Http operations
 */
public class HttpConnectionFuture extends SignalRFuture<Void> {

    private final Queue<Throwable> timeoutQueue = new ConcurrentLinkedQueue<>();
    private ErrorCallback timeoutCallback;
    private final Object timeoutLock = new Object();

    /**
     * Handles the timeout for an Http operation
     *
     * @param errorCallback The handler
     */
    public void onTimeout(ErrorCallback errorCallback) {
        synchronized (timeoutLock) {
            timeoutCallback = errorCallback;

            while (!timeoutQueue.isEmpty()) {
                if (timeoutCallback != null) {
                    timeoutCallback.onError(timeoutQueue.poll());
                }
            }
        }
    }

    /**
     * Triggers the timeout error
     *
     * @param error The error
     */
    public void triggerTimeout(Throwable error) {
        synchronized (timeoutLock) {
            if (timeoutCallback != null) {
                timeoutCallback.onError(error);
            } else {
                timeoutQueue.add(error);
            }
        }
    }

    /**
     * Represents the callback to invoke when a response is returned after a
     * request
     */
    public interface ResponseCallback {
        /**
         * Callback invoked when a response is returned by the request
         *
         * @param response The returned response
         * @throws Exception handling response
         */
        public void onResponse(Response response) throws Exception;
    }

}
