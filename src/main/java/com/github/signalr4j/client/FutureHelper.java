/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

import com.github.signalr4j.client.http.HttpConnectionFuture;

/**
 * Helper for Future operations
 */
public class FutureHelper {

    /**
     * Copy the Cancellation and Error handlers between two SignalRFuture
     * instances
     * 
     * @param sourceFuture
     *            The source future
     * @param targetFuture
     *            The target future
     */
    public static void copyHandlers(final SignalRFuture<?> sourceFuture, final SignalRFuture<?> targetFuture) {
        targetFuture.onCancelled(sourceFuture::cancel);

        sourceFuture.onError(targetFuture::triggerError);
    }

    /**
     * Copy the Cancellation and Error handlers between two SignalRFuture
     * instances, where the source is an HttpConnectionFuture
     * 
     * @param sourceFuture
     *            The source future
     * @param targetFuture
     *            The target future
     */
    public static void copyHandlers(HttpConnectionFuture sourceFuture, final SignalRFuture<?> targetFuture) {
        copyHandlers((SignalRFuture<?>) sourceFuture, targetFuture);

        sourceFuture.onTimeout(targetFuture::triggerError);
    }
}
