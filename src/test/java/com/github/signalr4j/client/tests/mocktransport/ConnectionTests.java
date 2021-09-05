/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.mocktransport;

import com.github.signalr4j.client.*;
import com.github.signalr4j.client.tests.util.MockClientTransport;
import com.github.signalr4j.client.tests.util.MultiResult;
import com.github.signalr4j.client.tests.util.Utils;
import com.github.signalr4j.client.transport.NegotiationResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectionTests {

    private static final String SERVER_URL = "http://myUrl.com/signalr/";
    private static final String CONNECTION_QUERYSTRING = "myVal=1";

    @Test
    public void testStart() throws Exception {

        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING, new NullLogger());

        MockClientTransport transport = new MockClientTransport();

        SignalRFuture<Void> startFuture = connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());

        assertEquals(ConnectionState.CONNECTING, connection.getState());

        transport.startOperation.future.setResult(null);

        assertEquals(ConnectionState.CONNECTED, connection.getState());

        assertTrue(startFuture.isDone());
    }

    @Test
    public void testMessageReceived() throws Exception {

        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING, new NullLogger());

        final MultiResult result = new MultiResult();

        Utils.addResultHandlersToConnection(connection, result, true);

        MockClientTransport transport = new MockClientTransport();

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);

        connection.received(new MessageReceivedHandler() {

            @Override
            public void onMessageReceived(JsonElement json) {
                result.listResult.add(json);
            }
        });

        JsonObject initMessage = new JsonObject();
        initMessage.addProperty("S", "1");
        transport.startOperation.callback.onData(initMessage.toString());

        JsonObject message1 = new JsonObject();
        message1.addProperty("I", "Hello World");
        transport.startOperation.callback.onData(message1.toString());

        JsonObject responseJson = new JsonObject();
        String groupsToken = UUID.randomUUID().toString();
        responseJson.addProperty("G", groupsToken);
        transport.startOperation.callback.onData(responseJson.toString());

        responseJson = new JsonObject();
        String messageId = "10";
        responseJson.addProperty("C", messageId);
        JsonArray messages = new JsonArray();
        JsonObject message2 = new JsonObject();
        message2.addProperty("name", "my dummy message");
        messages.add(message2);
        responseJson.add("M", messages);
        transport.startOperation.callback.onData(responseJson.toString());

        assertEquals(message1.toString(), result.listResult.get(0).toString());
        assertEquals(groupsToken, connection.getGroupsToken());
        assertEquals(messageId, connection.getMessageId());
        assertEquals(message2.toString(), result.listResult.get(1).toString());
    }

    @Test
    public void testDisconnectReceived() throws Exception {

        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING, new NullLogger());

        MockClientTransport transport = new MockClientTransport();

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");
        assertEquals(ConnectionState.CONNECTED, connection.getState());

        JsonObject disconnectMessage = new JsonObject();
        disconnectMessage.addProperty("D", 1);
        transport.startOperation.callback.onData(disconnectMessage.toString());

        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
    }

    @Test
    public void testReconnectReceived() throws Exception {

        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING, new NullLogger());

        MockClientTransport transport = new MockClientTransport();

        final MultiResult result = new MultiResult();

        Utils.addResultHandlersToConnection(connection, result, true);

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        JsonObject reconnectMessage = new JsonObject();
        reconnectMessage.addProperty("T", 1);
        transport.startOperation.callback.onData(reconnectMessage.toString());

        transport.startOperation.future.setResult(null);

        assertEquals(ConnectionState.CONNECTED, result.statesResult.get(0));
        assertEquals(ConnectionState.RECONNECTING, result.statesResult.get(1));
        assertEquals(ConnectionState.CONNECTED, connection.getState());
    }

    @Test
    public void testSendMessage() throws Exception {
        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING, new NullLogger());

        MockClientTransport transport = new MockClientTransport();

        final MultiResult result = new MultiResult();

        Utils.addResultHandlersToConnection(connection, result, true);

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        String dataToSend = "My data";
        connection.send("My data").done(new Action<Void>() {

            @Override
            public void run(Void obj) throws Exception {
                result.booleanResult = true;
            }
        });

        transport.sendOperation.future.setResult(null);

        assertEquals(dataToSend, transport.sendOperation.data);
    }

    @Test
    public void testStop() throws Exception {
        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING, new NullLogger());

        MockClientTransport transport = new MockClientTransport();

        final MultiResult result = new MultiResult();

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        assertEquals(ConnectionState.CONNECTED, connection.getState());

        connection.closed(new Runnable() {

            @Override
            public void run() {
                result.intResult++;
            }
        });

        connection.stop();

        transport.abortFuture.setResult(null);

        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
        assertEquals(1, result.intResult);
        assertEquals(1, transport.getAbortInvocations());
    }

    @Test
    public void testDisconnect() throws Exception {
        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING, new NullLogger());

        MockClientTransport transport = new MockClientTransport();

        final MultiResult result = new MultiResult();

        connection.start(transport);

        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        assertEquals(ConnectionState.CONNECTED, connection.getState());

        connection.closed(new Runnable() {

            @Override
            public void run() {
                result.intResult++;
            }
        });

        connection.disconnect();

        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
        assertEquals(1, result.intResult);
        assertEquals(0, transport.getAbortInvocations());
    }

    @Test
    public void testConnectionSlowAndTimeOut() throws Exception {
        Connection connection = new Connection(SERVER_URL, CONNECTION_QUERYSTRING, new NullLogger());

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

        connection.connectionSlow(new Runnable() {

            @Override
            public void run() {
                result.intResult++;
            }
        });

        Thread.sleep((long) ((negotiation.getDisconnectTimeout() + 1) * 1000));

        assertEquals(1, result.intResult);
        assertEquals(ConnectionState.RECONNECTING, connection.getState());
    }

}