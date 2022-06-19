/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import tel.race.signalr.client.*;
import tel.race.signalr.client.http.HttpConnection;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Implements the WebsocketTransport for the Java SignalR library Created by
 * stas on 07/07/14.
 */
public class WebsocketTransport extends HttpClientTransport {

    private static final String HTTP_SCHEME = "http";
    private static final String SECURE_HTTP_SCHEME = "https";

    private static final String WEBSCOCKET_SCHEME = "ws";
    private static final String SECURE_WEBSOCKET_SCHEME = "wss";

    private static final String HTTP_URL_START = HTTP_SCHEME + "://";
    private static final String SECURE_HTTP_URL_START = SECURE_HTTP_SCHEME + "://";

    private static final String WEBSOCKET_URL_START = WEBSCOCKET_SCHEME + "://";
    private static final String SECURE_WEBSOCKET_URL_START = SECURE_WEBSOCKET_SCHEME + "://";

    private String prefix;
    WebSocketClient webSocketClient;
    private UpdateableCancellableFuture<Void> connectionFuture;

    public WebsocketTransport() {
        super();
    }

    public WebsocketTransport(HttpConnection httpConnection) {
        super(httpConnection);
    }

    @Override
    public String getName() {
        return "webSockets";
    }

    @Override
    public boolean supportKeepAlive() {
        return true;
    }

    @Override
    public SignalRFuture<Void> start(final ConnectionBase connection, ConnectionType connectionType,
                                     final DataResultCallback callback) {

        final String connectionUrl = connection.getUrl()
                .replace(SECURE_HTTP_URL_START, SECURE_WEBSOCKET_URL_START)
                .replace(HTTP_URL_START, WEBSOCKET_URL_START);
        LOGGER.debug("WebSocket URL: {}", connectionUrl);

        final String connectionString = connectionType == ConnectionType.INITIAL_CONNECTION ? "connect" : "reconnect";

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("connectionToken", connection.getConnectionToken());
        requestParams.put("connectionData", connection.getConnectionData());
        requestParams.put("clientProtocol", Connection.PROTOCOL_VERSION.toString());
//		requestParams.put("groupsToken", connection.getGroupsToken());
//		requestParams.put("messageId", connection.getMessageId());
        requestParams.put("transport", getName());
        LOGGER.trace("WebSocket request params: {}", requestParams);

        boolean isSsl = false;

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(connectionUrl);
        urlBuilder.append(connectionString);

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            if (entry.getValue() != null) {
                joiner.add(entry.getKey() + "=" + encodeValue(entry.getValue()));
            }
        }
        urlBuilder.append("?").append(joiner);

        String url = urlBuilder.toString();

        if (connection.getQueryString() != null) {
            url += "&" + connection.getQueryString();
        }

        LOGGER.trace("WebSocket Encoded URL: {}", url);

        if (url.startsWith(SECURE_WEBSOCKET_SCHEME)) {
            isSsl = true;
        }

        connectionFuture = new UpdateableCancellableFuture<>(null);

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            connectionFuture.triggerError(e);
            return connectionFuture;
        }

        webSocketClient = new WebSocketClient(uri, new Draft_6455(), connection.getHeaders(), 0) {

            Exception e;

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                connectionFuture.setResult(null);
            }

            @Override
            public void onMessage(String s) {
                callback.onData(s);
            }

            @Override
            public void onClose(int errorCode, String message, boolean remote) {
                webSocketClient.close();
                if (errorCode != CloseFrame.NORMAL || e != null) {
                    if (e == null) {
                        e = new IllegalStateException("Remote " + remote + ", " + errorCode + " - " + message);
                    }
                    connection.onError(e, true);
                } else {
                    connection.onError(new IllegalStateException("Remote " + remote + ", " + errorCode + " - " + message), true);
                }
            }

            @Override
            public void onError(Exception e) {
                webSocketClient.close();
                this.e = e;
            }
        };

        if (isSsl) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try {
                webSocketClient.setSocket(factory.createSocket());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        webSocketClient.connect();

        connection.closed(() -> webSocketClient.close());

        return connectionFuture;
    }

    private String encodeValue(String value) {

        String result = "";

        try {
            if (value != null)
                result = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return result;
    }

    private Socket createConnectedSocket(URI uri) throws IOException {
        String host = uri.getHost();
        boolean isSecureSocket = uri.toString().startsWith(SECURE_WEBSOCKET_URL_START);
        int port = isSecureSocket ? 443 : 80;

        boolean useProxy = Platform.useProxy();
        String proxyHost = Platform.getProxyHost();
        int proxyPort = Platform.getProxyPort();
        Proxy proxy = useProxy ? new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort))
                : Proxy.NO_PROXY;

        if (isSecureSocket) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            if (useProxy) {
                Socket underlyingSocket = new Socket(proxy);
                underlyingSocket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
                return factory.createSocket(underlyingSocket, proxyHost, proxyPort, true);
            } else {
                Socket socket = factory.createSocket();
                socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
                return socket;
            }
        } else {
            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            return socket;
        }
    }

    @Override
    public SignalRFuture<Void> send(ConnectionBase connection, String data, DataResultCallback callback) {
        webSocketClient.send(data);
        return new UpdateableCancellableFuture<>(null);
    }

    private boolean isJSONValid(String test) {
        try {
            Connection.MAPPER.readTree(test);
            return true;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }
}
