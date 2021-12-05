package org.javawebstack.httpclient.websocket;

public interface WebSocketHandler {

    void onOpen(WebSocket socket);

    void onMessage(WebSocket socket, String message);

    void onMessage(WebSocket socket, byte[] message);

    void onClose(WebSocket socket, Integer code, String reason);

}
