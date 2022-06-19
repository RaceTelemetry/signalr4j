/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.hubs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Map;

/**
 * Represents the result of a hub operation
 */
public class HubResult {
    @JsonProperty("I")
    private String id;

    @JsonProperty("R")
    private JsonNode result;

    @JsonProperty("H")
    private boolean isHubException;

    @JsonProperty("E")
    private String error;

    @JsonProperty("D")
    private Object errorData;

    @JsonProperty("S")
    private Map<String, JsonNode> state;

    public String getId() {
        return id == null ? null : id.toLowerCase(Locale.getDefault());
    }

    public void setId(String id) {
        this.id = id;
    }

    public JsonNode getResult() {
        return result;
    }

    public void setResult(JsonNode result) {
        this.result = result;
    }

    public boolean isHubException() {
        return isHubException;
    }

    public void setIsHubException(boolean isHubException) {
        this.isHubException = isHubException;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getErrorData() {
        return errorData;
    }

    public void setErrorData(Object errorData) {
        this.errorData = errorData;
    }

    public Map<String, JsonNode> getState() {
        return state;
    }

    public void setState(Map<String, JsonNode> state) {
        this.state = state;
    }
}
