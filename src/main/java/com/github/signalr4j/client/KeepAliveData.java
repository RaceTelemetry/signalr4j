/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

import java.util.Calendar;

/**
 * Keep Alive data for the Heartbeat monitor
 */
class KeepAliveData {

    /**
     * Determines when we warn the developer that the connection may be lost
     */
    private double keepAliveWarnAt = 2.0 / 3.0;

    private long lastKeepAlive;

    /**
     * Timeout to designate when to force the connection into reconnecting
     */
    private long timeout;

    /**
     * Timeout to designate when to warn the developer that the connection may
     * be dead or is hanging.
     */

    private long timeoutWarning;

    /**
     * Frequency with which we check the keep alive. It must be short in order
     * to not miss/pick up any changes
     */
    private long checkInterval;

    /**
     * Initializes the Keep Alive data
     *
     * @param timeout Timeout in milliseconds
     */
    public KeepAliveData(long timeout) {
        setTimeout(timeout);
        setTimeoutWarning((long) (timeout * keepAliveWarnAt));
        setCheckInterval((timeout - getTimeoutWarning()) / 3);
        setLastKeepAlive(Calendar.getInstance().getTimeInMillis());
    }

    /**
     * Returns the last time the keep alive data was detected
     */
    public long getLastKeepAlive() {
        return lastKeepAlive;
    }

    /**
     * Sets the last time the keep alive data was detected
     */
    public void setLastKeepAlive(long timeInmilliseconds) {
        lastKeepAlive = timeInmilliseconds;
    }

    /**
     * Returns the timeout interval
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout interval
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns the timeout warning
     */
    public long getTimeoutWarning() {
        return timeoutWarning;
    }

    /**
     * Sets the timeout warning
     */
    public void setTimeoutWarning(long timeoutWarning) {
        this.timeoutWarning = timeoutWarning;
    }

    /**
     * Returns the Check interval
     */
    public long getCheckInterval() {
        return checkInterval;
    }

    /**
     * Sets the Check interval
     */
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }
}
