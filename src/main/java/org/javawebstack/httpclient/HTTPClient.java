package org.javawebstack.httpclient;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.javawebstack.httpclient.interfaces.BeforeRequest;
import org.javawebstack.httpclient.interfaces.ResponseTransformer;

import java.util.HashMap;
import java.util.Map;

public class HTTPClient {

    private Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .disableHtmlEscaping()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    private int timeout = 5000;
    private String baseUrl = "";
    private Map<String, String> defaultHeaders = new HashMap<>();
    private ResponseTransformer transformer;

    private BeforeRequest beforeInterceptor;

    public HTTPClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public HTTPClient() { }
    private Map<String, String> defaultQuery = new HashMap<>();

    public HTTPClient gson(Gson gson){
        this.gson = gson;
        return this;
    }

    public Gson getGson() {
        return gson;
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
        transformer = responseTransformer;
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

    public HTTPClient before(BeforeRequest requestInterceptor){
        beforeInterceptor = requestInterceptor;
        return this;
    }

    public BeforeRequest getBeforeInterceptor() {
        return beforeInterceptor;
    }

    public HTTPRequest post(String path){
        return request("POST", path);
    }

    public HTTPRequest post(String path, Object body){
        return post(path).jsonBody(body);
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

    public ResponseTransformer getTransformer() {
        return transformer;
    }
}
