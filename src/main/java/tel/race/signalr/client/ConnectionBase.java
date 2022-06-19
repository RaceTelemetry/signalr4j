/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import tel.race.signalr.client.http.Request;
import tel.race.signalr.client.transport.ClientTransport;

import java.util.Map;

public interface ConnectionBase {

    /**
     * Returns the URL used by the connection
     *
     * @return The URL of the connection
     */
    String getUrl();

    /**
     * Returns the credentials used by the connection
     *
     * @return credentials
     */
    Credentials getCredentials();

    /**
     * Sets the credentials the connection should use
     *
     * @param credentials Credentials
     */
    void setCredentials(Credentials credentials);

    /**
     * Sets the message id the connection should use
     *
     * @param messageId message ID of the connection
     */
    void setMessageId(String messageId);

    /**
     * Sets whether the connection should be re-attempted
     * on error or we simply disconnect and notify user
     *
     * @param reconnectOnError should reconnect on error
     */
    void setReconnectOnError(boolean reconnectOnError);

    /**
     * Sets the groups token the connection should use
     *
     * @param groupsToken group token of the connection
     */
    void setGroupsToken(String groupsToken);

    /**
     * Sets the handler for the "Reconnecting" event
     *
     * @param handler reconnection handler
     */
    void reconnecting(Runnable handler);

    /**
     * Sets the handler for the "Reconnected" event
     *
     * @param handler reconnected handler
     */
    void reconnected(Runnable handler);

    /**
     * Sets the handler for the "Connected" event
     *
     * @param handler connected handler
     */
    void connected(Runnable handler);

    /**
     * Sets the handler for the "Error" event
     *
     * @param handler error handler
     */
    void error(ErrorCallback handler);

    /**
     * Sets the handler for the "StateChanged" event
     *
     * @param handler state changed handler
     */
    void stateChanged(StateChangedCallback handler);

    /**
     * Triggers the Error event
     *
     * @param error                      The error that triggered the event
     * @param mustCleanCurrentConnection True if the connection must be cleaned
     */
    void onError(Throwable error, boolean mustCleanCurrentConnection);

    /**
     * Sets the handler for the "Received" event
     *
     * @param handler message received handler
     */
    void received(MessageReceivedHandler handler);

    void onReceived(JsonNode message) throws JsonProcessingException;

    /**
     * Sets the handler for the "ConnectionSlow" event
     *
     * @param handler connection slow handler
     */
    void connectionSlow(Runnable handler);

    /**
     * Sets the handler for the "Closed" event
     *
     * @param handler conneciton closed handler
     */
    void closed(Runnable handler);

    /**
     * Returns the connection token
     *
     * @return connection token
     */
    String getConnectionToken();

    /**
     * Returns the connection ID
     *
     * @return connection ID
     */
    String getConnectionId();

    /**
     * Returns the query string used by the connection
     *
     * @return query string
     */
    String getQueryString();

    /**
     * Returns the current message ID
     *
     * @return message ID
     */
    String getMessageId();

    /**
     * Returns the connection groups token
     *
     * @return groups token
     */
    String getGroupsToken();

    /**
     * Returns the data used by the connection
     *
     * @return connection data
     */
    String getConnectionData();

    /**
     * Returns the connection state
     *
     * @return connection state
     */
    ConnectionState getState();

    /**
     * Starts the connection
     *
     * @param transport Transport to be used by the connection
     * @return Future for the operation
     */
    SignalRFuture<Void> start(ClientTransport transport);

    /**
     * Aborts the connection and closes it
     */
    void stop();

    /**
     * Closes the connection
     */
    void disconnect();

    /**
     * Sends data using the connection
     *
     * @param data Data to send
     * @return Future for the operation
     */
    SignalRFuture<Void> send(String data);

    /**
     * Prepares a request that is going to be sent to the server
     *
     * @param request The request to prepare
     */
    void prepareRequest(Request request);

    /**
     * Returns the connection headers
     *
     * @return connection HTTP headers
     */
    Map<String, String> getHeaders();

    /**
     * Add a header
     *
     * @param headerName  name of the HTTP header
     * @param headerValue value of the HTTP header
     */
    void addHeader(String headerName, String headerValue);
}