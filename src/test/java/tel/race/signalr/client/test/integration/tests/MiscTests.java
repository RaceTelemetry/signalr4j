/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.test.integration.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import tel.race.signalr.client.ConnectionState;
import tel.race.signalr.client.hubs.HubConnection;
import tel.race.signalr.client.hubs.HubException;
import tel.race.signalr.client.hubs.HubProxy;
import tel.race.signalr.client.test.integration.ApplicationContext;
import tel.race.signalr.client.test.integration.TransportType;
import tel.race.signalr.client.test.integration.framework.*;
import tel.race.signalr.client.transport.ClientTransport;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class MiscTests extends TestGroup {

    private static final String INTEGRATION_TESTS_HUB_NAME = "integrationTestsHub";

    private TestCase createBasicConnectionFlowTest(String name, final TransportType transportType) {
        TestCase test = new TestCase() {

            private InternalTestData testData;

            @Override
            public TestResult executeTest() {
                try {
                    final HubConnection connection = ApplicationContext.createHubConnection();
                    ClientTransport transport = ApplicationContext.createTransport(transportType);

                    testData = new InternalTestData();
                    testData.connectionStates.add(connection.getState());
                    connection.stateChanged((oldState, newState) -> testData.connectionStates.add(newState));

                    connection.received(json -> testData.receivedMessages.add(json));

                    final Semaphore semaphore = new Semaphore(0);
                    HubProxy proxy = connection.createHubProxy(INTEGRATION_TESTS_HUB_NAME);
                    connection.closed(() -> {
                        testData.connectionWasClosed = true;
                        semaphore.release();
                    });

                    proxy.subscribe(new Object() {

                        @SuppressWarnings("unused")
                        public void Echo(String data) {
                            testData.receivedData.add(data);
                        }
                    });

                    String data = UUID.randomUUID().toString();

                    connection.start(transport).get();
                    proxy.setState("myVar", new IntNode(1));
                    proxy.invoke("echo", data);
                    proxy.invoke("updateState", "myVar", 2);

                    ApplicationContext.sleep();

                    connection.stop();

                    semaphore.acquire();

                    ApplicationContext.sleep();

                    TestResult result = new TestResult();
                    result.setStatus(TestStatus.PASSED);
                    result.setTestCase(this);

                    //validations

                    if (!Util.compareArrays(
                            new ConnectionState[]{
                                    ConnectionState.DISCONNECTED,
                                    ConnectionState.CONNECTING,
                                    ConnectionState.CONNECTED,
                                    ConnectionState.DISCONNECTED},
                            testData.connectionStates.toArray())) {
                        return createResultFromException(new Exception("The connection states were incorrect"));
                    }

                    if (testData.receivedMessages.size() == 0) {
                        return createResultFromException(new Exception("Messages not received"));
                    }

                    if (!testData.connectionWasClosed) {
                        return createResultFromException(new Exception("Conneciton was not closed"));
                    }

                    if (testData.receivedData.size() != 1 || !testData.receivedData.get(0).toString().equals(data)) {
                        return createResultFromException(new Exception("Invalid received data"));
                    }

                    // pending: validate tracing messages

                    return result;
                } catch (Exception e) {
                    return createResultFromException(e);
                }
            }
        };

        test.setName(name);

        return test;
    }

    static class InternalTestData {
        List<ConnectionState> connectionStates = new ArrayList<>();
        List<JsonNode> receivedMessages = new ArrayList<>();
        boolean connectionWasClosed = false;
        List<Throwable> errors = new ArrayList<>();
        List<Object> receivedData = new ArrayList<>();
    }

    private TestCase createErrorHandledAndConnectionContinuesTest(String name, final TransportType transportType) {
        TestCase test = new TestCase() {

            InternalTestData testData;

            @Override
            public TestResult executeTest() {
                try {
                    HubConnection connection = ApplicationContext.createHubConnection();
                    ClientTransport transport = ApplicationContext.createTransport(transportType);

                    testData = new InternalTestData();

                    connection.received(json -> testData.receivedMessages.add(json));

                    connection.error(error -> testData.errors.add(error));

                    HubProxy proxy = connection.createHubProxy(INTEGRATION_TESTS_HUB_NAME);

                    proxy.subscribe(new Object() {
                        @SuppressWarnings("unused")
                        public void echo(String data) {
                            testData.receivedData.add(data);
                        }
                    });

                    String data = UUID.randomUUID().toString();

                    connection.start(transport).get();

                    proxy.invoke("triggerError");

                    ApplicationContext.sleep();

                    proxy.invoke("echo", data);

                    ApplicationContext.sleep();

                    connection.stop();

                    ApplicationContext.sleep();

                    TestResult result = new TestResult();
                    result.setStatus(TestStatus.PASSED);
                    result.setTestCase(this);

                    //validations

                    if (testData.receivedMessages.size() == 0) {
                        return createResultFromException(new Exception("Messages expected"));
                    }

                    if (testData.errors.size() != 1 || testData.errors.get(0).getClass() != HubException.class) {
                        return createResultFromException(new Exception("Expected one error"));
                    }

                    if (testData.receivedData.size() != 1 || !testData.receivedData.get(0).toString().equals(data)) {
                        return createResultFromException(new Exception("Invalid received data"));
                    }

                    // pending: validate tracing messages

                    return result;
                } catch (Exception e) {
                    return createResultFromException(e);
                }
            }
        };

        test.setName(name);

        return test;
    }

    private TestCase createMessagesToGroupsTest(String name, final TransportType transportType) {
        TestCase test = new TestCase() {

            InternalTestData testData;

            @Override
            public TestResult executeTest() {
                try {
                    HubConnection connection = ApplicationContext.createHubConnection();
                    ClientTransport transport = ApplicationContext.createTransport(transportType);

                    testData = new InternalTestData();

                    HubProxy proxy = connection.createHubProxy(INTEGRATION_TESTS_HUB_NAME);

                    proxy.subscribe(new Object() {
                        @SuppressWarnings("unused")
                        public void echo(String data) {
                            testData.receivedData.add(data);
                        }
                    });

                    connection.start(transport).get();

                    proxy.invoke("sendMessageToGroup", "group1", "message1").get();

                    ApplicationContext.sleep();

                    proxy.invoke("joinGroup", "group1").get();

                    ApplicationContext.sleep();

                    proxy.invoke("sendMessageToGroup", "group1", "message2").get();

                    ApplicationContext.sleep();

                    proxy.invoke("leaveGroup", "group1").get();

                    ApplicationContext.sleep();

                    proxy.invoke("sendMessageToGroup", "group1", "message3").get();

                    ApplicationContext.sleep();

                    connection.stop();

                    ApplicationContext.sleep();

                    TestResult result = new TestResult();
                    result.setStatus(TestStatus.PASSED);
                    result.setTestCase(this);

                    //validations

                    if (testData.receivedData.size() != 1 || !testData.receivedData.get(0).equals("message2")) {
                        return createResultFromException(new Exception("Expected only one message with value 'message2'"));
                    }

                    return result;
                } catch (Exception e) {
                    return createResultFromException(e);
                }
            }
        };

        test.setName(name);

        return test;
    }

    private TestCase createDisconnectServerTest(String name, final TransportType transportType) {
        TestCase test = new TestCase() {

            InternalTestData testData;

            @Override
            public TestResult executeTest() {
                try {
                    HubConnection connection = ApplicationContext.createHubConnection();
                    ClientTransport transport = ApplicationContext.createTransport(transportType);

                    testData = new InternalTestData();

                    connection.reconnecting(() -> testData.connectionStates.add(ConnectionState.RECONNECTING));

                    connection.start(transport).get();

                    ApplicationContext.showMessage("Break connection with the server").get();

                    long current = Calendar.getInstance().getTimeInMillis();

                    while (Calendar.getInstance().getTimeInMillis() - current < 60 * 1000) {
                        if (connection.getState() == ConnectionState.DISCONNECTED) {
                            break;
                        }
                    }

                    TestResult result = new TestResult();
                    result.setStatus(TestStatus.PASSED);
                    result.setTestCase(this);

                    ApplicationContext.showMessage("Enable connection with the server").get();

                    //validations

                    if (connection.getState() != ConnectionState.DISCONNECTED) {
                        return createResultFromException(new Exception("Connection should be disconnected"));
                    }

                    if (!testData.connectionStates.contains(ConnectionState.RECONNECTING)) {
                        return createResultFromException(new Exception("The client should have tried to reconnect"));
                    }

                    return result;
                } catch (Exception e) {
                    return createResultFromException(e);
                }
            }
        };

        test.setName(name);

        return test;
    }

    private TestCase createReconnectServerTest(String name, final TransportType transportType) {
        TestCase test = new TestCase() {

            InternalTestData testData;

            @Override
            public TestResult executeTest() {
                try {
                    HubConnection connection = ApplicationContext.createHubConnection();
                    ClientTransport transport = ApplicationContext.createTransport(transportType);

                    testData = new InternalTestData();

                    connection.reconnecting(() -> testData.connectionStates.add(ConnectionState.RECONNECTING));

                    connection.start(transport).get();

                    ApplicationContext.showMessage("Break connection with the server for 10 seconds and re-enable it").get();

                    ApplicationContext.sleep(10);

                    TestResult result = new TestResult();
                    result.setStatus(TestStatus.PASSED);
                    result.setTestCase(this);

                    //validations

                    if (connection.getState() != ConnectionState.CONNECTED) {
                        return createResultFromException(new Exception("Connection should be connected"));
                    }

                    connection.disconnect();

                    if (!testData.connectionStates.contains(ConnectionState.RECONNECTING)) {
                        return createResultFromException(new Exception("The client should have tried to reconnect"));
                    }

                    return result;
                } catch (Exception e) {
                    return createResultFromException(e);
                }
            }
        };

        test.setName(name);

        return test;
    }

    private TestCase createConnectToUnavailableServerTest(String name, final TransportType transportType) {
        TestCase test = new TestCase() {

            InternalTestData testData;

            @Override
            public TestResult executeTest() {
                try {
                    HubConnection connection = ApplicationContext.createHubConnectionWithInvalidURL();
                    ClientTransport transport = ApplicationContext.createTransport(transportType);
                    testData = new InternalTestData();

                    connection.start(transport)
                            .onError(error -> testData.errors.add(error));


                    ApplicationContext.sleep();

                    TestResult result = new TestResult();
                    result.setStatus(TestStatus.PASSED);
                    result.setTestCase(this);

                    //validations

                    if (connection.getState() != ConnectionState.DISCONNECTED) {
                        return createResultFromException(new Exception("Connection should be disconnected"));
                    }

                    if (testData.errors.size() == 0) {
                        return createResultFromException(new Exception("Exception should have been thrown"));
                    }

                    return result;
                } catch (Exception e) {
                    return createResultFromException(e);
                }
            }
        };

        test.setName(name);

        return test;
    }


    private TestCase createPendingCallbacksAbortedTest(String name, final TransportType transportType) {
        TestCase test = new TestCase() {

            InternalTestData testData;

            @Override
            public TestResult executeTest() {
                try {
                    HubConnection connection = ApplicationContext.createHubConnection();
                    ClientTransport transport = ApplicationContext.createTransport(transportType);

                    testData = new InternalTestData();

                    HubProxy proxy = connection.createHubProxy(INTEGRATION_TESTS_HUB_NAME);
                    connection.start(transport).get();

                    proxy.invoke(String.class, "waitAndReturn", 20)
                            .done(obj -> testData.receivedData.add(obj))
                            .onError(error -> testData.errors.add(error));

                    connection.stop();

                    ApplicationContext.sleep(15);

                    TestResult result = new TestResult();
                    result.setStatus(TestStatus.PASSED);
                    result.setTestCase(this);

                    //validations

                    if (System.getProperty("java.runtime.name").toLowerCase().contains("android")) {
                        // outside android, java lib might not break the connection soon enough to avoid receiving the message callback
                        if (testData.receivedData.size() != 0) {
                            return createResultFromException(new Exception("No result should have been received"));
                        }
                    }

                    if (testData.errors.size() == 0) {
                        return createResultFromException(new Exception("Exception should have been thrown when connection was closed"));
                    }

                    return result;
                } catch (Exception e) {
                    return createResultFromException(e);
                }
            }
        };

        test.setName(name);

        return test;
    }

    private TestCase createCheckHeaderTest(String name, final TransportType transportType) {
        TestCase test = new TestCase() {
            @Override
            public TestResult executeTest() {
                try {
                    HubConnection connection = ApplicationContext.createHubConnection();
                    ClientTransport transport = ApplicationContext.createTransport(transportType);

                    final String headerName = UUID.randomUUID().toString();
                    final String headerValue = UUID.randomUUID().toString();
                    connection.setCredentials(request -> request.addHeader(headerName, headerValue));

                    HubProxy proxy = connection.createHubProxy(INTEGRATION_TESTS_HUB_NAME);
                    connection.start(transport).get();

                    String retValue = proxy.invoke(String.class, "HeaderData", headerName).get();

                    connection.stop();

                    TestResult result = new TestResult();
                    result.setStatus(TestStatus.PASSED);
                    result.setTestCase(this);

                    //validations

                    if (!headerValue.equals(retValue)) {
                        return createResultFromException(new ExpectedValueException(headerName, retValue));
                    }

                    return result;
                } catch (Exception e) {
                    return createResultFromException(e);
                }
            }
        };

        test.setName(name);

        return test;
    }


    public MiscTests() {
        super("SignalR tests");

        for (TransportType transportType : TransportType.values()) {
            this.addTest(createBasicConnectionFlowTest("Basic connection flow - " + transportType.name(), transportType));
        }

        for (TransportType transportType : TransportType.values()) {
            this.addTest(createMessagesToGroupsTest("Join and leave groups - " + transportType.name(), transportType));
        }

        for (TransportType transportType : TransportType.values()) {
            this.addTest(createErrorHandledAndConnectionContinuesTest("Error handled and connection continues - " + transportType.name(), transportType));
        }

        for (TransportType transportType : TransportType.values()) {
            this.addTest(createDisconnectServerTest("Disconnect server after connection - " + transportType.name(), transportType));
        }

        for (TransportType transportType : TransportType.values()) {
            this.addTest(createReconnectServerTest("Reconnect server after brief disconnection - " + transportType.name(), transportType));
        }

        for (TransportType transportType : TransportType.values()) {
            this.addTest(createConnectToUnavailableServerTest("Connect to unavailable server - " + transportType.name(), transportType));
        }

        for (TransportType transportType : TransportType.values()) {
            this.addTest(createPendingCallbacksAbortedTest("Pending callbacks aborted - " + transportType.name(), transportType));
        }

        for (TransportType transportType : TransportType.values()) {
            this.addTest(createCheckHeaderTest("Check headers - " + transportType.name(), transportType));
        }

    }

}
