/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.hubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.signalr4j.client.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a subscription to a message
 */
public class Subscription {
    private final List<Action<JsonNode[]>> received = new ArrayList<>();

    /**
     * Triggers the "Received" event
     *
     * @param data Event data
     */
    void onReceived(JsonNode[] data) throws Exception {
        for (Action<JsonNode[]> handler : received) {
            handler.run(data);
        }
    }

    /**
     * Add a handler to the "Received" event
     *
     * @param received Event handler
     */
    public void addReceivedHandler(Action<JsonNode[]> received) {
        this.received.add(received);
    }
}
