/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.test.integration;

import com.github.signalr4j.client.hubs.HubConnection;
import com.github.signalr4j.client.test.integration.framework.TestCase;
import com.github.signalr4j.client.test.integration.framework.TestExecutionCallback;
import com.github.signalr4j.client.transport.AutomaticTransport;
import com.github.signalr4j.client.transport.ClientTransport;
import com.github.signalr4j.client.transport.LongPollingTransport;
import com.github.signalr4j.client.transport.ServerSentEventsTransport;

import java.util.concurrent.Future;

public class ApplicationContext {

    private static TestPlatformContext testPlatformContext;

    public static void setTestPlatformContext(TestPlatformContext testPlatformContext) {
        ApplicationContext.testPlatformContext = testPlatformContext;
    }

    public static void sleep() throws Exception {
        testPlatformContext.sleep(3);
    }

    public static void sleep(int seconds) throws Exception {
        testPlatformContext.sleep(seconds);
    }

    public static HubConnection createHubConnection() {
        String url = getServerUrl();

        HubConnection connection = new HubConnection(url, "", true);

        return connection;
    }

    public static HubConnection createHubConnectionWithInvalidURL() {
        String url = "http://signalr.net/fake";

        HubConnection connection = new HubConnection(url, "", true);

        return connection;
    }

    public static String getServerUrl() {
        return testPlatformContext.getServerUrl();
    }

    public static String getLogPostURL() {
        return testPlatformContext.getLogPostUrl();
    }


    public static ClientTransport createTransport(TransportType transportType) {
        switch (transportType) {
            case AUTO:
                return new AutomaticTransport();

            case LONG_POLLING:
                return new LongPollingTransport();

            case SERVER_SENT_EVENTS:
                return new ServerSentEventsTransport();
            default:
                return null;
        }
    }

    public static Future<Void> showMessage(String message) {
        return testPlatformContext.showMessage(message);
    }

    public static void executeTest(TestCase testCase, TestExecutionCallback callback) {
        testPlatformContext.executeTest(testCase, callback);
    }
}
