/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.signalr4j.client.Connection;
import com.github.signalr4j.client.ConnectionBase;
import com.github.signalr4j.client.Constants;
import com.github.signalr4j.client.MessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class TransportHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportHelper.class);

    public static MessageResult processReceivedData(String data, ConnectionBase connection) throws JsonProcessingException {
        MessageResult result = new MessageResult();

        if (data == null) {
            return result;
        }

        data = data.trim();

        if ("".equals(data)) {
            return result;
        }

        JsonNode json = Connection.MAPPER.readTree(data);


        if (json.size() == 0) {
            return result;
        }

        if (json.get("I") != null) {
            LOGGER.trace("Invoking message received with: {}", json);
            connection.onReceived(json);
        } else {

            // disconnected
            if (json.get("D") != null) {
                if (json.get("D").intValue() == 1) {
                    LOGGER.trace("Disconnect message received");
                    result.setDisconnect(true);
                    return result;
                }
            }

            // should reconnect
            if (json.get("T") != null) {
                if (json.get("T").intValue() == 1) {
                    LOGGER.trace("Reconnect message received");
                    result.setReconnect(true);
                }
            }

            if (json.get("G") != null) {
                String groupsToken = json.get("G").textValue();
                LOGGER.trace("Group token received: {}" + groupsToken);
                connection.setGroupsToken(groupsToken);
            }

            JsonNode messages = json.get("M");
            if (messages != null && messages.isArray()) {
                if (json.get("C") != null) {
                    String messageId = json.get("C").textValue();
                    LOGGER.trace("MessageId received: {}", messageId);
                    connection.setMessageId(messageId);
                }

                for (JsonNode message : messages) {
                    LOGGER.trace("Invoking OnReceived with: {}", message);
                    connection.onReceived(message);
                }
            }

            if (json.get("S") != null) {
                if (json.get("S").intValue() == 1) {
                    LOGGER.debug("Initialization message received");
                    result.setInitialize(true);
                }
            }
        }

        return result;
    }

    /**
     * Creates the query string used on receive
     *
     * @param transport  Transport to use
     * @param connection Current connection
     * @return The querystring
     */
    public static String getReceiveQueryString(ClientTransport transport, ConnectionBase connection) {
        StringBuilder qsBuilder = new StringBuilder();

        qsBuilder.append("?transport=").append(transport.getName())
                .append("&connectionToken=").append(urlEncode(connection.getConnectionToken()));

        qsBuilder.append("&connectionId=").append(urlEncode(connection.getConnectionId()));

        if (connection.getMessageId() != null) {
            qsBuilder.append("&messageId=").append(urlEncode(connection.getMessageId()));
        }

        if (connection.getGroupsToken() != null) {
            qsBuilder.append("&groupsToken=").append(urlEncode(connection.getGroupsToken()));
        }

        String connectionData = connection.getConnectionData();
        if (connectionData != null) {
            qsBuilder.append("&connectionData=").append(urlEncode(connectionData));
        }

        String customQuery = connection.getQueryString();

        if (customQuery != null) {
            qsBuilder.append("&").append(customQuery);
        }

        return qsBuilder.toString();
    }

    /**
     * Creates the query string used on sending
     *
     * @param connection current connection
     * @return The querystring
     */
    public static String getNegotiateQueryString(ConnectionBase connection) {
        StringBuilder qsBuilder = new StringBuilder();
        qsBuilder.append("?clientProtocol=").append(urlEncode(Connection.PROTOCOL_VERSION.toString()));

        if (connection.getConnectionData() != null) {
            qsBuilder.append("&connectionData=").append(urlEncode(connection.getConnectionData()));
        }

        if (connection.getQueryString() != null) {
            qsBuilder.append("&").append(connection.getQueryString());
        }

        return qsBuilder.toString();
    }

    /**
     * Creates the query string used on sending
     *
     * @param transport  the transport to use
     * @param connection current connection
     * @return The querystring
     */
    public static String getSendQueryString(ClientTransport transport, ConnectionBase connection) {
        StringBuilder qsBuilder = new StringBuilder();
        qsBuilder.append("?transport=").append(TransportHelper.urlEncode(transport.getName()));

        qsBuilder.append("&connectionToken=").append(TransportHelper.urlEncode(connection.getConnectionToken()));

        qsBuilder.append("&connectionId=").append(TransportHelper.urlEncode(connection.getConnectionId()));

        if (connection.getConnectionData() != null) {
            qsBuilder.append("&connectionData=").append(TransportHelper.urlEncode(connection.getConnectionData()));
        }

        if (connection.getQueryString() != null) {
            qsBuilder.append("&").append(connection.getQueryString());
        }

        return qsBuilder.toString();
    }

    public static String urlEncode(String s) {
        if (s == null) {
            return "";
        }

        String encoded = null;
        try {
            encoded = URLEncoder.encode(s, Constants.UTF8_NAME);
        } catch (UnsupportedEncodingException ignored) {
        }

        return encoded;
    }
}
