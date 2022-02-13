package org.javawebstack.httpclient.implementation;

import org.javawebstack.httpclient.HTTPClientSocket;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SimpleHTTPRequestImplementation implements IHTTPRequestImplementation {

    private String method;
    private String url;
    private boolean sslVerification;
    private boolean followRedirects;
    private int timeout;
    private Map<String, String[]> requestHeaders;
    private byte[] requestBody;

    private int status;
    private String statusMessage;
    private Map<String, String[]> responseHeaders = new HashMap<>();
    private HTTPClientSocket socket;

    public void setMethod(String method) {
        this.method = method;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSslVerification(boolean sslVerification) {
        this.sslVerification = sslVerification;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setRequestHeaders(Map<String, String[]> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public void setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
    }

    public int getResponseStatus() {
        return status;
    }

    public String getResponseStatusMessage() {
        return statusMessage;
    }

    public Map<String, String[]> getResponseHeaders() {
        return responseHeaders;
    }

    public InputStream getResponseStream() {
        return socket.getInputStream();
    }

    public int execute() {
        try {
            socket = new HTTPClientSocket(url, !sslVerification);
            socket.setRequestMethod(method);
            requestHeaders.forEach((k, values) -> {
                for(String v : values)
                    socket.addRequestHeader(k ,v);
            });
            if(requestBody != null) {
                socket.setRequestHeader("content-length", String.valueOf(requestBody.length));
                socket.getOutputStream().write(requestBody);
            }
            status = socket.getResponseStatus();
            statusMessage = socket.getResponseStatusMessage();
            for(String k : socket.getResponseHeaderNames())
                responseHeaders.put(k, socket.getResponseHeaders(k).toArray(new String[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(status == 0)
            status = -1;
        return status;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

}
