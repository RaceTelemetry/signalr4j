/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.signalr4j.client.Connection;

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
    public NegotiationResponse(String jsonContent) {
        if (jsonContent == null || "".equals(jsonContent)) {
            return;
        }

        JsonNode json;
        try {
            json = Connection.MAPPER.readTree(jsonContent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialize negotiation response", e);
        }

        setConnectionId(json.get("ConnectionId").textValue());
        setConnectionToken(json.get("ConnectionToken").textValue());
        setUrl(json.get("Url").textValue());
        setProtocolVersion(json.get("ProtocolVersion").textValue());
        setDisconnectTimeout(json.get("DisconnectTimeout").doubleValue());
        setTryWebSockets(json.get("TryWebSockets").booleanValue());

        JsonNode keepAliveElement = json.get("KeepAliveTimeout");
        if (keepAliveElement != null && !keepAliveElement.isNull()) {
            setKeepAliveTimeout(keepAliveElement.doubleValue());
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
