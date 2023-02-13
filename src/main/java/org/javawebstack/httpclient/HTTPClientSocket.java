package org.javawebstack.httpclient;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

public class HTTPClientSocket {

    private final Socket socket;
    private final InputStream inputStream;
    private InputStream internalInputStream;
    private final OutputStream outputStream;
    private final String requestPath;
    private String requestMethod = "GET";
    private final String host;
    private final Map<String, List<String>> requestHeaders = new HashMap<>();
    private int responseStatus;
    private String responseStatusMessage;
    private final Map<String, List<String>> responseHeaders = new HashMap<>();
    private boolean headersSent;
    private boolean headersReceived;

    public HTTPClientSocket(String url, boolean insecure) throws IOException {
        String[] urlSplit = url.split("/", 4);
        if(urlSplit.length < 3)
            throw new RuntimeException("Invalid HTTP or WebSocket URL: " + url);
        boolean ssl = urlSplit[0].equals("https:") || urlSplit[0].equals("wss:");
        this.host = urlSplit[2];
        String[] hostSplit = urlSplit[2].split(":");
        String host = hostSplit[0];
        int port = hostSplit.length > 1 ? Integer.parseInt(hostSplit[1]) : (ssl ? 443 : 80);
        requestPath = "/" + (urlSplit.length > 3 ? urlSplit[3] : "");
        if(ssl) {
            SSLSocketFactory factory;
            if(insecure) {
                TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };
                try {
                    SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    factory = sc.getSocketFactory();
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    throw new IOException(e);
                }
            } else {
                factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            }
            socket = factory.createSocket(host, port);
            ((SSLSocket) socket).startHandshake();
        } else {
            socket = new Socket(host, port);
        }
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
    }

    public InputStream getInputStream() {
        return new HTTPInputStream();
    }

    public OutputStream getOutputStream() {
        return new HTTPOutputStream();
    }

    public int getResponseStatus() throws IOException {
        if(!headersReceived)
            readHeaders();
        return responseStatus;
    }

    public String getResponseStatusMessage() throws IOException {
        if(!headersReceived)
            readHeaders();
        return responseStatusMessage;
    }

    public HTTPClientSocket setRequestMethod(String method) {
        this.requestMethod = method;
        return this;
    }

    public Set<String> getResponseHeaderNames() throws IOException {
        if(!headersReceived)
            readHeaders();
        return responseHeaders.keySet();
    }

    public String getResponseHeader(String name) throws IOException {
        if(!headersReceived)
            readHeaders();
        List<String> values = responseHeaders.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.size() == 0 ? null : values.get(0);
    }

    public List<String> getResponseHeaders(String name) throws IOException {
        if(!headersReceived)
            readHeaders();
        return responseHeaders.getOrDefault(name.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    public HTTPClientSocket addRequestHeader(String name, String value) {
        this.requestHeaders.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(value);
        return this;
    }

    public HTTPClientSocket setRequestHeader(String name, String value) {
        this.requestHeaders.put(name.toLowerCase(Locale.ROOT), Arrays.asList(value));
        return this;
    }

    private void writeHeaders() throws IOException {
        if(headersSent)
            return;
        headersSent = true;
        StringBuilder sb = new StringBuilder(requestMethod.toUpperCase(Locale.ROOT))
                .append(" ")
                .append(requestPath)
                .append(" HTTP/1.1\nHost: ")
                .append(host);
        sb.append("\r\n");
        requestHeaders.forEach((k, values) -> values.forEach(v -> sb.append(k).append(": ").append(v).append("\r\n")));
        sb.append("\r\n");
        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void readHeaders() throws IOException {
        if(headersReceived)
            return;
        if(!headersSent)
            writeHeaders();
        headersReceived = true;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int lb = -1;
        while (true) {
            int b = inputStream.read();
            if(b == -1) {
                socket.close();
                throw new IOException("Unexpected end of stream");
            }
            if(b == '\r' && lb == '\n') {
                b = inputStream.read();
                break;
            }
            baos.write(b);
            lb = b;
        }
        String[] lines = new String(baos.toByteArray(), StandardCharsets.UTF_8).split("\\r?\\n");
        if(lines.length < 2) {
            socket.close();
            throw new IOException("Invalid http response");
        }
        String[] first = lines[0].split(" ", 3);
        if(first.length != 3 || !first[0].startsWith("HTTP/")) {
            socket.close();
            throw new IOException("Invalid http response");
        }
        responseStatus = Integer.parseInt(first[1]);
        responseStatusMessage = first[2];
        for(int i=1; i<lines.length; i++) {
            if(lines[i].length() == 0)
                continue;
            String[] hspl = lines[i].split(": ", 2);
            if(hspl.length != 2)
                throw new IOException("Invalid http request");
            List<String> values = responseHeaders.computeIfAbsent(hspl[0].toLowerCase(Locale.ROOT), h -> new ArrayList<>());
            values.add(hspl[1]);
        }
        if(getResponseHeader("transfer-encoding") != null) {
            switch (getResponseHeader("transfer-encoding")) {
                case "chunked":
                    internalInputStream = new ChunkedHTTPInputStream();
                    break;
                default:
                    internalInputStream = new StandardHTTPInputStream();
                    break;
            }
        } else if(getResponseHeader("upgrade") != null) {
            internalInputStream = inputStream;
        } else {
            internalInputStream = new StandardHTTPInputStream();
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    private class StandardHTTPInputStream extends InputStream {
        int len;
        int c = 0;
        StandardHTTPInputStream() throws IOException {
            String cl = getResponseHeader("content-length");
            len = cl == null ? 0 : Integer.parseInt(cl);
        }
        public int read() throws IOException {
            if(c >= len)
                return -1;
            c++;
            return inputStream.read();
        }
    }

    private class ChunkedHTTPInputStream extends InputStream {
        int remChunk = -1;
        boolean finished;
        public int read() throws IOException {
            if(finished)
                return -1;
            if(remChunk < 1) {
                if(remChunk == 0) {
                    inputStream.read();
                    inputStream.read();
                }
                int b;
                StringBuilder hex = new StringBuilder();
                while ((b = inputStream.read()) != '\r')
                    hex.append((char) b);
                remChunk = Integer.parseInt(hex.toString(), 16);
                if(remChunk == 0) {
                    finished = true;
                    return -1;
                }
                inputStream.read();
            }
            remChunk--;
            return inputStream.read();
        }
    }

    private class HTTPInputStream extends InputStream {
        public int available() throws IOException {
            if(!headersReceived)
                readHeaders();
            return internalInputStream.available();
        }
        public int read() throws IOException {
            if(!headersReceived)
                readHeaders();
            return internalInputStream.read();
        }
    }

    private class HTTPOutputStream extends OutputStream {
        public void flush() throws IOException {
            writeHeaders();
            super.flush();
        }
        public void write(int i) throws IOException {
            if(!headersSent)
                writeHeaders();
            outputStream.write(i);
        }
        public void close() throws IOException {
            close();
        }
    }

}
