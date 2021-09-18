/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.test.integration.framework;

import java.util.List;

public interface TestExecutionCallback {
    void onTestStart(TestCase test);

    void onTestComplete(TestCase test, TestResult result);

    void onTestGroupComplete(TestGroup group, List<TestResult> results);
}
