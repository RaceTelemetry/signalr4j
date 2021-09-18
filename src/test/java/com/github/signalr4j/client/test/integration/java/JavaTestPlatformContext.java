/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.test.integration.java;

import com.github.signalr4j.client.SignalRFuture;
import com.github.signalr4j.client.test.integration.TestPlatformContext;
import com.github.signalr4j.client.test.integration.framework.TestCase;
import com.github.signalr4j.client.test.integration.framework.TestExecutionCallback;
import com.github.signalr4j.client.test.integration.framework.TestResult;

import java.util.Scanner;
import java.util.concurrent.Future;

public class JavaTestPlatformContext implements TestPlatformContext {

    private boolean loggingEnabled = false;
    private final String serverUrl;

    public JavaTestPlatformContext(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    @Override
    public String getLogPostUrl() {
        return "http://not-supported/";
    }

    @Override
    public Future<Void> showMessage(String message) {
        SignalRFuture<Void> future = new SignalRFuture<>();

        System.out.println(message);
        System.out.println("Press any key to continue...");
        Scanner scanner = new Scanner(System.in);
        scanner.next();

        future.setResult(null);

        return future;
    }

    @Override
    public void executeTest(TestCase testCase, TestExecutionCallback callback) {
        TestResult result = testCase.executeTest();
        callback.onTestComplete(testCase, result);
    }

    @Override
    public void sleep(int seconds) throws Exception {
        System.out.println("Sleeping for " + seconds + " seconds...");
        Thread.sleep(seconds * 1000L);
        System.out.println("Woke up");
    }
}
