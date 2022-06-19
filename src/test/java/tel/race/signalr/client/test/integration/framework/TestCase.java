/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.test.integration.framework;

import tel.race.signalr.client.test.integration.ApplicationContext;


public abstract class TestCase {
    private String name;

    private String description;

    private Class<?> expectedExceptionClass;

    private boolean enabled;

    private TestStatus status;

    private StringBuilder testLog;

    public TestCase(String name) {
        enabled = false;
        status = TestStatus.NOT_RUN;
        testLog = new StringBuilder();
        this.name = name;
    }

    public TestCase() {
        this(null);
    }

    public void log(String log) {
        testLog.append(log);
        testLog.append("\n");
    }

    public String getLog() {
        return testLog.toString();
    }

    public void clearLog() {
        testLog = new StringBuilder();
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void run(TestExecutionCallback callback) {
        try {
            if (callback != null)
                callback.onTestStart(this);
        } catch (Exception e) {
            // do nothing
        }
        status = TestStatus.RUNNING;
        try {
            ApplicationContext.executeTest(this, callback);
        } catch (Exception e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                log("  " + stackTrace[i].toString());
            }

            TestResult result;
            if (e.getClass() != this.getExpectedExceptionClass()) {
                result = createResultFromException(e);
                status = result.getStatus();
            } else {
                result = new TestResult();
                result.setException(e);
                result.setStatus(TestStatus.PASSED);
                result.setTestCase(this);
                status = result.getStatus();
            }

            if (callback != null)
                callback.onTestComplete(this, result);
        }
    }

    public abstract TestResult executeTest();

    protected TestResult createResultFromException(Exception e) {
        return createResultFromException(new TestResult(), e);
    }

    protected TestResult createResultFromException(TestResult result, Exception e) {
        result.setException(e);
        result.setTestCase(this);

        result.setStatus(TestStatus.FAILED);

        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setExpectedExceptionClass(Class<?> expectedExceptionClass) {
        this.expectedExceptionClass = expectedExceptionClass;
    }

    public Class<?> getExpectedExceptionClass() {
        return expectedExceptionClass;
    }
}
