package org.javawebstack.httpclient.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

public class WebSocketFrame {

    public static final byte OP_CLOSE = 0x8;
    public static final byte OP_PING = 0x9;
    public static final byte OP_PONG = 0xA;
    public static final byte OP_TEXT = 0x1;
    public static final byte OP_BINARY = 0x2;

    private byte flags;
    private byte opcode;
    private byte[] maskKey;
    private byte[] payload;

    public boolean isFin() {
        return (flags & 0b1000_0000) > 0;
    }

    public WebSocketFrame setFin(boolean fin) {
        flags = (byte) ((flags & 0b0111_1111) | ((fin ? 1 : 0) << 7));
        return this;
    }

    public boolean isRsv1() {
        return (flags & 0b0100_0000) > 0;
    }

    public WebSocketFrame setRsv1(boolean rsv1) {
        flags = (byte) ((flags & 0b1011_1111) | ((rsv1 ? 1 : 0) << 6));
        return this;
    }

    public boolean isRsv2() {
        return (flags & 0b0010_0000) > 0;
    }

    public WebSocketFrame setRsv2(boolean rsv2) {
        flags = (byte) ((flags & 0b1101_1111) | ((rsv2 ? 1 : 0) << 5));
        return this;
    }

    public boolean isRsv3() {
        return (flags & 0b0001_0000) > 0;
    }

    public WebSocketFrame setRsv3(boolean rsv3) {
        flags = (byte) ((flags & 0b1110_1111) | ((rsv3 ? 1 : 0) << 4));
        return this;
    }

    public byte getOpcode() {
        return opcode;
    }

    public WebSocketFrame setOpcode(byte opcode) {
        this.opcode = opcode;
        return this;
    }

    public byte[] getMaskKey() {
        return maskKey;
    }

    public WebSocketFrame setMaskKey(byte[] maskKey) {
        this.maskKey = maskKey;
        return this;
    }

    public WebSocketFrame setMaskKey() {
        byte[] key = new byte[4];
        new SecureRandom().nextBytes(key);
        return setMaskKey(key);
    }

    public byte[] getPayload() {
        return payload;
    }

    public WebSocketFrame setPayload(byte[] payload) {
        this.payload = payload;
        return this;
    }

    public void write(OutputStream stream) throws IOException {
        stream.write(flags | opcode);
        int lengthByte = payload.length > 125 ? (payload.length > 0xFFFF ? 127 : 126) : payload.length;
        stream.write((maskKey != null ? 0b1000_0000 : 0) | lengthByte);
        if(lengthByte == 127) {
            stream.write(payload.length >> 24);
            stream.write((payload.length & 0xFF0000) >> 16);
        }
        if(lengthByte > 125) {
            stream.write((payload.length & 0xFF00) >> 8);
            stream.write(payload.length & 0xFF);
        }
        if(maskKey != null)
            stream.write(maskKey);
        if(maskKey != null) {
            for(int i=0; i<payload.length; i++)
                stream.write(payload[i] ^ maskKey[i % 4]);
        } else {
            stream.write(payload);
        }
    }

    public static WebSocketFrame read(InputStream stream) throws IOException {
        WebSocketFrame frame = new WebSocketFrame();
        int b = safeRead(stream);
        frame.flags = (byte) (b & 0xF0);
        frame.opcode = (byte) (b & 0x0F);
        b = safeRead(stream);
        frame.maskKey = (b >> 7) == 1 ? new byte[4] : null;
        int len = b & 0b0111_1111;
        if(len == 126) {
            len = safeRead(stream) << 8;
            len |= safeRead(stream);
        } else if(len == 127) {
            len = safeRead(stream) << 24;
            len |= safeRead(stream) << 16;
            len |= safeRead(stream) << 8;
            len |= safeRead(stream);
        }
        if(frame.maskKey != null) {
            frame.maskKey[0] = (byte) safeRead(stream);
            frame.maskKey[1] = (byte) safeRead(stream);
            frame.maskKey[2] = (byte) safeRead(stream);
            frame.maskKey[3] = (byte) safeRead(stream);
        }
        frame.payload = new byte[len];
        if(frame.maskKey != null) {
            for(int i=0; i<len; i++)
                frame.payload[i] = (byte) (safeRead(stream) ^ frame.maskKey[i % 4]);
        } else {
            for(int i=0; i<len; i++)
                frame.payload[i] = (byte) safeRead(stream);
        }
        return frame;
    }

    private static int safeRead(InputStream stream) throws IOException {
        int b = stream.read();
        if(b == -1)
            throw new IOException("Unexpected end of stream");
        return b;
    }

}
