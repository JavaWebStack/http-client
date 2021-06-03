package org.javawebstack.httpclient.interceptor;

import org.javawebstack.httpclient.HTTPRequest;

public interface RequestInterceptor {
    void intercept(HTTPRequest request);
}
