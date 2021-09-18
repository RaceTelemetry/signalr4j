/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.test.integration.java;

import com.github.signalr4j.client.test.integration.ApplicationContext;
import com.github.signalr4j.client.test.integration.framework.TestCase;
import com.github.signalr4j.client.test.integration.framework.TestExecutionCallback;
import com.github.signalr4j.client.test.integration.framework.TestGroup;
import com.github.signalr4j.client.test.integration.framework.TestResult;
import com.github.signalr4j.client.test.integration.tests.MiscTests;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Program {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("There must be one argument with the server url.");
            return;
        }

        String serverUrl = args[0];
        JavaTestPlatformContext testPlatformContext = new JavaTestPlatformContext(serverUrl);
        testPlatformContext.setLoggingEnabled(false);
        ApplicationContext.setTestPlatformContext(testPlatformContext);

        List<TestGroup> testGroups = new ArrayList<>();
        testGroups.add(new MiscTests());

        List<TestCase> tests = new ArrayList<>();

        for (TestGroup group : testGroups) {
            tests.addAll(group.getTestCases());
        }

        final Scanner scanner = new Scanner(System.in);
        String option = "";
        while (!option.equals("q")) {
            System.out.println("Type a test number to execute the test. 'q' to quit:");

            for (int i = 0; i < tests.size(); i++) {
                System.out.println(i + ". " + tests.get(i).getName());
            }

            option = scanner.next();
            if (!option.equals("q")) {
                int index = -1;
                try {
                    index = Integer.decode(option);
                } catch (NumberFormatException ignored) {
                }

                if (index > -1 && index < tests.size()) {
                    TestCase test = tests.get(index);

                    test.run(new TestExecutionCallback() {

                        @Override
                        public void onTestStart(TestCase test) {
                            System.out.println("Starting test - " + test.getName());
                        }

                        @Override
                        public void onTestGroupComplete(TestGroup group, List<TestResult> results) {
                        }

                        @Override
                        public void onTestComplete(TestCase test, TestResult result) {
                            String extraData = "";
                            if (result.getException() != null) {
                                extraData = " - " + result.getException().toString();
                            }
                            System.out.println("Test completed - " + test.getName() + " - " + result.getStatus() + extraData);
                            System.out.println("Press any key to continue...");
                            scanner.next();
                        }
                    });
                }
            }
        }
    }

}
