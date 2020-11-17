package org.javawebstack.httpclient.interceptor;

import org.javawebstack.httpclient.HTTPRequest;

public interface BeforeRequestInterceptor {
    void doBefore(HTTPRequest request);
}
