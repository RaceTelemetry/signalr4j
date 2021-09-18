/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.realtransport;

import com.github.signalr4j.client.hubs.HubConnection;
import com.github.signalr4j.client.hubs.HubProxy;
import com.github.signalr4j.client.tests.util.MultiResult;
import org.junit.Ignore;
import org.junit.Test;

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