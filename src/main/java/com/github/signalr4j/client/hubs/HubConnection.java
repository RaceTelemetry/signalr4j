/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.hubs;

import com.github.signalr4j.client.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a SignalRConnection that implements the Hubs protocol
 */
public class HubConnection extends Connection {

    private final Map<String, Action<HubResult>> callbacks = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, HubProxy> hubs = Collections.synchronizedMap(new HashMap<>());
    private Integer callbackId = 0;

    /**
     * Initializes the connection
     *
     * @param url
     *            The connection URL
     * @param queryString
     *            The connection query string
     * @param useDefaultUrl
     *            indicates if the default SignalR URL should be used
     * @param logger
     *            The connection logger
     */
    public HubConnection(String url, String queryString, boolean useDefaultUrl, Logger logger) {
        super(getUrl(url, useDefaultUrl), queryString, logger);
    }

    /**
     * Initialized the connection
     * 
     * @param url
     *            The connection URL
     */
    public HubConnection(String url) {
        super(getUrl(url, true));
    }

    /**
     * Initializes the connection
     * 
     * @param url
     *            The connection URL
     * @param useDefaultUrl
     *            indicates if the default SignalR URL should be used
     */
    public HubConnection(String url, boolean useDefaultUrl) {
        super(getUrl(url, useDefaultUrl));
    }

    @Override
    public void onReceived(JsonElement message) {
        super.onReceived(message);

        log("Processing message", LogLevel.INFORMATION);
        if (getState() == ConnectionState.CONNECTED) {
            if (message.isJsonObject() && message.getAsJsonObject().has("I")) {
                log("Getting HubResult from message", LogLevel.VERBOSE);
                HubResult result = gson.fromJson(message, HubResult.class);

                String id = result.getId().toLowerCase(Locale.getDefault());
                log("Result Id: " + id, LogLevel.VERBOSE);
                log("Result Data: " + result.getResult(), LogLevel.VERBOSE);

                if (callbacks.containsKey(id)) {
                    log("Get and remove callback with id: " + id, LogLevel.VERBOSE);
                    Action<HubResult> callback = callbacks.remove(id);

                    try {
                        log("Execute callback for message", LogLevel.VERBOSE);
                        callback.run(result);
                    } catch (Exception e) {
                        onError(e, false);
                    }
                }
            } else {
                HubInvocation invocation = gson.fromJson(message, HubInvocation.class);
                log("Getting HubInvocation from message", LogLevel.VERBOSE);

                String hubName = invocation.getHub().toLowerCase(Locale.getDefault());
                log("Message for: " + hubName, LogLevel.VERBOSE);

                if (hubs.containsKey(hubName)) {
                    HubProxy hubProxy = hubs.get(hubName);
                    if (invocation.getState() != null) {
                        for (String key : invocation.getState().keySet()) {
                            JsonElement value = invocation.getState().get(key);
                            log("Setting state for hub: " + key + " -> " + value, LogLevel.VERBOSE);
                            hubProxy.setState(key, value);
                        }
                    }

                    String eventName = invocation.getMethod().toLowerCase(Locale.getDefault());
                    log("Invoking event: " + eventName + " with arguments " + arrayToString(invocation.getArgs()), LogLevel.VERBOSE);
    
                    try {
                        hubProxy.invokeEvent(eventName, invocation.getArgs());
                    } catch (Exception e) {
                        onError(e, false);
                    }
                }
            }
        }
    }

    private static String arrayToString(JsonElement[] args) {
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
        JsonArray jsonArray = new JsonArray();

        for (String hubName : hubs.keySet()) {
            JsonObject element = new JsonObject();
            element.addProperty("name", hubName);
            jsonArray.add(element);
        }

        String connectionData = jsonArray.toString();

        log("Getting connection data: " + connectionData, LogLevel.VERBOSE);
        return connectionData;
    }

    @Override
    protected void onClosed() {
        clearInvocationCallbacks("Connection closed");
        super.onClosed();
    }

    private void clearInvocationCallbacks(String error) {
        log("Clearing invocation callbacks: " + error, LogLevel.VERBOSE);
        HubResult result = new HubResult();
        result.setError(error);

        for (String key : callbacks.keySet()) {
            try {
                log("Invoking callback with empty result: " + key, LogLevel.VERBOSE);
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
     * @param hubName
     *            The hub name
     * @return The proxy for the hub
     * @throws InvalidStateException
     *             If called when not disconnected, the method will throw an
     *             exception
     */
    public HubProxy createHubProxy(String hubName) {
        if (state != ConnectionState.DISCONNECTED) {
            throw new InvalidStateException(state);
        }

        if (hubName == null) {
            throw new IllegalArgumentException("hubName cannot be null");
        }

        String hubNameLower = hubName.toLowerCase(Locale.getDefault());

        log("Creating hub proxy: " + hubNameLower, LogLevel.INFORMATION);

        HubProxy proxy;
        if (hubs.containsKey(hubNameLower)) {
            proxy = hubs.get(hubNameLower);
        } else {
            proxy = new HubProxy(this, hubName, getLogger());
            hubs.put(hubNameLower, proxy);
        }

        return proxy;
    }

    /**
     * Registers a callback
     * 
     * @param callback
     *            The callback to register
     * @return The callback Id
     */
    String registerCallback(Action<HubResult> callback) {
        String id = callbackId.toString().toLowerCase(Locale.getDefault());
        log("Registering callback: " + id, LogLevel.VERBOSE);
        callbacks.put(id, callback);
        callbackId++;
        return id;
    }

    /**
     * Removes a callback
     * 
     * @param callbackId
     *            Id for the callback to remove
     */
    void removeCallback(String callbackId) {
        log("Removing callback: " + callbackId, LogLevel.VERBOSE);
        callbacks.remove(callbackId.toLowerCase(Locale.getDefault()));
    }

    /**
     * Generates a standarized URL
     * 
     * @param url
     *            The base URL
     * @param useDefaultUrl
     *            Indicates if the default SignalR suffix should be appended
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
