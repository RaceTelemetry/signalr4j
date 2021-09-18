/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

import java.util.Calendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Heartbeat Monitor to detect slow or timed out connections
 */
public class HeartbeatMonitor {
    private Runnable onWarning;

    private Runnable onTimeout;

    private KeepAliveData keepAliveData;

    private ScheduledThreadPoolExecutor executor;

    private boolean timedOut = false;

    private boolean hasBeenWarned = false;

    private boolean stopped = true;

    private final Object sync = new Object();

    /**
     * Starts the monitor
     *
     * @param keepAliveData Data for keep-alive timings
     * @param connection    Connection to monitor
     */
    public void start(KeepAliveData keepAliveData, final ConnectionBase connection) {
        if (keepAliveData == null) {
            throw new IllegalArgumentException("keepAliveData cannot be null");
        }

        if (this.keepAliveData != null) {
            stop();
        }

        synchronized (sync) {
            this.keepAliveData = keepAliveData;

            timedOut = false;
            hasBeenWarned = false;
            stopped = false;

            long interval = this.keepAliveData.getCheckInterval();

            executor = new ScheduledThreadPoolExecutor(1);
            executor.scheduleAtFixedRate(() -> {
                synchronized (sync) {
                    if (!stopped) {
                        if (connection.getState() == ConnectionState.CONNECTED) {
                            long lastKeepAlive = HeartbeatMonitor.this.keepAliveData.getLastKeepAlive();
                            long timeElapsed = Calendar.getInstance().getTimeInMillis() - lastKeepAlive;

                            if (timeElapsed >= HeartbeatMonitor.this.keepAliveData.getTimeout()) {
                                if (!timedOut) {
                                    // Connection has been lost
                                    timedOut = true;
                                    onTimeout.run();
                                }
                            } else if (timeElapsed >= HeartbeatMonitor.this.keepAliveData.getTimeoutWarning()) {
                                if (!hasBeenWarned) {
                                    // Inform user and set HasBeenWarned to
                                    // true
                                    hasBeenWarned = true;
                                    onWarning.run();
                                }
                            } else {
                                hasBeenWarned = false;
                                timedOut = false;
                            }
                        }
                    }
                }
            }, interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops the heartbeat monitor
     */
    public void stop() {
        if (!stopped) {
            synchronized (sync) {
                stopped = true;
                if (executor != null) {
                    executor.shutdown();
                    executor = null;
                }
            }
        }
    }

    /**
     * Alerts the monitor that a beat was detected
     */
    public void beat() {
        synchronized (sync) {
            if (keepAliveData != null) {
                keepAliveData.setLastKeepAlive(Calendar.getInstance().getTimeInMillis());
            }
        }
    }

    /**
     * Returns the "Warning" event handler
     */
    public Runnable getOnWarning() {
        return onWarning;
    }

    /**
     * Sets the "Warning" event handler
     */
    public void setOnWarning(Runnable onWarning) {
        this.onWarning = onWarning;
    }

    /**
     * Returns the "Timeout" event handler
     */
    public Runnable getOnTimeout() {
        return onTimeout;
    }

    /**
     * Sets the "Timeout" event handler
     */
    public void setOnTimeout(Runnable onTimeout) {
        this.onTimeout = onTimeout;
    }

    /**
     * Returns the Keep Alive data
     */
    public KeepAliveData getKeepAliveData() {
        return keepAliveData;
    }

    /**
     * Sets the Keep Alive data
     */
    public void setKeepAliveData(KeepAliveData keepAliveData) {
        this.keepAliveData = keepAliveData;
    }
}
