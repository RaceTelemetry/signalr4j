/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.mocktransport;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.signalr4j.client.Connection;
import com.github.signalr4j.client.ConnectionState;
import com.github.signalr4j.client.SignalRFuture;
import com.github.signalr4j.client.tests.util.MockClientTransport;
import com.github.signalr4j.client.tests.util.MultiResult;
import com.github.signalr4j.client.tests.util.Utils;
import com.github.signalr4j.client.transport.NegotiationResponse;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectionTests {

    private static final String SERVER_URL = "https://myUrl.com/signalr/";
    private static final String CONNECTION_QUERYSTRING = "myVal=1";

    @Test
    public void testStart() {

        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING);

        MockClientTransport transport = new MockClientTransport();

        SignalRFuture<Void> startFuture = connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());

        assertEquals(ConnectionState.CONNECTING, connection.getState());

        transport.startOperation.future.setResult(null);

        assertEquals(ConnectionState.CONNECTED, connection.getState());

        assertTrue(startFuture.isDone());
    }

    @Test
    public void testMessageReceived() {

        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING);

        final MultiResult result = new MultiResult();

        Utils.addResultHandlersToConnection(connection, result, true);

        MockClientTransport transport = new MockClientTransport();

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);

        connection.received(json -> result.listResult.add(json));

        ObjectNode initMessage = Connection.MAPPER.createObjectNode();
        initMessage.put("S", "1");
        transport.startOperation.callback.onData(initMessage.toString());

        ObjectNode message1 = Connection.MAPPER.createObjectNode();
        message1.put("I", "Hello World");
        transport.startOperation.callback.onData(message1.toString());

        ObjectNode responseJson = Connection.MAPPER.createObjectNode();
        String groupsToken = UUID.randomUUID().toString();
        responseJson.put("G", groupsToken);
        transport.startOperation.callback.onData(responseJson.toString());

        responseJson = Connection.MAPPER.createObjectNode();
        String messageId = "10";
        responseJson.put("C", messageId);
        ArrayNode messages = Connection.MAPPER.createArrayNode();
        ObjectNode message2 = Connection.MAPPER.createObjectNode();
        message2.put("name", "my dummy message");
        messages.add(message2);
        responseJson.set("M", messages);
        transport.startOperation.callback.onData(responseJson.toString());

        assertEquals(message1.toString(), result.listResult.get(0).toString());
        assertEquals(groupsToken, connection.getGroupsToken());
        assertEquals(messageId, connection.getMessageId());
        assertEquals(message2.toString(), result.listResult.get(1).toString());
    }

    @Test
    public void testDisconnectReceived() {

        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING);

        MockClientTransport transport = new MockClientTransport();

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");
        assertEquals(ConnectionState.CONNECTED, connection.getState());

        ObjectNode disconnectMessage = Connection.MAPPER.createObjectNode();
        disconnectMessage.put("D", 1);
        transport.startOperation.callback.onData(disconnectMessage.toString());

        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
    }

    @Test
    public void testReconnectReceived() {

        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING);

        MockClientTransport transport = new MockClientTransport();

        final MultiResult result = new MultiResult();

        Utils.addResultHandlersToConnection(connection, result, true);

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        ObjectNode reconnectMessage = Connection.MAPPER.createObjectNode();
        reconnectMessage.put("T", 1);
        transport.startOperation.callback.onData(reconnectMessage.toString());

        transport.startOperation.future.setResult(null);

        assertEquals(ConnectionState.CONNECTED, result.statesResult.get(0));
        assertEquals(ConnectionState.RECONNECTING, result.statesResult.get(1));
        assertEquals(ConnectionState.CONNECTED, connection.getState());
    }

    @Test
    public void testSendMessage() {
        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING);

        MockClientTransport transport = new MockClientTransport();

        final MultiResult result = new MultiResult();

        Utils.addResultHandlersToConnection(connection, result, true);

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        String dataToSend = "My data";
        connection.send("My data").done(obj -> result.booleanResult = true);

        transport.sendOperation.future.setResult(null);

        assertEquals(dataToSend, transport.sendOperation.data);
    }

    @Test
    public void testStop() {
        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING);

        MockClientTransport transport = new MockClientTransport();

        final MultiResult result = new MultiResult();

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        assertEquals(ConnectionState.CONNECTED, connection.getState());

        connection.closed(() -> result.intResult++);

        connection.stop();

        transport.abortFuture.setResult(null);

        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
        assertEquals(1, result.intResult);
        assertEquals(1, transport.getAbortInvocations());
    }

    @Test
    public void testDisconnect() {
        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING);

        MockClientTransport transport = new MockClientTransport();

        final MultiResult result = new MultiResult();

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        assertEquals(ConnectionState.CONNECTED, connection.getState());

        connection.closed(() -> result.intResult++);

        connection.disconnect();

        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
        assertEquals(1, result.intResult);
        assertEquals(0, transport.getAbortInvocations());
    }

    @Test
    public void testConnectionSlowAndTimeOut() throws Exception {
        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING);

        MockClientTransport transport = new MockClientTransport();
        transport.setSupportKeepAlive(true);

        final MultiResult result = new MultiResult();

        connection.start(transport);

        NegotiationResponse negotiation = Utils.getDefaultNegotiationResponse();

        negotiation.setKeepAliveTimeout(1);
        negotiation.setDisconnectTimeout(2);

        transport.negotiationFuture.setResult(negotiation);
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        assertEquals(ConnectionState.CONNECTED, connection.getState());

        connection.connectionSlow(() -> result.intResult++);

        Thread.sleep((long) ((negotiation.getDisconnectTimeout() + 1) * 1000));

        assertEquals(1, result.intResult);
        assertEquals(ConnectionState.RECONNECTING, connection.getState());
    }

}