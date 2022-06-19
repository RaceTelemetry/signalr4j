/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.hubs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class HubInvocation {
    @JsonProperty("I")
    private String callbackId;

    @JsonProperty("H")
    private String hub;

    @JsonProperty("M")
    private String method;

    @JsonProperty("A")
    private JsonNode[] args;

    @JsonProperty("S")
    private Map<String, JsonNode> state;

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

    public JsonNode[] getArgs() {
        return args;
    }

    public void setArgs(JsonNode[] args) {
        this.args = args;
    }

    public Map<String, JsonNode> getState() {
        return state;
    }

    public void setState(Map<String, JsonNode> state) {
        this.state = state;
    }
}
