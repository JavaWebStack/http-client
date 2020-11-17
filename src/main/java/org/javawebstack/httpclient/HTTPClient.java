package org.javawebstack.httpclient;

import org.javawebstack.graph.GraphElement;
import org.javawebstack.graph.GraphMapper;
import org.javawebstack.graph.NamingPolicy;
import org.javawebstack.httpclient.interceptor.BeforeRequestInterceptor;
import org.javawebstack.httpclient.interceptor.ResponseTransformer;

import java.util.HashMap;
import java.util.Map;

public class HTTPClient {

    private GraphMapper graphMapper = new GraphMapper()
            .setNamingPolicy(NamingPolicy.SNAKE_CASE);
    private int timeout = 5000;
    private String baseUrl = "";
    private Map<String, String> defaultHeaders = new HashMap<>();
    private Map<String, String> defaultQuery = new HashMap<>();

    private ResponseTransformer responseTransformer;
    private BeforeRequestInterceptor beforeInterceptor;

    public HTTPClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public HTTPClient() { }

    public HTTPClient graphMapper(GraphMapper mapper){
        this.graphMapper = mapper;
        return this;
    }

    public GraphMapper getGraphMapper(){
        return graphMapper;
    }

    public HTTPClient timeout(int timeout){
        this.timeout = timeout;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public HTTPClient header(String key, String value){
        defaultHeaders.put(key, value);
        return this;
    }

    public HTTPClient query(String key, String value){
        defaultQuery.put(key, value);
        return this;
    }

    public Map<String, String> getDefaultQuery() {
        return defaultQuery;
    }

    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    public HTTPClient setDefaultQuery(Map<String, String> defaultQuery) {
        this.defaultQuery = defaultQuery;
        return this;
    }

    public HTTPClient headers(Map<String, String> defaultHeaders){
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    public HTTPClient authorization(String type, String value){
        return header("Authorization", type + " " + value);
    }

    public HTTPClient bearer(String token){
        return authorization("Bearer", token);
    }

    public HTTPClient setBaseUrl(String baseUrl) {
        if(baseUrl.endsWith("/"))
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        this.baseUrl = baseUrl;
        return this;
    }

    public HTTPClient transformer(ResponseTransformer responseTransformer){
        responseTransformer = responseTransformer;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public HTTPRequest request(String method, String path){
        return new HTTPRequest(this, method, path);
    }

    public HTTPRequest get(String path){
        return request("GET", path);
    }

    public HTTPClient before(BeforeRequestInterceptor requestInterceptor){
        beforeInterceptor = requestInterceptor;
        return this;
    }

    public BeforeRequestInterceptor getBeforeInterceptor() {
        return beforeInterceptor;
    }

    public HTTPRequest post(String path){
        return request("POST", path);
    }

    public HTTPRequest post(String path, Object body){
        return post(path).jsonBody((GraphElement) body);
    }

    public HTTPRequest put(String path){
        return request("PUT", path);
    }

    public HTTPRequest put(String path, Object body){
        return put(path).jsonBody(body);
    }

    public HTTPRequest delete(String path){
        return request("DELETE", path);
    }

    public ResponseTransformer getResponseTransformer() {
        return responseTransformer;
    }
}
