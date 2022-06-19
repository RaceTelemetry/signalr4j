/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.test.integration;

import tel.race.signalr.client.test.integration.framework.TestCase;
import tel.race.signalr.client.test.integration.framework.TestExecutionCallback;

import java.util.concurrent.Future;

public interface TestPlatformContext {

    String getServerUrl();

    String getLogPostUrl();

    Future<Void> showMessage(String message);

    void executeTest(TestCase testCase, TestExecutionCallback callback);

    void sleep(int seconds) throws Exception;
}
