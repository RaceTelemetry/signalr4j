/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.test.integration.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class TestGroup {
    List<TestCase> testCases;
    String name;
    TestStatus status;
    ConcurrentLinkedQueue<TestCase> testRunQueue;
    boolean newTestRun;

    public TestGroup(String name) {
        this.name = name;
        status = TestStatus.NOT_RUN;
        testCases = new ArrayList<>();
        testRunQueue = new ConcurrentLinkedQueue<>();
        newTestRun = false;
    }

    public TestStatus getStatus() {
        return status;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    protected void addTest(TestCase testCase) {
        testCases.add(testCase);
    }


    public void runTests(TestExecutionCallback callback) {
        List<TestCase> testsToRun = new ArrayList<>();

        for (TestCase testCase : testCases) {
            if (testCase.isEnabled()) {
                testsToRun.add(testCase);
            }
        }

        if (testsToRun.size() > 0) {
            runTests(testsToRun, callback);
        }
    }


    public void runTests(List<TestCase> testsToRun, final TestExecutionCallback callback) {
        try {
            onPreExecute();
        } catch (Exception e) {
            status = TestStatus.FAILED;
            if (callback != null)
                callback.onTestGroupComplete(this, null);
            return;
        }

        final TestRunStatus testRunStatus = new TestRunStatus();

        newTestRun = true;

        int oldQueueSize = testRunQueue.size();
        testRunQueue.clear();
        testRunQueue.addAll(testsToRun);
        cleanTestsState();
        testRunStatus.results.clear();
        status = TestStatus.NOT_RUN;

        if (oldQueueSize == 0) {
            executeNextTest(callback, testRunStatus);
        }
    }


    private void cleanTestsState() {
        for (TestCase test : testRunQueue) {
            test.setStatus(TestStatus.NOT_RUN);
            test.clearLog();
        }
    }

    private void executeNextTest(final TestExecutionCallback callback, final TestRunStatus testRunStatus) {
        newTestRun = false;
        final TestGroup group = this;

        try {
            TestCase nextTest = testRunQueue.poll();
            if (nextTest != null) {
                nextTest.run(new TestExecutionCallback() {
                    @Override
                    public void onTestStart(TestCase test) {
                        if (!newTestRun && callback != null)
                            callback.onTestStart(test);
                    }

                    @Override
                    public void onTestGroupComplete(TestGroup group, List<TestResult> results) {
                        if (!newTestRun && callback != null)
                            callback.onTestGroupComplete(group, results);
                    }

                    @Override
                    public void onTestComplete(TestCase test, TestResult result) {
                        if (newTestRun) {
                            cleanTestsState();
                            testRunStatus.results.clear();
                            status = TestStatus.NOT_RUN;
                        } else {
                            if (test.getExpectedExceptionClass() != null) {
                                if (result.getException() != null && result.getException().getClass() == test.getExpectedExceptionClass()) {
                                    result.setStatus(TestStatus.PASSED);
                                } else {
                                    result.setStatus(TestStatus.FAILED);
                                }
                            }

                            test.setStatus(result.getStatus());
                            testRunStatus.results.add(result);

                            if (callback != null)
                                callback.onTestComplete(test, result);
                        }

                        executeNextTest(callback, testRunStatus);
                    }
                });


            } else {
                // end run

                try {
                    onPostExecute();
                } catch (Exception e) {
                    status = TestStatus.FAILED;
                }

                // if at least one test failed, the test group
                // failed
                if (status != TestStatus.FAILED) {
                    status = TestStatus.PASSED;
                    for (TestResult r : testRunStatus.results) {
                        if (r.getStatus() == TestStatus.FAILED) {
                            status = TestStatus.FAILED;
                            break;
                        }
                    }
                }

                if (callback != null)
                    callback.onTestGroupComplete(group, testRunStatus.results);
            }


        } catch (Exception e) {
            if (callback != null)
                callback.onTestGroupComplete(this, testRunStatus.results);
        }
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void onPreExecute() {

    }

    public void onPostExecute() {

    }

    private static class TestRunStatus {
        public List<TestResult> results;

        public TestRunStatus() {
            results = new ArrayList<>();
        }
    }
}