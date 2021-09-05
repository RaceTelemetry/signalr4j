/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.hubs;

import com.github.signalr4j.client.Action;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a subscription to a message
 */
public class Subscription {
    private final List<Action<JsonElement[]>> received = new ArrayList<>();

    /**
     * Triggers the "Received" event
     *
     * @param data Event data
     */
    void onReceived(JsonElement[] data) throws Exception {
        for (Action<JsonElement[]> handler : received) {
            handler.run(data);
        }
    }

    /**
     * Add a handler to the "Received" event
     *
     * @param received Event handler
     */
    public void addReceivedHandler(Action<JsonElement[]> received) {
        this.received.add(received);
    }
}
