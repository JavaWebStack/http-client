package org.javawebstack.httpclient.websocket;

import org.javawebstack.httpclient.HTTPClientSocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class WebSocket implements Runnable {

    private final HTTPClientSocket socket;
    private final WebSocketHandler handler;

    public WebSocket(HTTPClientSocket socket, WebSocketHandler handler) throws IOException {
        this.socket = socket;
        this.handler = handler;
        socket.setRequestHeader("connection", "Upgrade");
        socket.setRequestHeader("upgrade", "websocket");
        byte[] keyBytes = new byte[16];
        new SecureRandom().nextBytes(keyBytes);
        String key = new String(Base64.getEncoder().encode(keyBytes), StandardCharsets.US_ASCII);
        socket.setRequestHeader("sec-websocket-key", key);
        socket.setRequestHeader("sec-websocket-version", "13");
        if(socket.getResponseStatus() != 101)
            throw new IOException("Server didn't accept protocol change");
        handler.onOpen(this);
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public void run() {
        WebSocketFrame frame;
        while (!socket.isClosed()) {
            try {
                frame = WebSocketFrame.read(socket.getInputStream());
                switch (frame.getOpcode()) {
                    case WebSocketFrame.OP_CLOSE:
                        Integer code = null;
                        String reason = null;
                        byte[] payload = frame.getPayload();
                        if(payload.length >= 2) {
                            code = (payload[0] << 8) | payload[1];
                            if(payload.length > 2) {
                                byte[] reasonBytes = new byte[payload.length - 2];
                                System.arraycopy(payload, 2, reasonBytes, 0, reasonBytes.length);
                                reason = new String(reasonBytes, StandardCharsets.UTF_8);
                            }
                        }
                        handler.onClose(this, code, reason);
                        frame.setMaskKey().write(socket.getOutputStream());
                        socket.close();
                        break;
                    case WebSocketFrame.OP_PING:
                        if (frame.getPayload().length > 125) {
                            close(1002, "Protocol Error");
                        } else {
                            frame.setOpcode(WebSocketFrame.OP_PONG).setMaskKey().write(socket.getOutputStream());
                        }
                        break;
                    case WebSocketFrame.OP_BINARY:
                        handler.onMessage(this, frame.getPayload());
                        break;
                    case WebSocketFrame.OP_TEXT:
                        handler.onMessage(this, new String(frame.getPayload(), StandardCharsets.UTF_8));
                        break;
                }
            } catch (IOException e) {
                handler.onClose(this, null, e.getMessage());
                return;
            }
        }
    }

    public void send(byte[] message) throws IOException {
        new WebSocketFrame().setFin(true).setOpcode(WebSocketFrame.OP_BINARY).setPayload(message).setMaskKey().write(socket.getOutputStream());
    }

    public void send(String message) throws IOException {
        new WebSocketFrame().setFin(true).setOpcode(WebSocketFrame.OP_TEXT).setPayload(message.getBytes(StandardCharsets.UTF_8)).setMaskKey().write(socket.getOutputStream());
    }

    public void close(Integer code, String reason) {
        byte[] reasonBytes = reason == null ? null : reason.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[code == null ? 0 : (reason == null ? 2 : (reasonBytes.length + 2))];
        if(code != null) {
            payload[0] = (byte) (code >> 8);
            payload[1] = (byte) (code & 0xF);
            if(reasonBytes != null)
                System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.length);
        }
        try {
            new WebSocketFrame().setFin(true).setOpcode(WebSocketFrame.OP_CLOSE).setPayload(payload).setMaskKey().write(socket.getOutputStream());
            socket.close();
        } catch (IOException ignored) {}
    }

}
