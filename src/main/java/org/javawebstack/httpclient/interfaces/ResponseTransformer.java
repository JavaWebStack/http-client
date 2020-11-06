package org.javawebstack.httpclient.interfaces;

import org.javawebstack.httpclient.HTTPRequest;

public interface ResponseTransformer {
    Object transform(HTTPRequest request);
}
