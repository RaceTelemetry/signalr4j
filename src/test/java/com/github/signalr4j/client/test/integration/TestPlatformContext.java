/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.test.integration;

import com.github.signalr4j.client.test.integration.framework.TestCase;
import com.github.signalr4j.client.test.integration.framework.TestExecutionCallback;

import java.util.concurrent.Future;

public interface TestPlatformContext {

    String getServerUrl();

    String getLogPostUrl();

    Future<Void> showMessage(String message);

    void executeTest(TestCase testCase, TestExecutionCallback callback);

    void sleep(int seconds) throws Exception;
}
