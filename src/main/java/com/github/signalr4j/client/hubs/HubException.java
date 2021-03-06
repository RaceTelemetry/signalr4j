/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.hubs;

/**
 * Represents a Hub Exception
 */
public class HubException extends Exception {

    private static final long serialVersionUID = 5958638666959902780L;
    private final Object errorData;

    /**
     * Creates a new Hub exception
     * 
     * @param error
     *            The error message
     * @param errorData
     *            The error data
     */
    public HubException(String error, Object errorData) {
        super(error);

        this.errorData = errorData;
    }

    /**
     * Returns the error data
     */
    public Object getErrorData() {
        return errorData;
    }
}
