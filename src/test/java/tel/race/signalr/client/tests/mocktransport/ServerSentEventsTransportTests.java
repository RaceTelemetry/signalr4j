/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.mocktransport;

import org.junit.Before;
import org.junit.Test;
import tel.race.signalr.client.SignalRFuture;
import tel.race.signalr.client.tests.util.*;
import tel.race.signalr.client.transport.ConnectionType;
import tel.race.signalr.client.transport.ServerSentEventsTransport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServerSentEventsTransportTests extends HttpClientTransportTests {

    @Before
    public void setUp() {
        Sync.reset();
    }

    @Test
    public void testSupportKeepAlive() {
        MockHttpConnection httpConnection = new MockHttpConnection();
        ServerSentEventsTransport transport = new ServerSentEventsTransport(httpConnection);

        assertTrue(transport.supportKeepAlive());
    }

    @Test
    public void testStart() throws Exception {

        MockHttpConnection httpConnection = new MockHttpConnection();
        ServerSentEventsTransport transport = new ServerSentEventsTransport(httpConnection);

        MockConnection connection = new MockConnection();

        final MultiResult result = new MultiResult();

        final String dataLock = "dataLock" + getTransportType().toString();

        SignalRFuture<Void> future = transport.start(connection, ConnectionType.INITIAL_CONNECTION, data -> {
            result.stringResult = data;
            Sync.complete(dataLock);
        });

        MockHttpConnection.RequestEntry entry = httpConnection.getRequest();
        entry.response.writeLine("data: initialized\n\n");
        entry.response.writeLine("data: Hello");
        entry.response.writeLine("world\n\n");

        Utils.finishMessage(entry);

        Sync.waitComplete(dataLock);

        String startUrl = connection.getUrl() + "connect?transport=serverSentEvents&connectionToken=" + Utils.encode(connection.getConnectionToken())
                + "&connectionId=" + Utils.encode(connection.getConnectionId()) + "&messageId=" + Utils.encode(connection.getMessageId()) + "&groupsToken="
                + Utils.encode(connection.getGroupsToken()) + "&connectionData=" + Utils.encode(connection.getConnectionData()) + "&"
                + connection.getQueryString();

        assertEquals(startUrl, entry.request.getUrl());

        assertEquals("Hello\nworld", result.stringResult);
        assertTrue(future.isDone());
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.SERVER_SENT_EVENTS;
    }

}