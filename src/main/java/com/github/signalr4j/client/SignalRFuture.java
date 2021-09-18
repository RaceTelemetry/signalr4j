/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Represents long-running SignalR operations
 */
public class SignalRFuture<V> implements Future<V> {
    boolean isCancelled = false;
    boolean isDone = false;
    private V result = null;
    private final List<Runnable> onCancelled = new ArrayList<>();
    private final List<Action<V>> onDone = new ArrayList<>();
    private final Object doneLock = new Object();
    private final List<ErrorCallback> errorCallback = new ArrayList<>();
    private final Queue<Throwable> errorQueue = new ConcurrentLinkedQueue<>();
    private final Object errorLock = new Object();
    private Throwable lastError = null;

    private final Semaphore resultSemaphore = new Semaphore(0);

    /**
     * Handles the cancellation event
     *
     * @param onCancelled The handler
     */
    public void onCancelled(Runnable onCancelled) {
        this.onCancelled.add(onCancelled);
    }

    /**
     * Cancels the operation
     */
    public void cancel() {
        isCancelled = true;
        for (Runnable onCancelled : onCancelled) {
            onCancelled.run();
        }
        resultSemaphore.release();
    }

    /**
     * Sets a result to the future and finishes its execution
     *
     * @param result The future result
     */
    public void setResult(V result) {
        synchronized (doneLock) {
            this.result = result;
            isDone = true;

            if (onDone.size() > 0) {
                for (Action<V> handler : onDone) {
                    try {
                        handler.run(result);
                    } catch (Exception e) {
                        triggerError(e);
                    }
                }
            }
        }

        resultSemaphore.release();
    }

    /**
     * Indicates if the operation is cancelled
     *
     * @return True if the operation is cancelled
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancel();
        return true;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        try {
            return get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (resultSemaphore.tryAcquire(timeout, unit)) {
            if (errorWasTriggered()) {
                throw new ExecutionException(lastError);
            } else if (isCancelled()) {
                throw new InterruptedException("Operation was cancelled");
            } else {
                return result;
            }
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    /**
     * Handles the completion of the Future. If the future was already
     * completed, it triggers the handler right away.
     *
     * @param action The handler
     */
    public SignalRFuture<V> done(Action<V> action) {
        synchronized (doneLock) {
            onDone.add(action);

            if (isDone()) {
                try {
                    action.run(get());
                } catch (Exception e) {
                    triggerError(e);
                }
            }
        }

        return this;
    }

    /**
     * Handles error during the execution of the Future. If it's the first time
     * the method is invoked on the object and errors were already triggered,
     * the handler will be called once per error, right away.
     *
     * @param errorCallback The handler
     */
    public SignalRFuture<V> onError(ErrorCallback errorCallback) {
        synchronized (errorLock) {
            this.errorCallback.add(errorCallback);
            while (!errorQueue.isEmpty()) {
                // Only the first error handler will get the queued errors
                if (errorCallback != null) {
                    errorCallback.onError(errorQueue.poll());
                }
            }
        }

        return this;
    }

    /**
     * Triggers an error for the Future
     *
     * @param error The error
     */
    public void triggerError(Throwable error) {
        synchronized (errorLock) {
            lastError = error;
            resultSemaphore.release();
            if (errorCallback.size() > 0) {
                for (ErrorCallback handler : errorCallback) {
                    handler.onError(error);
                }
            } else {
                errorQueue.add(error);
            }
        }
    }

    /**
     * Indicates if an error was triggered
     *
     * @return True if an error was triggered
     */
    public boolean errorWasTriggered() {
        return lastError != null;
    }

}
