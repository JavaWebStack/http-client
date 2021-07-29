package org.javawebstack.httpclient;

import org.javawebstack.abstractdata.AbstractElement;
import org.javawebstack.abstractdata.util.QueryString;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;

public class HTTPRequest {

    private final HTTPClient client;
    private final String path;
    private final String method;
    private final QueryString query = new QueryString();
    private final Map<String, String[]> requestHeaders = new HashMap<>();
    private byte[] requestBody;
    private final Map<String, String[]> responseHeaders = new HashMap<>();
    private final List<HttpCookie> requestCookies = new ArrayList<>();
    private final List<HttpCookie> responseCookies = new ArrayList<>();
    private byte[] responseBody;
    private int status;

    public HTTPRequest(HTTPClient client, String method, String path){
        this.client = client;
        this.method = method;
        this.path = path;
        for(String key : client.getDefaultQuery().keySet())
            query(key, client.getDefaultQuery().get(key));
        for(String key : client.getDefaultHeaders().keySet())
            header(key, client.getDefaultHeaders().get(key));
    }

    public void cookie(HttpCookie cookie){
        requestCookies.add(cookie);
    }

    public List<HttpCookie> cookies(){
        return responseCookies;
    }

    public HttpCookie cookie(String name) {
        return responseCookies.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public HTTPRequest header(String key, String... values){
        requestHeaders.put(key, values);
        return this;
    }

    public HTTPRequest query(String key, String value){
        query.set(key, value);
        return this;
    }

    public HTTPRequest query(String key1, String key2, String value){
        return query(key1 + "[" + key2 + "]", value);
    }

    public HTTPRequest query(String key1, String key2, String key3, String value){
        return query(key1 + "[" + key2 + "]" + "[" + key3 + "]", value);
    }

    public HTTPRequest body(byte[] body){
        this.requestBody = body;
        return this;
    }

    public HTTPRequest body(String body){
        return body(body.getBytes(StandardCharsets.UTF_8));
    }

    public HTTPRequest contentType(String contentType){
        return header("Content-Type", contentType);
    }

    public HTTPRequest authorization(String type, String value){
        return header("Authorization", type + " " + value);
    }

    public HTTPRequest basicAuth(String username, String password) {
        return authorization("Basic", Base64.getEncoder().encodeToString((username+":"+password).getBytes(StandardCharsets.UTF_8)));
    }

    public HTTPRequest formBodyString(QueryString query) {
        return formBodyString(query.toString());
    }

    public HTTPRequest formBodyString(String query) {
        return body(query).contentType("application/x-www-form-urlencoded");
    }

    public HTTPRequest formBody(Object object) {
        if(object instanceof QueryString)
            return formBodyString((QueryString) object);
        if(object instanceof AbstractElement)
            return formBodyElement((AbstractElement) object);
        return formBodyElement(client.getAbstractMapper().toAbstract(object));
    }

    public HTTPRequest formBodyElement(AbstractElement element) {
        return body(element.toJsonString()).contentType("application/x-www-form-urlencoded");
    }

    public HTTPRequest bearer(String token){
        return authorization("Bearer", token);
    }

    public HTTPRequest jsonBody(Object object) {
        if(object instanceof AbstractElement)
            return jsonBodyElement((AbstractElement) object);
        return jsonBodyElement(client.getAbstractMapper().toAbstract(object));
    }

    public Map<String, String[]> headers() {
        return responseHeaders;
    }

    public HTTPRequest jsonBodyElement(AbstractElement element) {
        return body(element.toJsonString()).contentType("application/json");
    }

    public int status() {
        execute();
        return status;
    }

    public byte[] bytes() {
        execute();
        return responseBody;
    }

    public String string(){
        return new String(bytes(), StandardCharsets.UTF_8);
    }

    public <T> T object(Class<T> type) {
        if(type == null)
            return null;
        if(type.equals(byte[].class))
            return (T) responseBody;
        if(type.equals(String.class))
            return (T) string();
        return client.getAbstractMapper().fromAbstract(data(), type);
    }

    public AbstractElement data() {
        String contentType = header("Content-Type");
        if(contentType == null)
            contentType = "application/json";
        switch (contentType){
            case "application/x-www-form-urlencoded":
                AbstractElement.fromFormData(string());
        }
        return AbstractElement.fromJson(string());
    }

    public String header(String key) {
        String[] values = headers(key);
        return values.length < 1 ? null : values[0];
    }

    public String[] headers(String key) {
        String[] values = responseHeaders.get(key);
        return values == null ? new String[0] : values;
    }

    public HTTPRequest execute() {
        if (responseBody != null)
            return this;
        HttpURLConnection conn = null;
        try{
            URL theUrl = new URL(client.getBaseUrl() + ((path.startsWith("/") || path.startsWith("http://") || path.startsWith("https://")) ? "" : "/") + path + (query.size() > 0 ? "?" + query.toString() : ""));
            conn = (HttpURLConnection) theUrl.openConnection();
            if(!client.isSSLVerification() && conn instanceof HttpsURLConnection){
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                httpsConn.setSSLSocketFactory(sc.getSocketFactory());
                httpsConn.setHostnameVerifier((hostname, session) -> true);
            }
            conn.setReadTimeout(client.getTimeout());
            conn.setConnectTimeout(5000);
            conn.setRequestMethod(method);
            conn.setDoInput(true);
            for(String k : requestHeaders.keySet()){
                for(String v : requestHeaders.get(k))
                    conn.addRequestProperty(k, v);
            }

            List<String> reqCookies = new ArrayList<>();
            for(HttpCookie cookie : client.getDefaultCookies())
                reqCookies.add(cookie.getName()+"="+cookie.getValue());
            for(HttpCookie cookie : requestCookies)
                reqCookies.add(cookie.getName()+"="+cookie.getValue());

            if(reqCookies.size() > 0)
                conn.addRequestProperty("Cookie", String.join("; ", reqCookies));

            if (client.getBeforeInterceptor() != null)
                client.getBeforeInterceptor().doBefore(this);


            if(requestBody!=null){
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(requestBody);
                os.flush();
                os.close();
            }

            status = conn.getResponseCode();

            conn.getHeaderFields().forEach((k,v) -> {
                if(k != null && v != null)
                    responseHeaders.put(k.toLowerCase(Locale.ROOT), v.toArray(new String[0]));
            });

            for(String value : headers("set-cookie"))
                responseCookies.addAll(HttpCookie.parse("set-cookie: "+value));
            for(String value : headers("set-cookie2"))
                responseCookies.addAll(HttpCookie.parse("set-cookie2: "+value));
            if(client.isAutoCookies())
                cookies().forEach(client::cookie);

            if(status>299 || status<200){
                this.responseBody = readAll(conn.getErrorStream());
            }else{
                this.responseBody = readAll(conn.getInputStream());
            }
            return this;
        }catch(Exception e){
            try {
                status = conn.getResponseCode();
            } catch (IOException ioException) {}
            if(client.isDebug())
                e.printStackTrace();
            try {
                this.responseBody = readAll(conn.getErrorStream());
                return this;
            }catch(IOException | NullPointerException ex){
                if(client.isDebug())
                    ex.printStackTrace();
            }
        }
        this.responseBody = new byte[0];
        return this;
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int r = 0;
        while (r != -1){
            r = is.read(data);
            if(r != -1)
                baos.write(data, 0, r);
        }
        is.close();
        return baos.toByteArray();
    }

    public String toString(){
        return string();
    }
}
