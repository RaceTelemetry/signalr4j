/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.mocktransport;

import com.github.signalr4j.client.Connection;
import com.github.signalr4j.client.SignalRFuture;
import com.github.signalr4j.client.tests.util.*;
import com.github.signalr4j.client.transport.ClientTransport;
import com.github.signalr4j.client.transport.NegotiationResponse;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class HttpClientTransportTests {

    protected abstract TransportType getTransportType();

    @Test
    public void testNegotiate() throws Exception {
        final MockHttpConnection httpConnection = new MockHttpConnection();
        ClientTransport transport = Utils.createTransport(getTransportType(), httpConnection);

        Connection connection = new Connection("https://myUrl.com/");
        SignalRFuture<NegotiationResponse> future = transport.negotiate(connection);

        NegotiationResponse negotiation = Utils.getDefaultNegotiationResponse();

        String negotiationContent = Utils.getNegotiationResponseContent(negotiation);

        MockHttpConnection.RequestEntry entry = httpConnection.getRequest();
        entry.response.writeLine(negotiationContent);

        Utils.finishMessage(entry);

        NegotiationResponse negotiationResponse;

        negotiationResponse = future.get();

        assertEquals(negotiation.getConnectionId(), negotiationResponse.getConnectionId());
        assertEquals(negotiation.getConnectionToken(), negotiationResponse.getConnectionToken());
        assertEquals(negotiation.getProtocolVersion(), negotiationResponse.getProtocolVersion());
    }

    @Test
    public void testSend() throws Exception {
        final MockHttpConnection httpConnection = new MockHttpConnection();
        ClientTransport transport = Utils.createTransport(getTransportType(), httpConnection);

        MockConnection connection = new MockConnection();

        String dataToSend = UUID.randomUUID().toString();
        final MultiResult result = new MultiResult();

        final String dataLock = "dataLock" + getTransportType().toString();

        SignalRFuture<Void> send = transport.send(connection, dataToSend, receivedData -> {
            result.stringResult = receivedData.trim();
            Sync.complete(dataLock);
        });

        MockHttpConnection.RequestEntry entry = httpConnection.getRequest();
        entry.response.writeLine(entry.request.getContent());

        Utils.finishMessage(entry);

        String sendUrl = connection.getUrl() + "send?transport=" + transport.getName() + "&connectionToken=" + Utils.encode(connection.getConnectionToken())
                + "&connectionId=" + Utils.encode(connection.getConnectionId()) + "&connectionData=" + Utils.encode(connection.getConnectionData()) + "&"
                + connection.getQueryString();

        Sync.waitComplete(dataLock);
        assertEquals(sendUrl, entry.request.getUrl());

        assertEquals("data=" + dataToSend + "&", entry.request.getContent());
        assertEquals("data=" + dataToSend + "&", result.stringResult);
        assertTrue(send.isDone());
    }

    @Test
    public void testAbort() throws Exception {
        final MockHttpConnection httpConnection = new MockHttpConnection();

        ClientTransport transport = Utils.createTransport(getTransportType(), httpConnection);

        MockConnection connection = new MockConnection();

        final String connectLock = "connectLock" + getTransportType().toString();

        SignalRFuture<Void> abort = transport.abort(connection);
        abort.done(obj -> Sync.complete(connectLock));

        MockHttpConnection.RequestEntry entry = httpConnection.getRequest();
        entry.response.writeLine(entry.request.getContent());

        Utils.finishMessage(entry);

        String abortUrl = connection.getUrl() + "abort?transport=" + transport.getName() + "&connectionToken=" + Utils.encode(connection.getConnectionToken())
                + "&connectionId=" + Utils.encode(connection.getConnectionId()) + "&connectionData=" + Utils.encode(connection.getConnectionData()) + "&"
                + connection.getQueryString();

        Sync.waitComplete(connectLock);
        assertEquals(abortUrl, entry.request.getUrl());
        assertTrue(abort.isDone());
    }

    @Test
    @Ignore
    public void testInvalidNegotiationData() throws Exception {
        final MockHttpConnection httpConnection = new MockHttpConnection();
        ClientTransport transport = Utils.createTransport(getTransportType(), httpConnection);

        Connection connection = new Connection("https://myUrl.com/");
        SignalRFuture<NegotiationResponse> future = transport.negotiate(connection);

        final MultiResult result = new MultiResult();
        result.booleanResult = false;
        future.onError(error -> {
            result.booleanResult = true;
            Sync.complete("invalidNegotiationData");
        });

        future.done(obj -> Sync.complete("invalidNegotiationData"));

        String invalidNegotiationContent = "bad-data-123";

        MockHttpConnection.RequestEntry entry = httpConnection.getRequest();
        entry.response.writeLine(invalidNegotiationContent);

        Utils.finishMessage(entry);

        Sync.waitComplete("invalidNegotiationData");

        assertTrue(result.booleanResult);
    }

    @Test
    @Ignore
    public void testInvalidNegotiationJsonData() throws Exception {
        final MockHttpConnection httpConnection = new MockHttpConnection();
        ClientTransport transport = Utils.createTransport(getTransportType(), httpConnection);

        Connection connection = new Connection("https://myUrl.com/");
        SignalRFuture<NegotiationResponse> future = transport.negotiate(connection);

        final MultiResult result = new MultiResult();
        result.booleanResult = false;
        future.onError(error -> {
            result.booleanResult = true;
            Sync.complete("invalidNegotiationData");
        });

        future.done(obj -> Sync.complete("invalidNegotiationData"));

        String invalidNegotiationContent = "{\"myValue\":\"bad-data-123\"}";

        MockHttpConnection.RequestEntry entry = httpConnection.getRequest();
        entry.response.writeLine(invalidNegotiationContent);

        Utils.finishMessage(entry);

        Sync.waitComplete("invalidNegotiationData");

        assertTrue(result.booleanResult);
    }
}