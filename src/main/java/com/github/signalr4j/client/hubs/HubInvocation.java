/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.hubs;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class HubInvocation {
    @SerializedName("I")
    private String callbackId;

    @SerializedName("H")
    private String hub;

    @SerializedName("M")
    private String method;

    @SerializedName("A")
    private JsonElement[] args;

    @SerializedName("S")
    private Map<String, JsonElement> state;

    public String getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    public String getHub() {
        return hub;
    }

    public void setHub(String hub) {
        this.hub = hub;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonElement[] getArgs() {
        return args;
    }

    public void setArgs(JsonElement[] args) {
        this.args = args;
    }

    public Map<String, JsonElement> getState() {
        return state;
    }

    public void setState(Map<String, JsonElement> state) {
        this.state = state;
    }
}
