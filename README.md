<p align="center"><img src="https://raw.githubusercontent.com/JavaWebStack/docs/master/src/assets/img/icon.svg" width="100">
<br><br>
JavaWebStack HTTP-Client
</p>



## A little tour
Full documentations: [HTTP-Client JWS-Docs](https://javawebstack.org/docs/http-client)

```java
HTTPClient client = new HTTPClient();

// Sending a simple GET-Request and printing the response-text
System.out.println(client.get("https://example.javawebstack.org/api/test").string());

// Setting a base-url
client.setBaseUrl("https://example.javawebstack.org"); // or new HTTPClient(baseUrl)

// Sending a POST-Request with a JSON-Body
Map<String, Object> data = new HashMap<>();
data.put("username", "password");

HTTPResponse res = client.post("/api/post")
                        .jsonBody(data).json(User.class);
```