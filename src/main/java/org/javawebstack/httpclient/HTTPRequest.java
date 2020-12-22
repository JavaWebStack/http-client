package org.javawebstack.httpclient;

import org.javawebstack.graph.GraphElement;
import org.javawebstack.httpclient.interceptor.ResponseTransformer;
import org.javawebstack.querystring.QueryString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {

    private final HTTPClient client;
    private final String path;
    private final String method;
    private final QueryString query = new QueryString();
    private final Map<String, String> requestHeaders = new HashMap<>();
    private byte[] requestBody;
    private final Map<String, String> responseHeaders = new HashMap<>();
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

    public HTTPRequest header(String key, String value){
        requestHeaders.put(key, value);
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

    public HTTPRequest basicAuthorization(String username, String password){
        return authorization("Basic", Base64.getEncoder().encodeToString((username+":"+password).getBytes()));
    }

    public HTTPRequest formBodyString(QueryString query) {
        return body(query.toString()).contentType("application/x-www-form-urlencoded");
    }

    public HTTPRequest formBody(Object object){
        if(object instanceof QueryString)
            return formBodyString((QueryString) object);
        if(object instanceof GraphElement)
            return formBodyElement((GraphElement) object);
        return formBodyElement(client.getGraphMapper().toGraph(object));
    }

    public HTTPRequest formBodyElement(GraphElement element){
        return body(element.toJsonString()).contentType("application/x-www-form-urlencoded");
    }

    public HTTPRequest bearer(String token){
        return authorization("Bearer", token);
    }

    public HTTPRequest jsonBody(Object object){
        if(object instanceof GraphElement)
            return jsonBodyElement((GraphElement) object);
        return jsonBodyElement(client.getGraphMapper().toGraph(object));
    }

    public HTTPRequest jsonBodyElement(GraphElement element){
        return body(element.toJsonString()).contentType("application/json");
    }

    public int status(){
        return status;
    }

    public byte[] bytes(){
        if (responseBody == null)
            execute();

        return responseBody;
    }

    public String string(){
        return new String(bytes(), StandardCharsets.UTF_8);
    }

    public <T> T object(Class<T> type){
        if(type == null)
            return null;
        if(type.equals(byte[].class))
            return (T) responseBody;
        if(type.equals(String.class))
            return (T) string();
        return client.getGraphMapper().fromGraph(graph(), type);
    }

    public GraphElement graph(){
        String contentType = header("Content-Type");
        if(contentType == null)
            contentType = "application/json";
        switch (contentType){
            case "application/x-www-form-urlencoded":
                GraphElement.fromFormData(string());
        }
        return GraphElement.fromJson(string());
    }

    public String header(String key){
        return responseHeaders.get(key);
    }

    public <T> T transform(ResponseTransformer responseTransformer, Class<T> type){
        return (T) responseTransformer.transform(this);
    }

    /*
    * Requires a transformer in the HttpClient
    * */
    public <T> T transform(Class<T> type){
        return (T) client.getResponseTransformer().transform(this);
    }

    public HTTPRequest execute(){
        HttpURLConnection conn = null;
        try{
            URL theUrl = new URL(client.getBaseUrl() + ((path.startsWith("/") || path.startsWith("http://") || path.startsWith("https://")) ? "" : "/") + path + (query.size() > 0 ? "?" + query.toString() : ""));
            conn = (HttpURLConnection) theUrl.openConnection();
            conn.setReadTimeout(client.getTimeout());
            conn.setConnectTimeout(5000);
            conn.setRequestMethod(method);
            conn.setDoInput(true);
            for(String k : requestHeaders.keySet()){
                conn.setRequestProperty(k, requestHeaders.get(k));
            }

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
                if(v.size()>0)
                    responseHeaders.put(k, v.get(v.size()-1));
            });
            if(status>299){
                this.responseBody = readAll(conn.getErrorStream());
            }else{
                this.responseBody = readAll(conn.getInputStream());
            }
            return this;
        }catch(Exception e){
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
