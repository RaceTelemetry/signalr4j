/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.hubs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tel.race.signalr.client.Connection;
import tel.race.signalr.client.SignalRFuture;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Proxy for hub operations
 */
public class HubProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(HubProxy.class);

    private final String name;

    private final HubConnection connection;

    private final Map<String, Subscription> subscriptions = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, JsonNode> state = Collections.synchronizedMap(new HashMap<>());

    private static final List<String> EXCLUDED_METHODS = Arrays.asList("equals", "getClass", "hashCode", "notify",
            "notifyAll", "toString", "wait");

    private static final String SUBSCRIPTION_HANDLER_METHOD = "run";

    /**
     * Initializes the HubProxy
     *
     * @param connection HubConnection to use
     * @param name       Hub name
     */
    protected HubProxy(HubConnection connection, String name) {
        this.connection = connection;
        this.name = name;
    }

    /**
     * Sets the state for a key
     *
     * @param key   Key to set
     * @param state State to set
     */
    public void setState(String key, JsonNode state) {
        this.state.put(key, state);
    }

    /**
     * Gets the state for a key
     *
     * @param key Key to get
     * @return state node
     */
    public JsonNode getState(String key) {
        return state.get(key);
    }

    /**
     * Gets the value for a key
     *
     * @param <E>   Class to map result value to
     * @param key   Key to get
     * @param clazz Class used to deserialize the value
     * @return value mapped to generic class
     * @throws JsonProcessingException Throws exception when JSON cannot be mapped to the given class
     */
    public <E> E getValue(String key, Class<E> clazz) throws JsonProcessingException {
        return Connection.MAPPER.treeToValue(getState(key), clazz);
    }

    /**
     * Creates a subscription to an event
     *
     * @param eventName The name of the event
     * @return The subscription object
     */
    public Subscription subscribe(String eventName) {
        LOGGER.debug("Subscribe to event {}", eventName);
        if (eventName == null) {
            throw new IllegalArgumentException("eventName cannot be null");
        }

        eventName = eventName.toLowerCase(Locale.getDefault());

        Subscription subscription;
        if (subscriptions.containsKey(eventName)) {
            LOGGER.debug("Adding event to existing subscription: {}", eventName);
            subscription = subscriptions.get(eventName);
        } else {
            LOGGER.debug("Creating new subscription for: {}", eventName);
            subscription = new Subscription();
            subscriptions.put(eventName, subscription);
        }

        return subscription;
    }

    /**
     * Create subscriptions for all the object methods
     *
     * @param handler Handler for the hub messages
     */
    public void subscribe(final Object handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }

        Method[] methods = handler.getClass().getMethods();

        for (final Method method : methods) {
            if (!EXCLUDED_METHODS.contains(method.getName())) {
                Subscription subscription = subscribe(method.getName());
                subscription.addReceivedHandler(eventParameters -> {
                    LOGGER.trace("Handling dynamic subscription: {}", method.getName());
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != eventParameters.length) {
                        throw new RuntimeException("The handler  '" + handler.getClass() + "' has " + parameterTypes.length + " parameters, but there are " + eventParameters.length
                                + " values.");
                    }

                    Object[] parameters = new Object[parameterTypes.length];

                    for (int i = 0; i < eventParameters.length; i++) {
                        parameters[i] = Connection.MAPPER.treeToValue(eventParameters[i], parameterTypes[i]);
                    }
                    method.setAccessible(true);
                    LOGGER.trace("Invoking method for dynamic subscription: {}" + method.getName());
                    method.invoke(handler, parameters);
                });
            }
        }
    }

    /**
     * Removes all the subscriptions attached to an event
     *
     * @param eventName the event
     */
    public void removeSubscription(String eventName) {
        if (eventName != null) {
            subscriptions.remove(eventName.toLowerCase(Locale.getDefault()));
        }
    }

    /**
     * Invokes a hub method
     *
     * @param method Method name
     * @param args   Method arguments
     * @return A Future for the operation
     */
    public SignalRFuture<Void> invoke(String method, Object... args) {
        return invoke(null, method, args);
    }

    /**
     * Invokes a hub method that returns a value
     *
     * @param <E> Class to map result value to
     * @param resultClass class that will be used to map the result JSON
     * @param method Method name
     * @param args   Method arguments
     * @return A Future for the operation, that will return the method result
     */
    public <E> SignalRFuture<E> invoke(final Class<E> resultClass, final String method, Object... args) {
        if (method == null) {
            throw new IllegalArgumentException("method cannot be null");
        }

        if (args == null) {
            throw new IllegalArgumentException("args cannot be null");
        }

        LOGGER.debug("Invoking method on hub: {}", method);

        JsonNode[] jsonArguments = new JsonNode[args.length];

        for (int i = 0; i < args.length; i++) {
            jsonArguments[i] = Connection.MAPPER.valueToTree(args[i]);
        }

        final SignalRFuture<E> resultFuture = new SignalRFuture<>();

        final String callbackId = connection.registerCallback(result -> {
            LOGGER.debug("Executing invocation callback for: {}", method);
            if (result != null) {
                if (result.getError() != null) {
                    if (result.isHubException()) {
                        resultFuture.triggerError(new HubException(result.getError(), result.getErrorData()));
                    } else {
                        resultFuture.triggerError(new Exception(result.getError()));
                    }
                } else {
                    boolean errorHappened = false;
                    E resultObject = null;
                    try {
                        if (result.getState() != null) {
                            for (String key : result.getState().keySet()) {
                                setState(key, result.getState().get(key));
                            }
                        }

                        if (result.getResult() != null && resultClass != null) {
                            LOGGER.debug("Found result invoking method on hub: {}", result.getResult());
                            resultObject = Connection.MAPPER.treeToValue(result.getResult(), resultClass);
                        }
                    } catch (Exception e) {
                        errorHappened = true;
                        resultFuture.triggerError(e);
                    }

                    if (!errorHappened) {
                        try {
                            resultFuture.setResult(resultObject);
                        } catch (Exception e) {
                            resultFuture.triggerError(e);
                        }
                    }
                }
            }
        });

        HubInvocation hubData = new HubInvocation();
        hubData.setHub(name);
        hubData.setMethod(method);
        hubData.setArgs(jsonArguments);
        hubData.setCallbackId(callbackId);

        if (state.size() != 0) {
            hubData.setState(state);
        }

        final SignalRFuture<Void> sendFuture = connection.send(hubData);

        resultFuture.onCancelled(() -> connection.removeCallback(callbackId));

        resultFuture.onError(sendFuture::triggerError);

        return resultFuture;
    }

    /**
     * Invokes a hub event with argument
     *
     * @param eventName The name of the event
     * @param args      The event args
     */
    void invokeEvent(String eventName, JsonNode[] args) throws Exception {
        if (eventName == null) {
            throw new IllegalArgumentException("eventName cannot be null");
        }

        eventName = eventName.toLowerCase(Locale.getDefault());

        if (subscriptions.containsKey(eventName)) {
            Subscription subscription = subscriptions.get(eventName);
            subscription.onReceived(args);
        }
    }

    private <E1, E2, E3, E4, E5> void on(String eventName, final SubscriptionHandler5<E1, E2, E3, E4, E5> handler, final Class<?>... parameterTypes) {
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }

        Subscription subscription = subscribe(eventName);
        subscription.addReceivedHandler(eventParameters -> {
            Method method = null;

            for (Method m : handler.getClass().getMethods()) {
                if (m.getName().equals(SUBSCRIPTION_HANDLER_METHOD)) {
                    method = m;
                    break;
                }
            }

            if (parameterTypes.length != eventParameters.length) {
                throw new RuntimeException("The handler '" + eventName + "' has " + parameterTypes.length + " parameters, but there are " + eventParameters.length + " values.");
            }

            Object[] parameters = new Object[5];

            for (int i = 0; i < eventParameters.length; i++) {
                parameters[i] = Connection.MAPPER.treeToValue(eventParameters[i], parameterTypes[i]);
            }
            method.setAccessible(true);
            method.invoke(handler, parameters);
        });
    }

    public <E1, E2, E3, E4, E5> void on(String eventName, final SubscriptionHandler5<E1, E2, E3, E4, E5> handler, Class<E1> parameter1, Class<E2> parameter2,
                                        Class<E3> parameter3, Class<E4> parameter4, Class<E5> parameter5) {
        on(eventName, handler::run, parameter1, parameter2, parameter3, parameter4, parameter5);
    }

    public <E1, E2, E3, E4> void on(String eventName, final SubscriptionHandler4<E1, E2, E3, E4> handler, Class<E1> parameter1, Class<E2> parameter2,
                                    Class<E3> parameter3, Class<E4> parameter4) {
        on(eventName, (SubscriptionHandler5<E1, E2, E3, E4, Void>) (p1, p2, p3, p4, p5) -> handler.run(p1, p2, p3, p4), parameter1, parameter2, parameter3, parameter4);
    }

    public <E1, E2, E3> void on(String eventName, final SubscriptionHandler3<E1, E2, E3> handler, Class<E1> parameter1, Class<E2> parameter2,
                                Class<E3> parameter3) {
        on(eventName, (SubscriptionHandler5<E1, E2, E3, Void, Void>) (p1, p2, p3, p4, p5) -> handler.run(p1, p2, p3), parameter1, parameter2, parameter3);
    }

    public <E1, E2> void on(String eventName, final SubscriptionHandler2<E1, E2> handler, Class<E1> parameter1, Class<E2> parameter2) {
        on(eventName, (SubscriptionHandler5<E1, E2, Void, Void, Void>) (p1, p2, p3, p4, p5) -> handler.run(p1, p2), parameter1, parameter2);
    }

    public <E1> void on(String eventName, final SubscriptionHandler1<E1> handler, Class<E1> parameter1) {
        on(eventName, (SubscriptionHandler5<E1, Void, Void, Void, Void>) (p1, p2, p3, p4, p5) -> handler.run(p1), parameter1);
    }

    public <E1> void on(String eventName, final SubscriptionHandler handler) {
        on(eventName, (SubscriptionHandler5<Void, Void, Void, Void, Void>) (p1, p2, p3, p4, p5) -> handler.run());
    }
}
