import org.javawebstack.httpclient.HTTPClient;

public class test {
    public static void main(String[] args) {
        HTTPClient client = new HTTPClient();
        System.out.println("Test: "+
                client.get("https://postman-echo.com/")
                        .string());
    }
}
