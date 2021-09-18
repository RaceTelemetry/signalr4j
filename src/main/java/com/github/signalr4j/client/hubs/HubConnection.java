/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.hubs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.signalr4j.client.Action;
import com.github.signalr4j.client.Connection;
import com.github.signalr4j.client.ConnectionState;
import com.github.signalr4j.client.InvalidStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a SignalRConnection that implements the Hubs protocol
 */
public class HubConnection extends Connection {

    private static final Logger LOGGER = LoggerFactory.getLogger(HubConnection.class);

    private final Map<String, Action<HubResult>> callbacks = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, HubProxy> hubs = Collections.synchronizedMap(new HashMap<>());
    private Integer callbackId = 0;

    /**
     * Initializes the connection
     *
     * @param url           The connection URL
     * @param queryString   The connection query string
     * @param useDefaultUrl indicates if the default SignalR URL should be used
     */
    public HubConnection(String url, String queryString, boolean useDefaultUrl) {
        super(getUrl(url, useDefaultUrl), queryString);
    }

    /**
     * Initialized the connection
     *
     * @param url The connection URL
     */
    public HubConnection(String url) {
        super(getUrl(url, true));
    }

    /**
     * Initializes the connection
     *
     * @param url           The connection URL
     * @param useDefaultUrl indicates if the default SignalR URL should be used
     */
    public HubConnection(String url, boolean useDefaultUrl) {
        super(getUrl(url, useDefaultUrl));
    }

    @Override
    public void onReceived(JsonNode message) throws JsonProcessingException {
        super.onReceived(message);

        LOGGER.debug("Processing message");
        if (getState() == ConnectionState.CONNECTED) {
            if (message.isObject() && message.has("I")) {
                LOGGER.trace("Getting HubResult from message");
                HubResult result = Connection.MAPPER.treeToValue(message, HubResult.class);

                String id = result.getId().toLowerCase(Locale.getDefault());
                LOGGER.trace("Result Id: {}", id);
                LOGGER.trace("Result Data: {}", result.getResult());

                if (callbacks.containsKey(id)) {
                    LOGGER.trace("Get and remove callback with id: {}", id);
                    Action<HubResult> callback = callbacks.remove(id);

                    try {
                        LOGGER.trace("Execute callback for message");
                        callback.run(result);
                    } catch (Exception e) {
                        onError(e, false);
                    }
                }
            } else {
                HubInvocation invocation = Connection.MAPPER.treeToValue(message, HubInvocation.class);
                LOGGER.trace("Getting HubInvocation from message");

                String hubName = invocation.getHub().toLowerCase(Locale.getDefault());
                LOGGER.trace("Message for: {}", hubName);

                if (hubs.containsKey(hubName)) {
                    HubProxy hubProxy = hubs.get(hubName);
                    if (invocation.getState() != null) {
                        for (String key : invocation.getState().keySet()) {
                            JsonNode value = invocation.getState().get(key);
                            LOGGER.trace("Setting state for hub: {} -> {}", key, value);
                            hubProxy.setState(key, value);
                        }
                    }

                    String eventName = invocation.getMethod().toLowerCase(Locale.getDefault());
                    LOGGER.trace("Invoking event: {} with arguments {}", eventName, arrayToString(invocation.getArgs()));

                    try {
                        hubProxy.invokeEvent(eventName, invocation.getArgs());
                    } catch (Exception e) {
                        onError(e, false);
                    }
                }
            }
        }
    }

    private static String arrayToString(JsonNode[] args) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(args[i].toString());
        }

        sb.append("]");

        return sb.toString();
    }

    @Override
    public String getConnectionData() {
        ArrayNode array = Connection.MAPPER.createArrayNode();

        for (String hubName : hubs.keySet()) {
            array.add(Connection.MAPPER.createObjectNode()
                    .put("name", hubName));
        }

        try {
            return Connection.MAPPER.writeValueAsString(array);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize connection data", e);
        }
    }

    @Override
    protected void onClosed() {
        clearInvocationCallbacks("Connection closed");
        super.onClosed();
    }

    private void clearInvocationCallbacks(String error) {
        LOGGER.trace("Clearing invocation callbacks: {}", error);
        HubResult result = new HubResult();
        result.setError(error);

        for (String key : callbacks.keySet()) {
            try {
                LOGGER.trace("Invoking callback with empty result: {}", key);
                callbacks.get(key).run(result);
            } catch (Exception ignored) {
            }
        }

        callbacks.clear();
    }

    @Override
    protected void onReconnecting() {
        clearInvocationCallbacks("Reconnecting");
        super.onReconnecting();
    }

    /**
     * Creates a proxy for a hub
     *
     * @param hubName The hub name
     * @return The proxy for the hub
     * @throws InvalidStateException If called when not disconnected, the method will throw an
     *                               exception
     */
    public HubProxy createHubProxy(String hubName) {
        if (state != ConnectionState.DISCONNECTED) {
            throw new InvalidStateException(state);
        }

        if (hubName == null) {
            throw new IllegalArgumentException("hubName cannot be null");
        }

        String hubNameLower = hubName.toLowerCase(Locale.getDefault());

        LOGGER.debug("Creating hub proxy: {}", hubNameLower);

        HubProxy proxy;
        if (hubs.containsKey(hubNameLower)) {
            proxy = hubs.get(hubNameLower);
        } else {
            proxy = new HubProxy(this, hubName);
            hubs.put(hubNameLower, proxy);
        }

        return proxy;
    }

    /**
     * Registers a callback
     *
     * @param callback The callback to register
     * @return The callback Id
     */
    String registerCallback(Action<HubResult> callback) {
        String id = callbackId.toString().toLowerCase(Locale.getDefault());
        LOGGER.trace("Registering callback: {}", id);
        callbacks.put(id, callback);
        callbackId++;
        return id;
    }

    /**
     * Removes a callback
     *
     * @param callbackId Id for the callback to remove
     */
    void removeCallback(String callbackId) {
        LOGGER.trace("Removing callback: {}", callbackId);
        callbacks.remove(callbackId.toLowerCase(Locale.getDefault()));
    }

    /**
     * Generates a standarized URL
     *
     * @param url           The base URL
     * @param useDefaultUrl Indicates if the default SignalR suffix should be appended
     * @return The connection URL
     */
    private static String getUrl(String url, boolean useDefaultUrl) {
        if (!url.endsWith("/")) {
            url += "/";
        }

        if (useDefaultUrl) {
            return url + "signalr";
        }

        return url;
    }

    @Override
    protected String getSourceNameForLog() {
        return "HubConnection";
    }
}
