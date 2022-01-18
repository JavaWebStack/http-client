package org.javawebstack.httpclient.implementation;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ApacheHTTPRequestImplementation implements IHTTPRequestImplementation {

    private String method;
    private String url;
    private boolean sslVerification;
    private boolean followRedirects;
    private int timeout;
    private Map<String, String[]> requestHeaders;
    private byte[] requestBody;

    private int status;
    private Map<String, String[]> responseHeaders = new HashMap<>();
    private HttpEntity responseEntity;

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

    public Map<String, String[]> getResponseHeaders() {
        return responseHeaders;
    }

    public InputStream getResponseStream() {
        try {
            return responseEntity.getContent();
        } catch (IOException ignored) {
            return null;
        }
    }

    public int execute() {
        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .setRedirectsEnabled(followRedirects)
                    .build();
            HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                    .setDefaultRequestConfig(config);

            if(!sslVerification) {
                clientBuilder
                        .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }

            HttpClient client = clientBuilder.build();

            RequestBuilder builder = RequestBuilder.create(method);
            builder.setUri(url);
            requestHeaders.forEach((k, values) -> {
                for(String v : values)
                    builder.addHeader(k ,v);
            });

            if(requestBody != null) {
                String contentType = requestHeaders.computeIfAbsent("content-type", n -> new String[]{ "text/plain" })[0];
                builder.setEntity(new ByteArrayEntity(requestBody, ContentType.create(contentType)));
            }

            HttpResponse response = client.execute(builder.build());

            status = response.getStatusLine().getStatusCode();
            Map<String, List<String>> resHeaders = new HashMap<>();
            for(Header h : response.getAllHeaders())
                resHeaders.computeIfAbsent(h.getName().toLowerCase(Locale.ROOT), n -> new ArrayList<>()).add(h.getValue());
            resHeaders.forEach((k, v) -> responseHeaders.put(k, v.toArray(new String[0])));

            responseEntity = response.getEntity();
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {

        }
        if(status == 0)
            status = -1;
        return status;
    }

    public void close() {

    }

}
