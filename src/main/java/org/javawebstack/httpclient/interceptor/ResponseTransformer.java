package org.javawebstack.httpclient.interceptor;

import org.javawebstack.httpclient.HTTPRequest;

public interface ResponseTransformer {
    Object transform(HTTPRequest request);
}
