package com.finflow;

import com.finflow.entity.SafeToSpendSnapshot;
import com.finflow.repository.PaymentWebhookEventRepository;
import com.finflow.repository.SafeToSpendSnapshotRepository;
import com.finflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(
        named = "FINFLOW_TEST_DB_URL",
        matches = ".+",
        disabledReason = "Database-backed tests require FINFLOW_TEST_DB_URL, FINFLOW_TEST_DB_USERNAME, and FINFLOW_TEST_DB_PASSWORD")
class PaymentSafetyIntegrationTests {

    private static final String PIN = "123456";
    private static final String PASSWORD = "Test@12345";

    @LocalServerPort
    private int port;

    @Value("${finflow.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentWebhookEventRepository webhookEventRepository;

    @Autowired
    private SafeToSpendSnapshotRepository snapshotRepository;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void walletFundingIsIdempotentAndRejectsKeyReuseWithDifferentAmount() throws Exception {
        String token = registerAndLogin("Wallet Idem");
        long accountId = createAccount(token);
        assertEquals(200, send("POST", "/api/accounts/" + accountId + "/deposit", token, Map.of("amount", "500.00"), null).statusCode());
        assertEquals(201, send("POST", "/api/wallet", token, null, null).statusCode());

        String key = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of("accountId", accountId, "amount", "120.00", "description", "Top up");
        HttpResponse<String> first = send("POST", "/api/wallet/fund", token, body, key);
        HttpResponse<String> replay = send("POST", "/api/wallet/fund", token, body, key);
        assertEquals(200, first.statusCode(), first.body());
        assertEquals(200, replay.statusCode(), replay.body());
        assertEquals(read(first).path("id").asLong(), read(replay).path("id").asLong());

        HttpResponse<String> conflict = send("POST", "/api/wallet/fund", token,
                Map.of("accountId", accountId, "amount", "121.00", "description", "Top up"), key);
        assertEquals(409, conflict.statusCode(), conflict.body());

        assertMoney("380.00", read(send("GET", "/api/accounts/" + accountId, token, null, null)).path("balance"));
    }

    @Test
    void concurrentTransfersNeverOverdrawTheSender() throws Exception {
        String senderToken = registerAndLogin("Concurrent Sender");
        String receiverToken = registerAndLogin("Concurrent Receiver");
        long senderAccountId = createAccount(senderToken);
        long receiverAccountId = createAccount(receiverToken);
        String receiverAccountNumber = read(send("GET", "/api/accounts/" + receiverAccountId, receiverToken, null, null))
                .path("accountNumber").asText();
        assertEquals(200, send("POST", "/api/accounts/" + senderAccountId + "/deposit", senderToken,
                Map.of("amount", "100.00"), null).statusCode());

        int attempts = 10;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int index = 0; index < attempts; index++) {
                Callable<Integer> transfer = () -> {
                    start.await();
                    return send("POST", "/api/accounts/" + senderAccountId + "/transfer", senderToken,
                            Map.of("receiverAccountNumber", receiverAccountNumber, "amount", "20.00",
                                    "category", "TRANSFER", "pin", PIN),
                            UUID.randomUUID().toString()).statusCode();
                };
                results.add(pool.submit(transfer));
            }
            start.countDown();
            int succeeded = 0;
            for (Future<Integer> result : results) {
                int status = result.get();
                if (status == 200) succeeded++;
                else assertTrue(status == 400 || status == 409 || status == 422, "unexpected status " + status);
            }
            assertEquals(5, succeeded);
        } finally {
            pool.shutdownNow();
        }

