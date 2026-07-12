package com.stock.crawler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stock.crawler.exception.ProviderCircuitOpenException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpUtilsCircuitBreakerTest {

    private MockWebServer server;
    private AtomicLong now;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        now = new AtomicLong(1_000_000L);
        HttpUtils.resetProviderStateForTest(now::get);
    }

    @AfterEach
    void tearDown() throws IOException {
        HttpUtils.resetProviderStateForTest(System::currentTimeMillis);
        server.shutdown();
    }

    @Test
    void provider_opensAfterThreeFailuresAndAllowsOneHalfOpenProbe() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
        String url = server.url("/provider").toString();
        CrawlerRequestPolicy oneAttempt = new CrawlerRequestPolicy(
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(3),
                1);

        for (int i = 0; i < 3; i++) {
            assertThrows(IOException.class, () -> HttpUtils.get(url, null, oneAttempt));
        }
        assertThrows(
                ProviderCircuitOpenException.class,
                () -> HttpUtils.get(url, null, oneAttempt));
        assertEquals(3, server.getRequestCount());

        now.addAndGet(Duration.ofSeconds(61).toMillis());

        assertEquals("ok", HttpUtils.get(url, null, oneAttempt));
        assertEquals(4, server.getRequestCount());
    }

    @Test
    void halfOpen_rejectsConcurrentProbeWhileFirstProbeIsInFlight() throws Exception {
        CountDownLatch probeStarted = new CountDownLatch(1);
        CountDownLatch releaseProbe = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (calls.incrementAndGet() <= 3) {
                    return new MockResponse().setResponseCode(503);
                }
                probeStarted.countDown();
                releaseProbe.await(5, TimeUnit.SECONDS);
                return new MockResponse().setResponseCode(200).setBody("ok");
            }
        });
        String url = server.url("/half-open").toString();
        CrawlerRequestPolicy oneAttempt = new CrawlerRequestPolicy(
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(3),
                1);
        for (int i = 0; i < 3; i++) {
            assertThrows(IOException.class, () -> HttpUtils.get(url, null, oneAttempt));
        }
        now.addAndGet(Duration.ofSeconds(61).toMillis());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> probe = executor.submit(() -> HttpUtils.get(url, null, oneAttempt));
            assertTrue(probeStarted.await(3, TimeUnit.SECONDS));

            assertThrows(
                    ProviderCircuitOpenException.class,
                    () -> HttpUtils.get(url, null, oneAttempt));

            releaseProbe.countDown();
            assertEquals("ok", probe.get(3, TimeUnit.SECONDS));
            assertEquals(4, server.getRequestCount());
        } finally {
            releaseProbe.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void provider_opensAfterThreeNetworkFailures() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        String url = server.url("/disconnect").toString();
        CrawlerRequestPolicy oneAttempt = new CrawlerRequestPolicy(
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(3),
                1);

        for (int i = 0; i < 3; i++) {
            assertThrows(IOException.class, () -> HttpUtils.get(url, null, oneAttempt));
        }

        assertThrows(
                ProviderCircuitOpenException.class,
                () -> HttpUtils.get(url, null, oneAttempt));
    }
}
