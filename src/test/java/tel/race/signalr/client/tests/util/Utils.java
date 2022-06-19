/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.util;

import tel.race.signalr.client.Connection;
import tel.race.signalr.client.ConnectionState;
import tel.race.signalr.client.Constants;
import tel.race.signalr.client.http.HttpConnection;
import tel.race.signalr.client.transport.ClientTransport;
import tel.race.signalr.client.transport.LongPollingTransport;
import tel.race.signalr.client.transport.NegotiationResponse;
import tel.race.signalr.client.transport.ServerSentEventsTransport;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

public class Utils {

    public static String encode(String str) {
        try {
            return URLEncoder.encode(str, Constants.UTF8_NAME);
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    public static ClientTransport createTransport(TransportType transportType, HttpConnection httpConnection) {
        if (transportType == TransportType.SERVER_SENT_EVENTS) {
            return new ServerSentEventsTransport(httpConnection);
        } else {
            return new LongPollingTransport(httpConnection);
        }
    }

    public static String getNegotiationResponseContent(NegotiationResponse negotiation) {

        return String
                .format("{\"Url\":\"/signalr\", \"ConnectionToken\":\"%s\", \"ConnectionId\":\"%s\",\"KeepAliveTimeout\":%s,\"DisconnectTimeout\":%s,\"TryWebSockets\":%s,\"WebSocketServerUrl\":\"%s\", \"ProtocolVersion\":\"%s\"}",
                        negotiation.getConnectionToken(), negotiation.getConnectionId(), negotiation.getKeepAliveTimeout(), negotiation.getDisconnectTimeout(),
                        negotiation.shouldTryWebSockets(), negotiation.getUrl(), negotiation.getProtocolVersion());
    }

    public static NegotiationResponse getDefaultNegotiationResponse() {

        NegotiationResponse negotiation = new NegotiationResponse(null);

        negotiation.setConnectionToken(UUID.randomUUID().toString());
        negotiation.setConnectionId(UUID.randomUUID().toString());
        negotiation.setProtocolVersion("1.3");
        negotiation.setDisconnectTimeout(6);
        negotiation.setKeepAliveTimeout(3);
        negotiation.setTryWebSockets(false);
        negotiation.setUrl("/signalr");

        return negotiation;
    }

    public static void addResultHandlersToConnection(Connection connection, final MultiResult result, final boolean throwOnError) {
        connection.connected(() -> result.statesResult.add(ConnectionState.CONNECTED));

        connection.closed(() -> result.statesResult.add(ConnectionState.DISCONNECTED));

        connection.reconnected(() -> result.statesResult.add(ConnectionState.CONNECTED));

        connection.reconnecting(() -> result.statesResult.add(ConnectionState.RECONNECTING));

        connection.received(json -> result.listResult.add(json.toString()));

        connection.error(error -> {
            result.errorsResult.add(error);

            if (throwOnError) {
                throw new RuntimeException(error);
            }
        });
    }

    public static void finishMessage(MockHttpConnection.RequestEntry entry) {
        entry.finishRequest();
        entry.triggerResponse();
    }
}