        assertMoney("0.00", read(send("GET", "/api/accounts/" + senderAccountId, senderToken, null, null)).path("balance"));
        assertMoney("100.00", read(send("GET", "/api/accounts/" + receiverAccountId, receiverToken, null, null)).path("balance"));
    }

    @Test
    void webhookIsPublicButSignedAndReplaySafe() throws Exception {
        String provider = "FINFLOW_SANDBOX_PROVIDER";
        String eventId = "evt-" + UUID.randomUUID();
        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> body = Map.of("eventId", eventId, "eventType", "payment.succeeded", "paymentId", "pay-1",
                "status", "SUCCESS", "amount", "125.50", "timestamp", timestamp);
        String signature = sign(provider + "|" + eventId + "|payment.succeeded|pay-1|SUCCESS|125.50|" + timestamp);

        HttpResponse<String> unsigned = webhook(provider, body, null);
        assertEquals(401, unsigned.statusCode());
        assertTrue(unsigned.body().isEmpty());
        assertEquals(401, webhook(provider, body, "sha256=" + "00".repeat(32)).statusCode());
        assertEquals(401, webhook("UNKNOWN_PROVIDER", body, signature).statusCode());

        HttpResponse<String> first = webhook(provider, body, signature);
        assertEquals(200, first.statusCode(), first.body());
        assertEquals("RECEIVED", read(first).path("status").asText());

        HttpResponse<String> replay = webhook(provider, body, signature);
        assertEquals(200, replay.statusCode());
        assertEquals("DUPLICATE", read(replay).path("status").asText());
        assertEquals(1, webhookEventRepository.findByProviderOrderByReceivedAtDesc(provider).stream()
                .filter(event -> eventId.equals(event.getEventId())).count());
    }

    @Test
    void reconciliationRequiresAdminRole() throws Exception {
        String path = "/api/payments/reconciliation?from=" + LocalDateTime.now().minusDays(1)
                + "&to=" + LocalDateTime.now();
        assertEquals(401, send("GET", path, null, null, null).statusCode());

        String email = UUID.randomUUID() + "@recon.test";
        String userToken = registerAndLogin("Recon User", email);
        assertEquals(403, send("GET", path, userToken, null, null).statusCode());

        var user = userRepository.findByEmail(email).orElseThrow();
        user.setRole("ADMIN");
        userRepository.save(user);
        String adminToken = login(email);
        HttpResponse<String> admin = send("GET", path, adminToken, null, null);
        assertEquals(200, admin.statusCode(), admin.body());
    }

    @Test
    void listEndpointsStayBackwardCompatibleAndSupportPaging() throws Exception {
        String token = registerAndLogin("Paging User");
        for (int index = 1; index <= 3; index++) {
            HttpResponse<String> goal = send("POST", "/api/goals", token, Map.of(
                    "name", "Goal " + index, "targetAmount", "1000.00", "currentAmount", "0.00", "priority", "IMPORTANT"), null);
            assertEquals(201, goal.statusCode(), goal.body());
        }

        JsonNode unpaged = read(send("GET", "/api/goals", token, null, null));
        assertTrue(unpaged.isArray());
        assertEquals(3, unpaged.size());

        JsonNode firstPage = read(send("GET", "/api/goals?page=0&size=2", token, null, null));
        assertEquals(2, firstPage.path("content").size());
        assertEquals(3, firstPage.path("totalElements").asLong());
        assertEquals(2, firstPage.path("totalPages").asInt());
        assertEquals("Goal 3", firstPage.path("content").get(0).path("name").asText());
        JsonNode secondPage = read(send("GET", "/api/goals?page=1&size=2", token, null, null));
        assertEquals(1, secondPage.path("content").size());
        assertEquals("Goal 1", secondPage.path("content").get(0).path("name").asText());

        HttpResponse<String> invalid = send("GET", "/api/goals?size=0", token, null, null);
        assertEquals(400, invalid.statusCode());
        assertEquals("INVALID_PAGE_REQUEST", read(invalid).path("error").asText());
        assertEquals(400, send("GET", "/api/bills?size=101", token, null, null).statusCode());

        for (String path : List.of("/api/payments/mine", "/api/payments/history", "/api/autopay/history",
                "/api/notifications", "/api/bills", "/api/goals", "/api/cards", "/api/bank-links")) {
            HttpResponse<String> plain = send("GET", path, token, null, null);
            assertEquals(200, plain.statusCode(), path + " " + plain.body());
            assertTrue(read(plain).isArray(), path);
            HttpResponse<String> paged = send("GET", path + "?size=5", token, null, null);
            assertEquals(200, paged.statusCode(), path + " " + paged.body());
            assertTrue(read(paged).path("content").isArray(), path);
            assertEquals(5, read(paged).path("size").asInt(), path);
        }
    }

    @Test
    void incomeLedgerAndSafeToSpendExplanationWorkEndToEnd() throws Exception {
        String email = UUID.randomUUID() + "@insight.test";
        String token = registerAndLogin("Insight User", email);
        String otherToken = registerAndLogin("Other User");
        long accountId = createAccount(token);
        assertEquals(200, send("POST", "/api/accounts/" + accountId + "/deposit", token, Map.of("amount", "1000.00"), null).statusCode());

        HttpResponse<String> firstExplain = send("GET", "/api/insights/safe-to-spend/changes", token, null, null);
        assertEquals(200, firstExplain.statusCode(), firstExplain.body());
        JsonNode baseline = read(send("GET", "/api/insights/safe-to-spend/changes", token, null, null));
        assertTrue(baseline.path("baselineAvailable").asBoolean());
        assertEquals(0, baseline.path("drivers").size());

        long userId = userRepository.findByEmail(email).orElseThrow().getId();
        snapshotRepository.save(new SafeToSpendSnapshot(userId, java.time.LocalDate.now().minusDays(1),
                new BigDecimal("400.00"), new BigDecimal("400.00"), "[]", LocalDateTime.now()));

        HttpResponse<String> income = send("POST", "/api/income-schedules", token, Map.of(
                "sourceName", "Salary", "amount", "2500.00", "frequency", "MONTHLY",
                "nextExpectedDate", java.time.LocalDate.now().plusDays(5).toString()), null);
        assertEquals(201, income.statusCode(), income.body());
        assertMoney("2500.00", read(income).path("expectedWithinHorizon"));
        long incomeId = read(income).path("id").asLong();
        assertEquals(404, send("PUT", "/api/income-schedules/" + incomeId, otherToken, Map.of(
                "sourceName", "Hijack", "amount", "1.00", "frequency", "ONE_TIME",
                "nextExpectedDate", java.time.LocalDate.now().plusDays(1).toString()), null).statusCode());
        assertEquals(400, send("POST", "/api/income-schedules", token, Map.of(
                "sourceName", "Bad", "amount", "-5", "frequency", "DAILY",
                "nextExpectedDate", java.time.LocalDate.now().toString()), null).statusCode());

        JsonNode safe = read(send("GET", "/api/insights/safe-to-spend", token, null, null));
        assertMoney("3500.00", safe.path("safeToSpend"));

        JsonNode explained = read(send("GET", "/api/insights/safe-to-spend/changes", token, null, null));
        assertTrue(explained.path("baselineAvailable").asBoolean());
        assertEquals(java.time.LocalDate.now().minusDays(1).toString(), explained.path("baselineDate").asText());
        assertMoney("3100.00", explained.path("change"));
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode driver : explained.path("drivers")) sum = sum.add(driver.path("impact").decimalValue());
        assertEquals(0, new BigDecimal("3100.00").compareTo(sum));
        assertEquals("Salary", explained.path("drivers").get(0).path("description").asText());

        HttpResponse<String> goal = send("POST", "/api/goals", token, Map.of(
                "name", "Laptop", "targetAmount", "1000.00", "currentAmount", "100.00", "priority", "IMPORTANT"), null);
        assertEquals(201, goal.statusCode(), goal.body());
        long goalId = read(goal).path("id").asLong();
        assertEquals(200, send("POST", "/api/goals/" + goalId + "/contributions", token, Map.of("amount", "50.00"), null).statusCode());
        JsonNode ledger = read(send("GET", "/api/goals/" + goalId + "/contributions", token, null, null));
        assertEquals(2, ledger.size());
        assertEquals("MANUAL", ledger.get(0).path("source").asText());
        assertMoney("150.00", ledger.get(0).path("goalAmountAfter"));
        assertEquals("INITIAL", ledger.get(1).path("source").asText());
        assertEquals(1, read(send("GET", "/api/goals/" + goalId + "/contributions?size=1", token, null, null)).path("content").size());
        assertEquals(404, send("GET", "/api/goals/" + goalId + "/contributions", otherToken, null, null).statusCode());

        assertEquals(204, send("DELETE", "/api/income-schedules/" + incomeId, token, null, null).statusCode());
        assertEquals(204, send("DELETE", "/api/goals/" + goalId, token, null, null).statusCode());
    }

    private HttpResponse<String> webhook(String provider, Map<String, Object> body, String signature) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri("/api/payments/webhook/" + provider))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (signature != null) request.header("X-FinFlow-Signature", signature);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String sign(String canonical) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private String registerAndLogin(String name) throws Exception {
        return registerAndLogin(name, UUID.randomUUID() + "@safety.test");
    }

    private String registerAndLogin(String name, String email) throws Exception {
        HttpResponse<String> register = send("POST", "/api/auth/register", null,
                Map.of("name", name, "email", email, "password", PASSWORD, "phone", validPhone()), null);
        assertEquals(201, register.statusCode(), register.body());
        return login(email);
    }

    private String login(String email) throws Exception {
        HttpResponse<String> login = send("POST", "/api/auth/login", null, Map.of("email", email, "password", PASSWORD), null);
        assertEquals(200, login.statusCode(), login.body());
        return read(login).path("token").asText();
    }

    private long createAccount(String token) throws Exception {
        long userId = read(send("GET", "/api/users/me", token, null, null)).path("id").asLong();
        HttpResponse<String> created = send("POST", "/api/accounts/user/" + userId, token, Map.of(
                "accountType", "SAVINGS", "sandboxIdentityConfirmation", "CONFIRM",
                "transactionPin", PIN, "confirmTransactionPin", PIN), null);
        assertEquals(201, created.statusCode(), created.body());
        return read(created).path("id").asLong();
    }

    private HttpResponse<String> send(String method, String path, String token, Object body, String idempotencyKey) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (body != null) request.header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private JsonNode read(HttpResponse<String> response) {
        return json.readTree(response.body());
    }

    private void assertMoney(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()), "expected " + expected + " but was " + actual);
    }

    private String validPhone() {
        return String.format("%010d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
    }
}
