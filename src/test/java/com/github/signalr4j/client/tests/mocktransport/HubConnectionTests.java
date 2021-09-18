/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.mocktransport;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.signalr4j.client.Connection;
import com.github.signalr4j.client.hubs.HubConnection;
import com.github.signalr4j.client.hubs.HubProxy;
import com.github.signalr4j.client.hubs.Subscription;
import com.github.signalr4j.client.tests.util.MockClientTransport;
import com.github.signalr4j.client.tests.util.MultiResult;
import com.github.signalr4j.client.tests.util.Utils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HubConnectionTests {

    private static final String SERVER_URL = "https://myUrl.com/";

    @Test
    public void testConnectionDefaultUrl() {
        HubConnection connection = new HubConnection(SERVER_URL, "", true);

        assertEquals(SERVER_URL + "signalr/", connection.getUrl());
    }

    @Test
    public void testConnectionData() {

        HubConnection connection = new HubConnection(SERVER_URL, "", true);

        connection.createHubProxy("myProxy1");
        connection.createHubProxy("myProxy2");

        assertEquals(connection.getConnectionData(), "[{\"name\":\"myproxy1\"},{\"name\":\"myproxy2\"}]");
    }

    @Test
    public void testInvoke() {

        MockClientTransport transport = new MockClientTransport();
        HubConnection connection = new HubConnection(SERVER_URL, "", true);

        HubProxy proxy = connection.createHubProxy("myProxy1");

        connection.start(transport);
        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);
        transport.startOperation.callback.onData("{\"S\":1}");

        final MultiResult multiResult = new MultiResult();
        multiResult.booleanResult = false;

        String method = "myMethod";
        final String arg1 = "arg1";
        final int arg2 = 2;

        InvocationResult arg = new InvocationResult();
        arg.prop1 = arg1;
        arg.prop2 = arg2;

        proxy.invoke(InvocationResult.class, method, arg).done(result -> {
            multiResult.booleanResult = true;

            assertEquals(arg1, result.prop1);
            assertEquals(arg2, result.prop2);
        });

        transport.sendOperation.future.setResult(null);

        ObjectNode expectedSendData = Connection.MAPPER.createObjectNode();
        expectedSendData.put("I", "0");
        expectedSendData.put("H", "myProxy1");
        expectedSendData.put("M", method);
        ArrayNode sentArguments = Connection.MAPPER.createArrayNode();
        ObjectNode jsonArg1 = Connection.MAPPER.createObjectNode();
        jsonArg1.put("prop1", arg1);
        jsonArg1.put("prop2", arg2);
        sentArguments.add(jsonArg1);
        expectedSendData.set("A", sentArguments);

        assertEquals(expectedSendData.toString(), transport.sendOperation.data.toString());

        ObjectNode jsonResult = Connection.MAPPER.createObjectNode();
        jsonResult.put("I", "0");
        jsonResult.set("R", jsonArg1);

        transport.startOperation.callback.onData(jsonResult.toString());

        assertTrue(multiResult.booleanResult);
    }

    @Test
    public void testDynamicSubscriptionHandler() {

        MockClientTransport transport = new MockClientTransport();
        HubConnection connection = new HubConnection(SERVER_URL, "", true);

        HubProxy proxy = connection.createHubProxy("myProxy1");

        final MultiResult multiResult = new MultiResult();

        final String pString = "p1";
        final int pInt = 1;

        proxy.subscribe(new Object() {
            @SuppressWarnings("unused")
            public void message1(String arg1, int arg2) {
                assertEquals(pString, arg1);
                assertEquals(pInt, arg2);

                multiResult.listResult.add(1);
            }

            @SuppressWarnings("unused")
            public void message2(int arg1, String arg2) {
                assertEquals(pInt, arg1);
                assertEquals(pString, arg2);

                multiResult.listResult.add(2);
            }
        });

        connection.start(transport);
        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);

        ObjectNode message = Connection.MAPPER.createObjectNode();

        // message1
        ObjectNode hubMessage1 = Connection.MAPPER.createObjectNode();
        hubMessage1.put("H", "myproxy1");
        hubMessage1.put("M", "message1");
        ArrayNode jsonArgs = Connection.MAPPER.createArrayNode();
        jsonArgs.add(pString);
        jsonArgs.add(1);
        hubMessage1.set("A", jsonArgs);
        ArrayNode messageArray = Connection.MAPPER.createArrayNode();
        messageArray.add(hubMessage1);

        // message2
        ObjectNode hubMessage2 = Connection.MAPPER.createObjectNode();
        hubMessage2.put("H", "myproxy1");
        hubMessage2.put("M", "message2");
        jsonArgs = Connection.MAPPER.createArrayNode();
        jsonArgs.add(1);
        jsonArgs.add(pString);
        hubMessage2.set("A", jsonArgs);
        messageArray.add(hubMessage2);

        message.set("M", messageArray);

        transport.startOperation.callback.onData(message.toString());

        assertEquals(2, multiResult.listResult.size());
        assertEquals(1, multiResult.listResult.get(0));
        assertEquals(2, multiResult.listResult.get(1));
    }

    @Test
    public void testOnSubscriptionHandler() {

        MockClientTransport transport = new MockClientTransport();
        HubConnection connection = new HubConnection(SERVER_URL, "", true);

        HubProxy proxy = connection.createHubProxy("myProxy1");

        final MultiResult multiResult = new MultiResult();

        final String pString = "p1";
        final int pInt = 1;

        proxy.on("message1", (arg1, arg2) -> {
            assertEquals(pString, arg1);
            assertEquals(pInt, (int) arg2);

            multiResult.listResult.add(1);
        }, String.class, Integer.class);

        proxy.on("message2", (arg1, arg2) -> {
            assertEquals(pInt, (int) arg1);
            assertEquals(pString, arg2);

            multiResult.listResult.add(2);
        }, Integer.class, String.class);

        connection.start(transport);
        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);

        ObjectNode message = Connection.MAPPER.createObjectNode();

        // message1
        ObjectNode hubMessage1 = Connection.MAPPER.createObjectNode();
        hubMessage1.put("H", "myproxy1");
        hubMessage1.put("M", "message1");
        ArrayNode jsonArgs = Connection.MAPPER.createArrayNode();
        jsonArgs.add(pString);
        jsonArgs.add(1);
        hubMessage1.set("A", jsonArgs);
        ArrayNode messageArray = Connection.MAPPER.createArrayNode();
        messageArray.add(hubMessage1);

        // message2
        ObjectNode hubMessage2 = Connection.MAPPER.createObjectNode();
        hubMessage2.put("H", "myproxy1");
        hubMessage2.put("M", "message2");
        jsonArgs = Connection.MAPPER.createArrayNode();
        jsonArgs.add(1);
        jsonArgs.add(pString);
        hubMessage2.set("A", jsonArgs);
        messageArray.add(hubMessage2);

        message.set("M", messageArray);

        transport.startOperation.callback.onData(message.toString());

        assertEquals(2, multiResult.listResult.size());
        assertEquals(1, multiResult.listResult.get(0));
        assertEquals(2, multiResult.listResult.get(1));
    }

    @Test
    public void testSubscription() {

        MockClientTransport transport = new MockClientTransport();
        HubConnection connection = new HubConnection(SERVER_URL, "", true);

        HubProxy proxy = connection.createHubProxy("myProxy1");

        final MultiResult multiResult = new MultiResult();

        final String pString = "p1";
        final int pInt = 1;

        Subscription sub1 = proxy.subscribe("message1");

        sub1.addReceivedHandler(obj -> {
            assertEquals(pString, obj[0].textValue());
            assertEquals(pInt, obj[1].intValue());

            multiResult.listResult.add(1);
        });

        Subscription sub2 = proxy.subscribe("message2");

        sub2.addReceivedHandler(obj -> {
            assertEquals(pInt, obj[0].intValue());
            assertEquals(pString, obj[1].textValue());

            multiResult.listResult.add(2);
        });

        connection.start(transport);
        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);

        ObjectNode message = Connection.MAPPER.createObjectNode();

        // message1
        ObjectNode hubMessage1 = Connection.MAPPER.createObjectNode();
        hubMessage1.put("H", "myproxy1");
        hubMessage1.put("M", "message1");
        ArrayNode jsonArgs = Connection.MAPPER.createArrayNode();
        jsonArgs.add(pString);
        jsonArgs.add(1);
        hubMessage1.set("A", jsonArgs);
        ArrayNode messageArray = Connection.MAPPER.createArrayNode();
        messageArray.add(hubMessage1);

        // message2
        ObjectNode hubMessage2 = Connection.MAPPER.createObjectNode();
        hubMessage2.put("H", "myproxy1");
        hubMessage2.put("M", "message2");
        jsonArgs = Connection.MAPPER.createArrayNode();
        jsonArgs.add(1);
        jsonArgs.add(pString);
        hubMessage2.set("A", jsonArgs);
        messageArray.add(hubMessage2);

        message.set("M", messageArray);

        transport.startOperation.callback.onData(message.toString());

        assertEquals(2, multiResult.listResult.size());
        assertEquals(1, multiResult.listResult.get(0));
        assertEquals(2, multiResult.listResult.get(1));
    }

    @Test
    public void setHubConnectionHeaders() {
        HubConnection connection = new HubConnection(SERVER_URL, "", true);
        connection.getHeaders().put("key", "value");

        assertEquals(1, connection.getHeaders().values().size());
        assertEquals("value", connection.getHeaders().get("key"));
    }

    @Test
    public void testMultipleSubscriptionForEvent() {

        MockClientTransport transport = new MockClientTransport();
        HubConnection connection = new HubConnection(SERVER_URL, "", true);

        HubProxy proxy = connection.createHubProxy("myProxy1");

        final MultiResult multiResult = new MultiResult();

        final String pString = "p1";

        proxy.subscribe(new Object() {
            @SuppressWarnings("unused")
            public void message1(String arg1, int arg2) {
                multiResult.listResult.add(1);
            }
        });

        proxy.subscribe(new Object() {
            @SuppressWarnings("unused")
            public void message1(String arg1, int arg2) {
                multiResult.listResult.add(1);
            }
        });

        connection.start(transport);
        transport.negotiationFuture.setResult(Utils.getDefaultNegotiationResponse());
        transport.startOperation.future.setResult(null);

        ObjectNode message = Connection.MAPPER.createObjectNode();

        // message1
        ObjectNode hubMessage1 = Connection.MAPPER.createObjectNode();
        hubMessage1.put("H", "myproxy1");
        hubMessage1.put("M", "message1");
        ArrayNode jsonArgs = Connection.MAPPER.createArrayNode();
        jsonArgs.add(pString);
        jsonArgs.add(1);
        hubMessage1.set("A", jsonArgs);
        ArrayNode messageArray = Connection.MAPPER.createArrayNode();
        messageArray.add(hubMessage1);
        message.set("M", messageArray);

        transport.startOperation.callback.onData(message.toString());

        assertEquals(2, multiResult.listResult.size());
    }

    public static class InvocationResult {
        public String prop1;
        public int prop2;
    }

}