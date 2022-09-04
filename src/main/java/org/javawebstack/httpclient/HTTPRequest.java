package org.javawebstack.httpclient;

import org.javawebstack.abstractdata.AbstractElement;
import org.javawebstack.abstractdata.util.QueryString;
import org.javawebstack.httpclient.implementation.IHTTPRequestImplementation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HTTPRequest {

    private final HTTPClient client;
    private final String path;
    private final String method;
    private final QueryString query = new QueryString();
    private final Map<String, String[]> requestHeaders = new HashMap<>();
    private byte[] requestBody;
    private Map<String, String[]> responseHeaders = new HashMap<>();
    private final List<HttpCookie> requestCookies = new ArrayList<>();
    private final List<HttpCookie> responseCookies = new ArrayList<>();
    private byte[] responseBody;
    private int status;
    private String statusMessage;
    private boolean executed;

    private boolean followRedirects;

    public HTTPRequest(HTTPClient client, String method, String path) {
        this.client = client;

        this.method = method;
        this.path = path;
        this.followRedirects = client.isFollowingRedirects();
        for(String key : client.getDefaultQuery().keySet())
            query(key, client.getDefaultQuery().get(key));
        for(String key : client.getDefaultHeaders().keySet())
            header(key, client.getDefaultHeaders().get(key));
    }

    public HTTPRequest cookie(HttpCookie cookie) {
        requestCookies.add(cookie);
        return this;
    }

    public List<HttpCookie> cookies() {
        return responseCookies;
    }

    public HttpCookie cookie(String name) {
        return responseCookies.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public HTTPRequest header(String key, String... values) {
        requestHeaders.put(key.toLowerCase(Locale.ROOT), values);
        return this;
    }

    public HTTPRequest query (Map<String, String> values) {
        values.forEach(this::query);
        return this;
    }

    public HTTPRequest query(String key, String value){
        query.set(key, value);
        return this;
    }

    public HTTPRequest query(String key1, String key2, String value) {
        return query(key1 + "[" + key2 + "]", value);
    }

    public HTTPRequest query(String key1, String key2, String key3, String value) {
        return query(key1 + "[" + key2 + "]" + "[" + key3 + "]", value);
    }

    public HTTPRequest body(byte[] body) {
        this.requestBody = body;
        return this;
    }

    public HTTPRequest body(String body) {
        return body(body.getBytes(StandardCharsets.UTF_8));
    }

    public HTTPRequest contentType(String contentType) {
        return header("Content-Type", contentType);
    }

    public HTTPRequest authorization(String type, String value) {
        return header("Authorization", type + " " + value);
    }

    public HTTPRequest basicAuth(String username, String password) {
        return authorization("Basic", Base64.getEncoder().encodeToString((username+":"+password).getBytes(StandardCharsets.UTF_8)));
    }

    public HTTPRequest formBodyString(QueryString query) {
        return formBodyString(query.toString());
    }

    public HTTPRequest formBodyString(String query) {
        return body(query).contentType("application/x-www-form-urlencoded");
    }

    public HTTPRequest formBody(Object object) {
        if(object instanceof QueryString)
            return formBodyString((QueryString) object);
        if(object instanceof AbstractElement)
            return formBodyElement((AbstractElement) object);
        return formBodyElement(client.getAbstractMapper().toAbstract(object));
    }

    public HTTPRequest formBodyElement(AbstractElement element) {
        return body(element.toFormDataString()).contentType("application/x-www-form-urlencoded");
    }

    public HTTPRequest bearer(String token) {
        return authorization("Bearer", token);
    }

    public HTTPRequest tokenAuth(String token) {
        return authorization("token", token);
    }

    public HTTPRequest jsonBody(Object object) {
        if(object instanceof AbstractElement)
            return jsonBodyElement((AbstractElement) object);
        return jsonBodyElement(client.getAbstractMapper().toAbstract(object));
    }

    public Map<String, String[]> headers() {
        return responseHeaders;
    }

    public HTTPRequest jsonBodyElement(AbstractElement element) {
        return body(element.toJsonString()).contentType("application/json");
    }

    public int status() {
        execute();
        return status;
    }

    public String statusMessage() {
        execute();
        return statusMessage;
    }

    public byte[] bytes() {
        execute();
        return responseBody;
    }

    public String string() {
        return new String(bytes(), StandardCharsets.UTF_8);
    }

    public String redirect() {
        return header("Location");
    }

    public <T> T object(Class<T> type) {
        if(type == null)
            return null;
        if(type.equals(byte[].class))
            return (T) responseBody;
        if(type.equals(String.class))
            return (T) string();
        return client.getAbstractMapper().fromAbstract(data(), type);
    }

    public AbstractElement data() {
        String contentType = header("Content-Type");
        if(contentType == null)
            contentType = "application/json";

        switch (contentType){
            case "application/x-www-form-urlencoded":
                return AbstractElement.fromFormData(string());
            case "text/yaml":
            case "text/x-yaml":
            case "application/yaml":
            case "application/x-yaml":
                return AbstractElement.fromYaml(string());
        }
        return AbstractElement.fromJson(string());
    }

    public String header(String key) {
        String[] values = headers(key);
        return values.length < 1 ? null : values[0];
    }

    public String[] headers(String key) {
        String[] values = responseHeaders.get(key.toLowerCase(Locale.ROOT));
        return values == null ? new String[0] : values;
    }

    public HTTPRequest execute() {
        if (executed)
            return this;
        executed = true;

        if (client.getBeforeInterceptor() != null)
            client.getBeforeInterceptor().intercept(this);

        List<String> reqCookies = new ArrayList<>();
        for(HttpCookie cookie : client.getDefaultCookies())
            reqCookies.add(cookie.getName()+"="+cookie.getValue());
        for(HttpCookie cookie : requestCookies)
            reqCookies.add(cookie.getName()+"="+cookie.getValue());

        if(reqCookies.size() > 0) {
            String[] headers = requestHeaders.get("cookie");
            if(headers == null) {
                requestHeaders.put("cookie", new String[]{ String.join("; ", reqCookies) });
            } else {
                String[] newHeaders = new String[headers.length + 1];
                System.arraycopy(headers, 0, newHeaders, 0, headers.length);
                newHeaders[newHeaders.length - 1] = String.join("; ", reqCookies);
                requestHeaders.put("cookie", newHeaders);
            }
        }

        IHTTPRequestImplementation requestImplementation = client.getHttpImplementation().get();
        requestImplementation.setUrl(buildUrl());
        requestImplementation.setMethod(method);
        requestImplementation.setTimeout(client.getTimeout());
        requestImplementation.setFollowRedirects(followRedirects);
        requestImplementation.setRequestHeaders(requestHeaders);
        requestImplementation.setSslVerification(client.isSSLVerification());
        requestImplementation.setRequestBody(requestBody);
        status = requestImplementation.execute();
        statusMessage = requestImplementation.getResponseStatusMessage();
        responseHeaders = requestImplementation.getResponseHeaders();
        try {
            responseBody = readAll(requestImplementation.getResponseStream());
            requestImplementation.close();
        } catch (IOException ignored) {}

        for(String value : headers("set-cookie"))
            responseCookies.addAll(HttpCookie.parse("set-cookie: "+value));
        for(String value : headers("set-cookie2"))
            responseCookies.addAll(HttpCookie.parse("set-cookie2: "+value));
        if(client.isAutoCookies())
            cookies().forEach(client::cookie);

        if (client.getAfterInterceptor() != null)
            client.getAfterInterceptor().intercept(this);
        return this;
    }

    private String buildUrl() {
        return client.getBaseUrl() + ((path.startsWith("/") || path.startsWith("http://") || path.startsWith("https://")) ? "" : "/") + path + (query.size() > 0 ? "?" + query.toString() : "");
    }

    private static byte[] readAll(InputStream is) throws IOException {
        if(is == null)
            return new byte[0];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int r = 0;
        while (r != -1){
            r = is.read(data);
            if(r != -1)
                baos.write(data, 0, r);
        }
        is.close();
        return baos.toByteArray();
    }

    public HTTPRequest setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public boolean isFollowingRedirects() {
        return followRedirects;
    }

    public String toString(){
        return string();
    }
}
