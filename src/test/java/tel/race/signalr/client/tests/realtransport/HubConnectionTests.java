/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.realtransport;

import org.junit.Ignore;
import org.junit.Test;
import tel.race.signalr.client.hubs.HubConnection;
import tel.race.signalr.client.hubs.HubProxy;
import tel.race.signalr.client.tests.util.MultiResult;

import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore
public class HubConnectionTests {

    @Test
    public void testInvoke() throws Exception {

        HubConnection connection = new HubConnection(TestData.HUB_URL, TestData.CONNECTION_QUERYSTRING, true);

        HubProxy proxy = connection.createHubProxy(TestData.HUB_NAME);

        connection.start().get();

        String data = UUID.randomUUID().toString();

        proxy.invoke("TestMethod", data).get();

        String lastHubData = TestData.getLastHubData();

        assertEquals(data, lastHubData);
    }

    @Test
    public void testReceivedMessageForSubscription() throws Exception {
        HubConnection connection = new HubConnection(TestData.HUB_URL, TestData.CONNECTION_QUERYSTRING, true);

        HubProxy proxy = connection.createHubProxy(TestData.HUB_NAME);

        final Semaphore semaphore = new Semaphore(0);
        final MultiResult result = new MultiResult();
        proxy.on("testMessage", val -> {
            semaphore.release();
            result.stringResult = val;
        }, String.class);

        connection.start().get();
        TestData.triggerHubTestMessage();

        semaphore.acquire();

        assertNotNull(result.stringResult);
    }
}