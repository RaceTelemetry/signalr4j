/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.realtransport;

import org.junit.Ignore;
import org.junit.Test;
import tel.race.signalr.client.Connection;
import tel.race.signalr.client.Platform;
import tel.race.signalr.client.SignalRFuture;
import tel.race.signalr.client.tests.util.TransportType;
import tel.race.signalr.client.tests.util.Utils;
import tel.race.signalr.client.transport.ClientTransport;
import tel.race.signalr.client.transport.NegotiationResponse;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore
public abstract class HttpClientTransportTests {

    protected abstract TransportType getTransportType();

    @Test
    @Ignore
    public void testNegotiate() throws Exception {
        ClientTransport transport = Utils.createTransport(getTransportType(), Platform.createHttpConnection());

        Connection connection = new Connection(TestData.SERVER_URL, TestData.CONNECTION_QUERYSTRING);
        SignalRFuture<NegotiationResponse> future = transport.negotiate(connection);

        NegotiationResponse negotiationResponse = future.get();

        assertNotNull(negotiationResponse);
        assertNotNull(negotiationResponse.getConnectionId());
        assertNotNull(negotiationResponse.getConnectionToken());
        assertEquals("1.5", negotiationResponse.getProtocolVersion());
        assertNotNull(negotiationResponse.getUrl());
    }

    @Test
    @Ignore
    public void testSend() throws Exception {
        ClientTransport transport = Utils.createTransport(getTransportType(), Platform.createHttpConnection());
        Connection connection = new Connection(TestData.SERVER_URL, TestData.CONNECTION_QUERYSTRING);

        connection.start(transport).get();

        String dataToSend = UUID.randomUUID().toString();

        transport.send(connection, dataToSend, data -> {
            // TODO Auto-generated method stub
        }).get();

        String lastSentData = TestData.getLastSentData();

        assertEquals(dataToSend, lastSentData);
    }
}