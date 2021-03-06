/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

/***
 * Represents the state of a connection
 */
public enum ConnectionState {
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED
}