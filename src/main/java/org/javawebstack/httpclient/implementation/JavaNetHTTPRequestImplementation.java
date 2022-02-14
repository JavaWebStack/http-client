package org.javawebstack.httpclient.implementation;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JavaNetHTTPRequestImplementation implements IHTTPRequestImplementation {

    private String method;
    private String url;
    private HttpURLConnection conn;
    private boolean sslVerification;
    private boolean followRedirects;
    private int timeout;
    private Map<String, String[]> requestHeaders;
    private byte[] requestBody;

    private int status;
    private String statusMessage;

    public void setMethod(String method) {
        this.method = method;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSslVerification(boolean sslVerification) {
        this.sslVerification = sslVerification;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setRequestHeaders(Map<String, String[]> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public void setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
    }

    public int getResponseStatus() {
        return status;
    }

    public String getResponseStatusMessage() {
        return statusMessage;
    }

    public Map<String, String[]> getResponseHeaders() {
        Map<String, String[]> responseHeaders = new HashMap<>();
        conn.getHeaderFields().forEach((k,v) -> {
            if(k != null && v != null)
                responseHeaders.put(k.toLowerCase(Locale.ROOT), v.toArray(new String[0]));
        });
        return responseHeaders;
    }

    public InputStream getResponseStream() {
        int status = getResponseStatus();
        try {
            if(status>299 || status<200){
                return conn.getErrorStream();
            }else{
                return conn.getInputStream();
            }
        } catch (IOException ignored) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    public int execute() {
        try{
            URL theUrl = new URL(url);
            conn = (HttpURLConnection) theUrl.openConnection();
            if(!sslVerification && conn instanceof HttpsURLConnection){
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

            conn.setReadTimeout(timeout);
            conn.setConnectTimeout(timeout);
            conn.setRequestMethod(method);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(followRedirects);

            for(String k : requestHeaders.keySet()){
                for(String v : requestHeaders.get(k))
                    conn.addRequestProperty(k, v);
            }

            if(requestBody != null){
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(requestBody);
                os.flush();
                os.close();
            }

            status = conn.getResponseCode();
            statusMessage = conn.getResponseMessage();

        }catch(Exception ignored) {}
        if(status == 0)
            status = -1;
        return status;
    }

    public void close() {

    }

}
