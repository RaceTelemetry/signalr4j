/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.util;

import com.fasterxml.jackson.databind.JsonNode;
import tel.race.signalr.client.*;
import tel.race.signalr.client.http.Request;
import tel.race.signalr.client.transport.ClientTransport;

import java.util.HashMap;
import java.util.Map;

public class MockConnection implements ConnectionBase {

    private Credentials credentials;

    @Override
    public String getUrl() {
        return "https://myUrl.com/signalr/";
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void reconnecting(Runnable handler) {
    }

    @Override
    public void reconnected(Runnable handler) {
    }

    @Override
    public void connected(Runnable handler) {
    }

    @Override
    public void error(ErrorCallback handler) {
    }

    @Override
    public void received(MessageReceivedHandler handler) {
    }

    @Override
    public void connectionSlow(Runnable handler) {
    }

    @Override
    public void closed(Runnable handler) {
    }

    @Override
    public String getConnectionToken() {
        return "$CONNECTIONTOKEN";
    }

    @Override
    public String getConnectionId() {
        return "$CONNECTIONID";
    }

    @Override
    public String getQueryString() {
        return "val=1";
    }

    @Override
    public String getMessageId() {
        return "$MESSAGEID";
    }

    @Override
    public String getGroupsToken() {
        return "$GROUPSTOKEN";
    }

    @Override
    public String getConnectionData() {
        return "$CONNECTIONDATA";
    }

    @Override
    public ConnectionState getState() {
        return ConnectionState.CONNECTED;
    }

    @Override
    public SignalRFuture<Void> start(ClientTransport transport) {
        return null;
    }

    @Override
    public void stop() {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public SignalRFuture<Void> send(String data) {
        return null;
    }

    @Override
    public void prepareRequest(Request request) {
    }

    @Override
    public Map<String, String> getHeaders() {
        return new HashMap<>();
    }

    @Override
    public void setMessageId(String messageId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setReconnectOnError(boolean reconnectOnError) {

    }

    @Override
    public void setGroupsToken(String groupsToken) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(Throwable error, boolean mustCleanCurrentConnection) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReceived(JsonNode message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void stateChanged(StateChangedCallback handler) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addHeader(String headerName, String headerValue) {
        // TODO Auto-generated method stub

    }

}
