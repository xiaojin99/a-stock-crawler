package com.stock.crawler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.stock.crawler.exception.ProviderRateLimitException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpUtilsPolicyTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        HttpUtils.clearCooldownsForTest();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        HttpUtils.clearCooldownsForTest();
    }

    @Test
    void get_retriesServerErrorsOnlyUpToPolicyAttemptLimit() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/unstable", exchange -> {
            int requestNumber = requests.incrementAndGet();
            respond(exchange, requestNumber == 1 ? 503 : 200, requestNumber == 1 ? "busy" : "ok");
        });
        CrawlerRequestPolicy policy = new CrawlerRequestPolicy(
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(4),
                2);

        String result = HttpUtils.get(baseUrl + "/unstable", null, policy);

        assertEquals("ok", result);
        assertEquals(2, requests.get());
    }

    @Test
    void get_doesNotRetryOrdinaryClientErrors() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/not-found", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 404, "missing");
        });

        assertThrows(IOException.class, () -> HttpUtils.get(
                baseUrl + "/not-found",
                null,
                new CrawlerRequestPolicy(
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(4),
                        2)));

        assertEquals(1, requests.get());
    }

    @Test
    void get_honorsRetryAfterByCoolingDownHostWithoutRetrying429() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/limited", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Retry-After", "60");
            respond(exchange, 429, "slow down");
        });
        CrawlerRequestPolicy policy = new CrawlerRequestPolicy(
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(4),
                2);

        ProviderRateLimitException limited = assertThrows(
                ProviderRateLimitException.class,
                () -> HttpUtils.get(baseUrl + "/limited", null, policy));
        ProviderRateLimitException cooldown = assertThrows(
                ProviderRateLimitException.class,
                () -> HttpUtils.get(baseUrl + "/limited", null, policy));

        assertEquals(Duration.ofMinutes(1), limited.getRetryAfter());
        assertTrue(cooldown.getMessage().contains("cooldown"));
        assertEquals(1, requests.get());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
