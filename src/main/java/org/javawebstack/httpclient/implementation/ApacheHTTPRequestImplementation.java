package org.javawebstack.httpclient.implementation;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ApacheHTTPRequestImplementation implements IHTTPRequestImplementation {

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

    public String getResponseStatusMessage() {
        return statusMessage;
    }

    public Map<String, String[]> getResponseHeaders() {
        return responseHeaders;
    }

    public InputStream getResponseStream() {
        if(responseEntity == null)
            return new ByteArrayInputStream(new byte[0]);
        try {
            return responseEntity.getContent();
        } catch (IOException ignored) {
            return null;
        }
    }

    public int execute() {
        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .setConnectionRequestTimeout(timeout, TimeUnit.MILLISECONDS)
                    .setRedirectsEnabled(followRedirects)
                    .build();
            HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                    .setDefaultRequestConfig(config);

            if(!sslVerification) {
                SSLContext context = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
                HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                                .setSslContext(context)
                                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .build()
                        )
                        .build();
                clientBuilder.setConnectionManager(cm);
            }

            HttpClient client = clientBuilder.build();

            ClassicRequestBuilder builder = ClassicRequestBuilder.create(method);
            builder.setUri(url);
            requestHeaders.forEach((k, values) -> {
                for(String v : values)
                    builder.addHeader(k ,v);
            });

            if(requestBody != null) {
                String contentType = requestHeaders.computeIfAbsent("content-type", n -> new String[]{ "text/plain" })[0];
                builder.setEntity(new ByteArrayEntity(requestBody, ContentType.create(contentType)));
            }

            ClassicHttpResponse response = client.execute(builder.build(), res -> res);
            responseEntity = response.getEntity();

            status = response.getCode();
            statusMessage = response.getReasonPhrase();
            Map<String, List<String>> resHeaders = new HashMap<>();
            for(Header h : response.getHeaders())
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
