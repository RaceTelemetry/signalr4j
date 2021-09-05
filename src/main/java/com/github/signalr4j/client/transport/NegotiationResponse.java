/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Represents the negotiation response sent by the server in the handshake
 */
public class NegotiationResponse {
    public static final double INVALID_KEEP_ALIVE_TIMEOUT = -1;

    private String connectionId;
    private String connectionToken;
    private String url;
    private String protocolVersion;
    private double disconnectTimeout;
    private boolean tryWebSockets;
    private double keepAliveTimeout;

    /**
     * Initializes the negotiation response with Json data
     *
     * @param jsonContent Json data
     */
    public NegotiationResponse(String jsonContent, JsonParser parser) {
        if (jsonContent == null || "".equals(jsonContent)) {
            return;
        }

        JsonObject json = parser.parse(jsonContent).getAsJsonObject();

        setConnectionId(json.get("ConnectionId").getAsString());
        setConnectionToken(json.get("ConnectionToken").getAsString());
        setUrl(json.get("Url").getAsString());
        setProtocolVersion(json.get("ProtocolVersion").getAsString());
        setDisconnectTimeout(json.get("DisconnectTimeout").getAsDouble());
        setTryWebSockets(json.get("TryWebSockets").getAsBoolean());

        JsonElement keepAliveElement = json.get("KeepAliveTimeout");
        if (keepAliveElement != null && !keepAliveElement.isJsonNull()) {
            setKeepAliveTimeout(keepAliveElement.getAsDouble());
        } else {
            setKeepAliveTimeout(INVALID_KEEP_ALIVE_TIMEOUT);
        }

    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionToken() {
        return connectionToken;
    }

    public void setConnectionToken(String connectionToken) {
        this.connectionToken = connectionToken;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public double getDisconnectTimeout() {
        return disconnectTimeout;
    }

    public void setDisconnectTimeout(double disconnectTimeout) {
        this.disconnectTimeout = disconnectTimeout;
    }

    public boolean shouldTryWebSockets() {
        return tryWebSockets;
    }

    public void setTryWebSockets(boolean tryWebSockets) {
        this.tryWebSockets = tryWebSockets;
    }

    public double getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(double keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }
}
