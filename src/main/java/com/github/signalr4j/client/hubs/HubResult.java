/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.hubs;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Locale;
import java.util.Map;

/**
 * Represents the result of a hub operation
 */
public class HubResult {
    @SerializedName("I")
    private String id;

    @SerializedName("R")
    private JsonElement result;

    @SerializedName("H")
    private boolean isHubException;

    @SerializedName("E")
    private String error;

    @SerializedName("D")
    private Object errorData;

    @SerializedName("S")
    private Map<String, JsonElement> state;

    public String getId() {
        return id == null ? null : id.toLowerCase(Locale.getDefault());
    }

    public void setId(String id) {
        this.id = id;
    }

    public JsonElement getResult() {
        return result;
    }

    public void setResult(JsonElement result) {
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

    public Map<String, JsonElement> getState() {
        return state;
    }

    public void setState(Map<String, JsonElement> state) {
        this.state = state;
    }
}
