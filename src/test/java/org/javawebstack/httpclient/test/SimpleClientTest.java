package org.javawebstack.httpclient.test;

import org.javawebstack.httpclient.HTTPClient;
import org.javawebstack.httpclient.HTTPRequest;
import org.junit.jupiter.api.Test;

import java.net.HttpCookie;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleClientTest {

    @Test
    public void testSimpleGet() {
        HTTPClient client = new HTTPClient();
        client.debug();
        client.cookie(new HttpCookie("x-test-global", "Testing"));
        client.header("X-Test-Global", "Testing");
        client.bearer("abcdefg");
        HTTPRequest request = client.get("https://jsonplaceholder.typicode.com/todos/1");
        request.cookie(new HttpCookie("x-test-request", "Testing"));
        request.header("X-Test-Request", "Testing");
        request.bearer("abcdefg");
        request.contentType("text/plain");
        request.execute();
        assertEquals(200, request.status());
        assertEquals("{\n" +
                "  \"userId\": 1,\n" +
                "  \"id\": 1,\n" +
                "  \"title\": \"delectus aut autem\",\n" +
                "  \"completed\": false\n" +
                "}", request.string());
    }

}
