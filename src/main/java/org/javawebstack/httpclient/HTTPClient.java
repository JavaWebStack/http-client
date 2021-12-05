package org.javawebstack.httpclient;

import org.javawebstack.abstractdata.AbstractMapper;
import org.javawebstack.abstractdata.NamingPolicy;
import org.javawebstack.httpclient.interceptor.RequestInterceptor;
import org.javawebstack.httpclient.websocket.WebSocket;
import org.javawebstack.httpclient.websocket.WebSocketHandler;

import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HTTPClient {

    private AbstractMapper abstractMapper = new AbstractMapper()
            .setNamingPolicy(NamingPolicy.SNAKE_CASE);
    private int timeout = 5000;
    private String baseUrl = "";
    private Map<String, String[]> defaultHeaders = new HashMap<>();
    private Map<String, String> defaultQuery = new HashMap<>();
    private List<HttpCookie> defaultCookies = new ArrayList<>();

    private RequestInterceptor beforeInterceptor;
    private RequestInterceptor afterInterceptor;

    private boolean debug = false;
    private boolean sslVerification = true;
    private boolean autoCookies = false;

    private boolean followRedirects = false;

    private boolean legacyMode = false;

    public HTTPClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public HTTPClient() { }

    public HTTPClient debug(){
        this.debug = true;
        return this;
    }

    public boolean isDebug(){
        return debug;
    }

    public HTTPClient legacyMode() {
        this.legacyMode = true;
        return this;
    }

    public boolean isLegacyMode() {
        return legacyMode;
    }

    public HTTPClient autoCookies(){
        autoCookies = true;
        return this;
    }

    public boolean isAutoCookies(){
        return autoCookies;
    }

    public HTTPClient setSSLVerification(boolean sslVerification){
        this.sslVerification = sslVerification;
        return this;
    }

    public boolean isSSLVerification(){
        return this.sslVerification;
    }

    public HTTPClient abstractMapper(AbstractMapper mapper){
        this.abstractMapper = mapper;
        return this;
    }

    public AbstractMapper getAbstractMapper(){
        return abstractMapper;
    }

    public HTTPClient timeout(int timeout){
        this.timeout = timeout;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public HTTPClient header(String key, String... values){
        defaultHeaders.put(key, values);
        return this;
    }

    public HTTPClient query(String key, String value){
        defaultQuery.put(key, value);
        return this;
    }

    public HTTPClient cookie(HttpCookie cookie){
        removeCookie(cookie.getName());
        defaultCookies.add(cookie);
        return this;
    }

    public HTTPClient removeCookie(String name){
        for(HttpCookie cookie : new HashSet<>(defaultCookies)){
            if(cookie.getName().equalsIgnoreCase(name))
                defaultCookies.remove(cookie);
        }
        return this;
    }

    public List<HttpCookie> getDefaultCookies(){
        return defaultCookies;
    }

    public HTTPClient setDefaultCookies(List<HttpCookie> cookies){
        this.defaultCookies = cookies;
        return this;
    }

    public Map<String, String> getDefaultQuery() {
        return defaultQuery;
    }

    public Map<String, String[]> getDefaultHeaders() {
        return defaultHeaders;
    }

    public HTTPClient setDefaultQuery(Map<String, String> defaultQuery) {
        this.defaultQuery = defaultQuery;
        return this;
    }

    public HTTPClient setDefaultHeaders(Map<String, String[]> defaultHeaders){
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    public HTTPClient authorization(String type, String value){
        return header("Authorization", type + " " + value);
    }

    public HTTPClient bearer(String token){
        return authorization("Bearer", token);
    }

    public HTTPClient basicAuth(String username, String password) {
        return authorization("Basic", Base64.getEncoder().encodeToString((username+":"+password).getBytes(StandardCharsets.UTF_8)));
    }

    public HTTPClient setBaseUrl(String baseUrl) {
        if(baseUrl.endsWith("/"))
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        this.baseUrl = baseUrl;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public HTTPRequest request(String method, String path){
        return new HTTPRequest(this, method, path);
    }

    public WebSocket webSocket(String path, WebSocketHandler handler) throws IOException {
        return webSocket(path, handler, null);
    }

    public WebSocket webSocket(String path, WebSocketHandler handler, Map<String, String> additionalHeaders) throws IOException {
        HTTPClientSocket socket = new HTTPClientSocket(getBaseUrl() + ((path.startsWith("/") || path.startsWith("http://") || path.startsWith("https://")) ? "" : "/") + path, !isSSLVerification());
        if(additionalHeaders != null)
            additionalHeaders.forEach(socket::setRequestHeader);
        WebSocket webSocket = new WebSocket(socket, handler);
        new Thread(webSocket).start();
        return webSocket;
    }

    public HTTPRequest get(String path){
        return request("GET", path);
    }

    public HTTPClient before(RequestInterceptor requestInterceptor){
        beforeInterceptor = requestInterceptor;
        return this;
    }
    public HTTPClient after(RequestInterceptor requestInterceptor) {
        afterInterceptor = requestInterceptor;
        return this;
    }

    public RequestInterceptor getBeforeInterceptor() {
        return beforeInterceptor;
    }

    public RequestInterceptor getAfterInterceptor() {
        return afterInterceptor;
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

    public boolean isFollowingRedirects() {
        return followRedirects;
    }

    public HTTPClient setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

}
