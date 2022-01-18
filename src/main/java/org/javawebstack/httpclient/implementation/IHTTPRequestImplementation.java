package org.javawebstack.httpclient.implementation;

import java.io.InputStream;
import java.util.Map;

public interface IHTTPRequestImplementation {

    void setMethod(String method);

    void setUrl(String url);

    void setSslVerification(boolean sslVerification);

    void setFollowRedirects(boolean followRedirects);

    void setTimeout(int timeout);

    void setRequestHeaders(Map<String, String[]> requestHeaders);

    void setRequestBody(byte[] requestBody);

    Map<String, String[]> getResponseHeaders();

    InputStream getResponseStream();

    int execute();

    void close();

}
