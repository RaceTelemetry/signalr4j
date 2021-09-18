/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.util;

import com.github.signalr4j.client.ConnectionBase;
import com.github.signalr4j.client.SignalRFuture;
import com.github.signalr4j.client.transport.ClientTransport;
import com.github.signalr4j.client.transport.ConnectionType;
import com.github.signalr4j.client.transport.DataResultCallback;
import com.github.signalr4j.client.transport.NegotiationResponse;

public class MockClientTransport implements ClientTransport {
    private boolean supportKeepAlive = false;
    private int abortInvocations = 0;

    public SignalRFuture<NegotiationResponse> negotiationFuture;
    public TransportOperation startOperation;
    public TransportOperation sendOperation;
    public SignalRFuture<Void> abortFuture;

    public void setSupportKeepAlive(boolean support) {
        supportKeepAlive = support;
    }

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public boolean supportKeepAlive() {
        return supportKeepAlive;
    }

    @Override
    public SignalRFuture<NegotiationResponse> negotiate(ConnectionBase connection) {
        negotiationFuture = new SignalRFuture<>();
        return negotiationFuture;
    }

    @Override
    public SignalRFuture<Void> start(ConnectionBase connection, ConnectionType connectionType, DataResultCallback callback) {
        startOperation = new TransportOperation();
        startOperation.future = new SignalRFuture<>();
        startOperation.callback = callback;
        return startOperation.future;
    }

    @Override
    public SignalRFuture<Void> send(ConnectionBase connection, String data, DataResultCallback callback) {
        sendOperation = new TransportOperation();
        sendOperation.future = new SignalRFuture<>();
        sendOperation.callback = callback;
        sendOperation.data = data;
        return sendOperation.future;
    }

    @Override
    public SignalRFuture<Void> abort(ConnectionBase connection) {
        abortInvocations++;
        abortFuture = new SignalRFuture<>();
        return abortFuture;
    }

    public int getAbortInvocations() {
        return abortInvocations;
    }

    public static class TransportOperation {
        public SignalRFuture<Void> future;
        public DataResultCallback callback;
        public Object data;
    }

}
