package com.smartdining.bugs;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mot "nguoi dung" goi HTTP vao ung dung, giu cookie rieng (moi vai tro mot client).
 * Khong theo redirect de test doc duoc ma 302 cua trang dang nhap.
 */
final class ApiClient {
    static final ObjectMapper JSON = new ObjectMapper();

    private final String baseUrl;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http;

    record Response(int status, String body) {
        JsonNode json() {
            try {
                return JSON.readTree(body);
            } catch (IOException ex) {
                throw new AssertionError("Phan hoi khong phai JSON (HTTP " + status + "): " + body, ex);
            }
        }

        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Dang nhap qua form /login/{area}; tra ve ma HTTP (302 khi thanh cong). */
    Response login(String area, String username, String password) {
        String form = "username=" + encode(username) + "&password=" + encode(password);
        return send(HttpRequest.newBuilder(uri(area == null ? "/login" : "/login/" + area))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)));
    }

    Response get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET());
    }

    Response postJson(String path, Object body) {
        return sendJson("POST", path, body);
    }

    Response putJson(String path, Object body) {
        return sendJson("PUT", path, body);
    }

    /** Mo WebSocket voi cookie dang nhap cua client nay. */
    WebSocket openWebSocket(String path, WebSocket.Listener listener) {
        String cookieHeader = cookies.getCookieStore().getCookies().stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
        WebSocket.Builder builder = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10));
        if (!cookieHeader.isEmpty()) {
            builder.header("Cookie", cookieHeader);
        }
        return builder.buildAsync(URI.create(baseUrl.replaceFirst("^http", "ws") + path), listener).join();
    }

    private Response sendJson(String method, String path, Object body) {
        try {
            String payload = body instanceof String text ? text : JSON.writeValueAsString(body);
            return send(HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(payload)));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Response send(HttpRequest.Builder builder) {
        try {
            HttpResponse<String> response = http.send(builder.timeout(Duration.ofSeconds(30)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(response.statusCode(), response.body());
        } catch (IOException ex) {
            throw new IllegalStateException("Khong goi duoc " + baseUrl + ". Tomcat da chay chua?", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private URI uri(String path) {
        return URI.create(baseUrl + (path.startsWith("/") ? path : "/" + path));
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static Map<String, Object> body(Object... keyValues) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
