package org.javawebstack.httpclient.interfaces;

import org.javawebstack.httpclient.HTTPRequest;

public interface BeforeRequest {
    void doBefore(HTTPRequest request);
}
