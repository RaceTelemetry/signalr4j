/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.realtransport;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import tel.race.signalr.client.Connection;
import tel.race.signalr.client.ConnectionState;
import tel.race.signalr.client.SignalRFuture;
import tel.race.signalr.client.tests.util.MultiResult;
import tel.race.signalr.client.tests.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class ConnectionTests {

    @Test
    public void testStart() throws Exception {
        Connection connection = new Connection(TestData.SERVER_URL, TestData.CONNECTION_QUERYSTRING);

        final List<ConnectionState> newStates = new ArrayList<>();

        connection.stateChanged((oldState, newState) -> newStates.add(newState));

        SignalRFuture<Void> startFuture = connection.start();

        startFuture.get();

        assertEquals(2, newStates.size());
        assertEquals(ConnectionState.CONNECTING, newStates.get(0));
        assertEquals(ConnectionState.CONNECTED, newStates.get(1));
        assertEquals(ConnectionState.CONNECTED, connection.getState());

        assertTrue(startFuture.isDone());

        connection.disconnect();
    }

    @Test
    public void testMessageReceived() throws Exception {
        Connection connection = new Connection(TestData.SERVER_URL, TestData.CONNECTION_QUERYSTRING);

        final MultiResult result = new MultiResult();

        Utils.addResultHandlersToConnection(connection, result, true);

        connection.start().get();

        final Semaphore semaphore = new Semaphore(0);
        connection.received(json -> {
            result.listResult.add(json);
            semaphore.release();
        });

        TestData.triggerTestMessage();


        semaphore.acquire();

        assertEquals(1, result.listResult.size());
        JsonNode json = (JsonNode) result.listResult.get(0);
        String message = json.textValue();

        assertEquals("test message", message);

        connection.disconnect();
    }

    @Test
    public void testSendMessage() throws Exception {
        Connection connection = new Connection(TestData.SERVER_URL, TestData.CONNECTION_QUERYSTRING);

        final MultiResult result = new MultiResult();

        Utils.addResultHandlersToConnection(connection, result, true);

        connection.start().get();

        String dataToSend = UUID.randomUUID().toString();
        connection.send(dataToSend).get();

        String lastSentData = TestData.getLastSentData();

        assertEquals(dataToSend, lastSentData);

        connection.disconnect();
    }

    @Test
    public void testStop() throws Exception {
        Connection connection = new Connection(TestData.SERVER_URL, TestData.CONNECTION_QUERYSTRING);

        connection.start().get();

        final MultiResult result = new MultiResult();
        assertEquals(ConnectionState.CONNECTED, connection.getState());

        final Semaphore semaphore = new Semaphore(0);
        connection.closed(() -> {
            result.intResult++;
            semaphore.release();
        });

        connection.stop();

        semaphore.acquire();

        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
        assertEquals(1, result.intResult);
    }
}