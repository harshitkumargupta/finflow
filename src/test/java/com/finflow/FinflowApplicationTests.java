package com.finflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(
        named = "FINFLOW_TEST_DB_URL",
        matches = ".+",
        disabledReason = "Database-backed tests require FINFLOW_TEST_DB_URL, FINFLOW_TEST_DB_USERNAME, and FINFLOW_TEST_DB_PASSWORD")
class FinflowApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Transactional
    void createAccountRejectsAnotherUsersPathId() throws Exception {
        String firstToken = registerAndLogin(UUID.randomUUID() + "@ownership.test", "Test@123", "Owner One");
        String secondToken = registerAndLogin(UUID.randomUUID() + "@ownership.test", "Test@123", "Owner Two");
        long secondUserId = currentUserId(secondToken);
        mockMvc.perform(post("/api/accounts/user/" + secondUserId)
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountCreationPayload("SAVINGS")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void authEndpointsArePublicAndOtherEndpointsRequireJwt() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/999999999"))
                .andExpect(status().isUnauthorized());

        String email = UUID.randomUUID() + "@trace.test";
        String password = "Test@123";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "FinFlow Test",
                                "email", email,
                                "password", password,
                                "phone", validPhone()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Duplicate Email",
                                "email", email,
                                "password", password,
                                "phone", validPhone()))))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Email already registered")));

        String phone = validPhone();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Phone Test",
                                "email", UUID.randomUUID() + "@trace.test",
                                "password", password,
                                "phone", phone))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Duplicate Phone",
                                "email", UUID.randomUUID() + "@trace.test",
                                "password", password,
                                "phone", phone))))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Phone already registered")));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(loginResponse)
                .path("token")
                .asText();

        mockMvc.perform(get("/api/accounts/999999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void oneFinFlowAccountPerUserAndWithdrawalsCanBeAutoCategorized() throws Exception {
        String password = "Test@123";
        String token = registerAndLogin(UUID.randomUUID() + "@single-account.test", password, "Single Account User");
        String authorization = "Bearer " + token;
        long userId = currentUserId(token);

        long accountId = objectMapper.readTree(mockMvc.perform(post("/api/accounts/user/" + userId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountCreationPayload("SAVINGS")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primary").value(true))
                .andReturn().getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/accounts/user/" + userId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountCreationPayload("CURRENT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(containsString("primary FinFlow account already exists")));

        mockMvc.perform(post("/api/accounts/" + accountId + "/deposit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"description\":\"Salary\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25,\"description\":\"Pharmacy medicine\",\"pin\":\"123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/" + accountId + "/transactions")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[0].category").value("MEDICAL"))
                .andExpect(jsonPath("$[0].categorySource").value("AUTO_DETECTED"))
                .andExpect(jsonPath("$[0].categoryConfidence").value("HIGH"));

        String transactions = mockMvc.perform(get("/api/accounts/" + accountId + "/transactions")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long transactionId = objectMapper.readTree(transactions).get(0).path("id").asLong();
        mockMvc.perform(put("/api/accounts/" + accountId + "/transactions/" + transactionId + "/category")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"BILLS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("BILLS"))
                .andExpect(jsonPath("$.categorySource").value("USER_SELECTED"))
                .andExpect(jsonPath("$.originalDetectedCategory").doesNotExist());

        mockMvc.perform(post("/api/goals/assistant/plan")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"I want to buy a laptop for INR 80000 in 6 months\","
                                + "\"chosenMonthlySaving\":5000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SANDBOX_DETERMINISTIC"))
                .andExpect(jsonPath("$.recommendation.goalName").value("Laptop"))
                .andExpect(jsonPath("$.recommendation.necessity").value("IMPORTANT"))
                .andExpect(jsonPath("$.recommendation.requiredMonthlySavings").isNumber())
                .andExpect(jsonPath("$.recommendation.requiredWeeklySavings").isNumber())
                .andExpect(jsonPath("$.recommendation.chosenMonthlySaving").value(5000))
                .andExpect(jsonPath("$.recommendation.projectedCompletionDate").exists())
                .andExpect(jsonPath("$.recommendation.achievable").value(false))
                .andExpect(jsonPath("$.recommendation.suggestedActions.length()").isNumber())
                .andExpect(jsonPath("$.recommendation.keepProtected.length()").isNumber())
                .andExpect(jsonPath("$.snapshot.spendingBreakdown[0].necessity").value("NECESSARY"));

        mockMvc.perform(post("/api/goals/assistant/plan")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalName\":\"Small buffer\",\"targetAmount\":50,"
                                + "\"chosenMonthlySaving\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation.goalName").value("Small buffer"))
                .andExpect(jsonPath("$.recommendation.achievable").value(true))
                .andExpect(jsonPath("$.recommendation.remainingAmount").value(0));
    }

    @Test
    @Transactional
    void financePlanningEndpointsPersistAndStayAuthenticated() throws Exception {
        mockMvc.perform(get("/api/budgets")).andExpect(status().isUnauthorized());

        String email = UUID.randomUUID() + "@finance.test";
        String password = "Test@123";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Finance Planning Test",
                                "email", email,
                                "password", password,
                                "phone", validPhone()))))
                .andExpect(status().isCreated());
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).path("token").asText();

        String budget = objectMapper.writeValueAsString(Map.of(
                "category", "FOOD",
                "budgetAmount", 100,
                "startDate", java.time.LocalDate.now().withDayOfMonth(1).toString(),
                "endDate", java.time.LocalDate.now().withDayOfMonth(
                        java.time.LocalDate.now().lengthOfMonth()).toString()));
        String budgetResponse = mockMvc.perform(post("/api/budgets").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(budget))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spentAmount").value(0))
                .andExpect(jsonPath("$.remainingAmount").value(100))
                .andExpect(jsonPath("$.percentageUsed").value(0))
                .andReturn().getResponse().getContentAsString();
        long budgetId = objectMapper.readTree(budgetResponse).path("id").asLong();
        mockMvc.perform(post("/api/budgets").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FOOD\",\"budgetAmount\":0,\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-30\"}"))
                .andExpect(status().isBadRequest());

        String bill = objectMapper.writeValueAsString(Map.of(
                "name", "Test electricity bill",
                "amount", 999,
                "category", "BILLS",
                "frequency", "MONTHLY",
                "nextDueDate", java.time.LocalDate.now().plusDays(3).toString(),
                "reminderEnabled", true,
                "active", true));
        mockMvc.perform(post("/api/bills").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(bill))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test electricity bill"));
        mockMvc.perform(get("/api/bills/upcoming").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].daysUntilDue").value(3));

        long userId = objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .path("id").asLong();
        long accountId = objectMapper.readTree(mockMvc.perform(post("/api/accounts/user/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountCreationPayload("SAVINGS")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asLong();
        mockMvc.perform(post("/api/accounts/" + accountId + "/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"description\":\"Budget calculation test\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":80,\"category\":\"FOOD\",\"pin\":\"123456\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/budgets/" + budgetId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount").value(80))
                .andExpect(jsonPath("$.remainingAmount").value(20))
                .andExpect(jsonPath("$.percentageUsed").value(80));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BUDGET_WARNING")));
        mockMvc.perform(post("/api/accounts/" + accountId + "/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":3000}"))
                .andExpect(status().isOk());
        for (int amount : new int[]{100, 200, 300, 1000}) {
            mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":" + amount + ",\"category\":\"FOOD\",\"pin\":\"123456\"}"))
                    .andExpect(status().isOk());
        }

        String goal = objectMapper.writeValueAsString(Map.of(
                "name", "Test emergency fund",
                "targetAmount", 100000,
                "currentAmount", 35000,
                "targetDate", java.time.LocalDate.now().plusMonths(9).toString(),
                "description", "Test goal"));
        String goalResponse = mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(goal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.percentageComplete").value(35))
                .andExpect(jsonPath("$.remainingAmount").value(65000))
                .andExpect(jsonPath("$.requiredMonthlySavings").isNumber())
                .andReturn().getResponse().getContentAsString();
        long goalId = objectMapper.readTree(goalResponse).path("id").asLong();
        mockMvc.perform(post("/api/goals/" + goalId + "/contributions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(40000))
                .andExpect(jsonPath("$.percentageComplete").value(40))
                .andExpect(jsonPath("$.priority").value("IMPORTANT"))
                .andExpect(jsonPath("$.requiredDailySavings").isNumber());
        String savingsAutopay = mockMvc.perform(post("/api/goals/" + goalId + "/savings-autopay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accountId + ",\"amount\":10,"
                                + "\"frequency\":\"DAILY\",\"nextPaymentDate\":\""
                                + java.time.LocalDate.now() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("FINFLOW_SANDBOX"))
                .andExpect(jsonPath("$.frequency").value("DAILY"))
                .andExpect(jsonPath("$.savingsGoalId").value(goalId))
                .andReturn().getResponse().getContentAsString();
        long savingsAutopayId = objectMapper.readTree(savingsAutopay).path("id").asLong();
        mockMvc.perform(post("/api/autopay/" + savingsAutopayId + "/execute")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.isSimulated").value(true));
        mockMvc.perform(get("/api/goals/" + goalId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(40010))
                .andExpect(jsonPath("$.activeSavingsAutopayId").value(savingsAutopayId));
        mockMvc.perform(get("/api/financial-health").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.factors.length()").value(4));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UPCOMING_BILL")))
                .andExpect(content().string(containsString("BUDGET_WARNING")))
                .andExpect(content().string(containsString("BUDGET_EXCEEDED")))
                .andExpect(content().string(containsString("UNUSUAL_TRANSACTION")));
        mockMvc.perform(put("/api/notifications/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").isNumber());
        mockMvc.perform(get("/api/reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.currentBalance").value(1410))
                .andExpect(jsonPath("$.upcomingBills.length()").value(1))
                .andExpect(jsonPath("$.goals.length()").value(1));

        String otherEmail = UUID.randomUUID() + "@finance.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Other Finance User",
                                "email", otherEmail,
                                "password", password,
                                "phone", validPhone()))))
                .andExpect(status().isCreated());
        String otherToken = objectMapper.readTree(mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "email", otherEmail, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .path("token").asText();
        mockMvc.perform(get("/api/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/goals/" + goalId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void walletAllocationsAndSpendingLimitsUseTheAccountLedger() throws Exception {
        String email = UUID.randomUUID() + "@wallet.test";
        String password = "Test@123";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Wallet Test",
                                "email", email,
                                "password", password,
                                "phone", validPhone()))))
                .andExpect(status().isCreated());

        String token = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .path("token").asText();
        String authorization = "Bearer " + token;

        String walletBody = mockMvc.perform(post("/api/wallet").header("Authorization", authorization))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalBalance").value(0))
                .andExpect(jsonPath("$.availableBalance").value(0))
                .andReturn().getResponse().getContentAsString();
        long walletId = objectMapper.readTree(walletBody).path("id").asLong();

        mockMvc.perform(post("/api/money-rules").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Savings\",\"percentage\":30,"
                                + "\"destinationType\":\"SAVINGS\",\"priority\":1,\"active\":true}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/money-rules").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spending\",\"percentage\":70,"
                                + "\"destinationType\":\"SPENDING\",\"priority\":2,\"active\":true}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/money-rules").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Over-allocation\",\"percentage\":0.01,"
                                + "\"destinationType\":\"BILLS\",\"priority\":3,\"active\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_MONEY_RULE"));

        long userId = objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", authorization))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .path("id").asLong();
        long accountId = objectMapper.readTree(mockMvc.perform(post("/api/accounts/user/" + userId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountCreationPayload("SAVINGS")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/accounts/" + accountId + "/deposit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000,\"description\":\"Salary\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/wallet").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(1000))
                .andExpect(jsonPath("$.reservedBalance").value(300))
                .andExpect(jsonPath("$.availableBalance").value(700));

        mockMvc.perform(post("/api/spending-limits").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scopeType\":\"MONTHLY\",\"amount\":500}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":501,\"category\":\"FOOD\",\"pin\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SPENDING_LIMIT_EXCEEDED"));
        mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"category\":\"FOOD\",\"pin\":\"123456\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":201,\"category\":\"FOOD\",\"pin\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_AVAILABLE_BALANCE"));

        mockMvc.perform(get("/api/money/safety").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(500))
                .andExpect(jsonPath("$.reservedBalance").value(300))
                .andExpect(jsonPath("$.availableBalance").value(200))
                .andExpect(jsonPath("$.spentThisMonth").value(500))
                .andExpect(jsonPath("$.remainingMonthlyAllowance").value(0))
                .andExpect(jsonPath("$.safeToSpend").value(0));
        mockMvc.perform(get("/api/wallet/transactions").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/wallet/" + walletId).header("Authorization", authorization))
                .andExpect(status().isOk());

        String otherEmail = UUID.randomUUID() + "@wallet.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Other Wallet User",
                                "email", otherEmail,
                                "password", password,
                                "phone", validPhone()))))
                .andExpect(status().isCreated());
        String otherToken = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", otherEmail, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .path("token").asText();
        mockMvc.perform(get("/api/wallet/" + walletId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void finFlowPaymentsAreAtomicIdempotentAndVisibleToBothWalletOwners() throws Exception {
        String password = "Test@123";
        String senderEmail = UUID.randomUUID() + "@pay.test";
        String receiverEmail = UUID.randomUUID() + "@pay.test";
        String senderToken = registerAndLogin(senderEmail, password, "Payment Sender");
        String receiverToken = registerAndLogin(receiverEmail, password, "Payment Receiver");

        String senderWallet = mockMvc.perform(post("/api/wallet").header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String receiverWallet = mockMvc.perform(post("/api/wallet").header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String receiverPaymentId = objectMapper.readTree(receiverWallet).path("paymentId").asText();

        long senderUserId = currentUserId(senderToken);
        long receiverUserId = currentUserId(receiverToken);
        long senderAccountId = createAccount(senderToken, senderUserId);
        long receiverAccountId = createAccount(receiverToken, receiverUserId);
        mockMvc.perform(post("/api/accounts/" + senderAccountId + "/deposit")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500}"))
                .andExpect(status().isOk());

        String qr = mockMvc.perform(post("/api/payments/qr/create")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload").value("finflow:pay:v1:" + receiverPaymentId))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/payments/qr/scan")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payload", objectMapper.readTree(qr).path("payload").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("Payment Receiver"));

        String key = UUID.randomUUID().toString();
        String paymentBody = "{\"senderAccountId\":" + senderAccountId
                + ",\"receiverPaymentId\":\"" + receiverPaymentId
                + "\",\"amount\":125.50,\"category\":\"FOOD\",\"description\":\"Lunch\",\"pin\":\"123456\"}";
        String firstPayment = mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.provider").value("FINFLOW_INTERNAL"))
                .andReturn().getResponse().getContentAsString();
        long paymentId = objectMapper.readTree(firstPayment).path("id").asLong();
        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId));
        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody.replace("125.50", "126.50")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/wallet").header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(374.50));
        mockMvc.perform(get("/api/wallet").header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(125.50));
        mockMvc.perform(get("/api/payments/history").header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/payments/" + paymentId)
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber").value(
                        objectMapper.readTree(firstPayment).path("referenceNumber").asText()));
        mockMvc.perform(get("/api/accounts/" + receiverAccountId)
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.50));
    }

    @Test
    @Transactional
    void familyAllowanceIsReservedAndControlledSpendsRespectParentAndCategoryLimits() throws Exception {
        String password = "Test@123";
        String email = UUID.randomUUID() + "@family.test";
        String token = registerAndLogin(email, password, "Family Parent");
        String authorization = "Bearer " + token;
        long accountId = createAccount(token, currentUserId(token));
        mockMvc.perform(post("/api/accounts/" + accountId + "/deposit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/family").header("Authorization", authorization))
                .andExpect(status().isOk());
        String memberBody = mockMvc.perform(post("/api/family/members")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Kiddo\",\"monthlyAllowance\":300,"
                                + "\"approvalRequired\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long memberId = objectMapper.readTree(memberBody).path("id").asLong();

        String allowanceKey = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/family/members/" + memberId + "/allowances")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", allowanceKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accountId
                                + ",\"amount\":200,\"description\":\"Monthly allowance\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ALLOWANCE"));
        mockMvc.perform(post("/api/family/members/" + memberId + "/category-limits")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FOOD\",\"monthlyAmount\":50,\"pin\":\"123456\"}"))
                .andExpect(status().isOk());

        String spendKey = UUID.randomUUID().toString();
        String spendBody = "{\"accountId\":" + accountId
                + ",\"amount\":50,\"category\":\"FOOD\",\"description\":\"Lunch\","
                + "\"parentApproved\":false,\"pin\":\"123456\"}";
        mockMvc.perform(post("/api/family/members/" + memberId + "/spends")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", spendKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spendBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSFER"));
        mockMvc.perform(post("/api/family/members/" + memberId + "/spends")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spendBody.replace("\"parentApproved\":false", "\"parentApproved\":true")
                                .replace("\"amount\":50", "\"amount\":51")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SPENDING_LIMIT_EXCEEDED"));
        String approvedSpend = spendBody.replace("\"parentApproved\":false", "\"parentApproved\":true");
        String response = mockMvc.perform(post("/api/family/members/" + memberId + "/spends")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", spendKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvedSpend))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andReturn().getResponse().getContentAsString();
        long spendTransactionId = objectMapper.readTree(response).path("id").asLong();
        mockMvc.perform(post("/api/family/members/" + memberId + "/spends")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", spendKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvedSpend))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(spendTransactionId));

        mockMvc.perform(get("/api/family/members/" + memberId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedBalance").value(150))
                .andExpect(jsonPath("$.spentThisMonth").value(50))
                .andExpect(jsonPath("$.remainingAllowance").value(250));
        mockMvc.perform(get("/api/accounts/" + accountId).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(450));
        mockMvc.perform(get("/api/family/members/" + memberId + "/transactions")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(put("/api/family/members/" + memberId + "/freeze")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
        mockMvc.perform(post("/api/family/members/" + memberId + "/spends")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvedSpend))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void investmentEducationNeverFakesMarketDataAndAutopayRequiresAProvider() throws Exception {
        String password = "Test@123";
        String email = UUID.randomUUID() + "@invest.test";
        String token = registerAndLogin(email, password, "Investment Test");
        String authorization = "Bearer " + token;
        mockMvc.perform(get("/api/investments/education"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/investments/education").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disclaimer").value(containsString("not personalized")))
                .andExpect(jsonPath("$.categories.length()").value(8))
                .andExpect(jsonPath("$.topics.length()").value(13));
        mockMvc.perform(get("/api/investments/market-news").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("Live data unavailable - configure provider."))
                .andExpect(jsonPath("$.articles").isEmpty());
        mockMvc.perform(get("/api/news"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/news").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.message").value("Live data unavailable - configure provider."))
                .andExpect(jsonPath("$.articles").isEmpty());
        mockMvc.perform(get("/api/news/markets").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.articles").isEmpty());
        mockMvc.perform(get("/api/news/search?q=markets").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.articles").isEmpty());
        mockMvc.perform(get("/api/investments/market-snapshot")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.indices").isEmpty());

        String watchlistBody = mockMvc.perform(post("/api/investments/watchlist")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"ACME\",\"name\":\"Example Company\","
                                + "\"category\":\"STOCKS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marketDataAvailable").value(false))
                .andReturn().getResponse().getContentAsString();
        long watchlistId = objectMapper.readTree(watchlistBody).path("id").asLong();
        mockMvc.perform(post("/api/investments/watchlist")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"ACME\",\"name\":\"Example Company\","
                                + "\"category\":\"STOCKS\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/autopay").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/autopay")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":1,\"merchantName\":\"Example service\","
                                + "\"provider\":\"EXAMPLE\",\"amount\":10,\"maximumAmount\":12,"
                                + "\"frequency\":\"MONTHLY\",\"nextPaymentDate\":\""
                                + java.time.LocalDate.now().plusDays(10) + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("PAYMENT_PROVIDER_UNAVAILABLE"));
        mockMvc.perform(delete("/api/investments/watchlist/" + watchlistId)
                        .header("Authorization", authorization))
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void phase3PaymentIdAndQrAndInternalWalletPayments() throws Exception {
        String pass = "Test@123";
        String emailA = "usera_" + UUID.randomUUID() + "@finflow.test";
        String tokenA = registerAndLogin(emailA, pass, "User Alpha");
        String authA = "Bearer " + tokenA;
        long userAId = currentUserId(tokenA);
        long accAId = createAccount(tokenA, userAId);

        String emailB = "userb_" + UUID.randomUUID() + "@finflow.test";
        String tokenB = registerAndLogin(emailB, pass, "User Beta");
        String authB = "Bearer " + tokenB;
        long userBId = currentUserId(tokenB);
        long accBId = createAccount(tokenB, userBId);

        mockMvc.perform(post("/api/accounts/" + accAId + "/deposit")
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":2000}"))
                .andExpect(status().isOk());

        String walletABody = mockMvc.perform(post("/api/wallet")
                        .header("Authorization", authA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String paymentIdA = objectMapper.readTree(walletABody).path("paymentId").asText();

        String walletBBody = mockMvc.perform(post("/api/wallet")
                        .header("Authorization", authB))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String paymentIdB = objectMapper.readTree(walletBBody).path("paymentId").asText();

        mockMvc.perform(post("/api/wallet/fund")
                        .header("Authorization", authA)
                        .header("Idempotency-Key", "FUND-101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accAId + ",\"amount\":1000,\"description\":\"Seed wallet\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/qr/create")
                        .header("Authorization", authA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload").value(containsString("finflow:pay:v1:")));

        mockMvc.perform(post("/api/payments/resolve")
                        .header("Authorization", authA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"" + paymentIdB + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentIdB))
                .andExpect(jsonPath("$.recipientName").value("User Beta"));

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", authA)
                        .header("Idempotency-Key", "PAY-FAIL-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentId\":\"" + paymentIdA + "\",\"amount\":100,\"category\":\"OTHER\",\"pin\":\"123456\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", authA)
                        .header("Idempotency-Key", "PAY-FAIL-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentId\":\"unknown@finflow\",\"amount\":100,\"category\":\"OTHER\",\"pin\":\"123456\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", authA)
                        .header("Idempotency-Key", "PAY-FAIL-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentId\":\"" + paymentIdB + "\",\"amount\":0,\"category\":\"OTHER\",\"pin\":\"123456\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", authA)
                        .header("Idempotency-Key", "PAY-FAIL-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentId\":\"" + paymentIdB + "\",\"amount\":5000,\"category\":\"OTHER\",\"pin\":\"123456\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", authA)
                        .header("Idempotency-Key", "PAY-SUCCESS-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentId\":\"" + paymentIdB + "\",\"amount\":300,\"description\":\"Swiggy lunch\",\"pin\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber").value(containsString("FIN-")))
                .andExpect(jsonPath("$.amount").value(300))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", authA)
                        .header("Idempotency-Key", "PAY-SUCCESS-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentId\":\"" + paymentIdB + "\",\"amount\":300,\"description\":\"Swiggy lunch\",\"pin\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(300));

        mockMvc.perform(get("/api/wallet").header("Authorization", authA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.0));

        mockMvc.perform(get("/api/wallet").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(300.0));

        mockMvc.perform(get("/api/payments/history").header("Authorization", authA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/payments/history").header("Authorization", authB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private String registerAndLogin(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "email", email,
                                "password", password,
                                "phone", validPhone()))))
                .andExpect(status().isCreated());
        return objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .path("token").asText();
    }

    private long currentUserId(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .path("id").asLong();
    }

    private long createAccount(String token, long userId) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/accounts/user/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountCreationPayload("SAVINGS")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString())
                .path("id").asLong();
    }

    private String accountCreationPayload(String accountType) {
        return "{\"accountType\":\"" + accountType
                + "\",\"sandboxIdentityConfirmation\":\"CONFIRM\","
                + "\"transactionPin\":\"123456\","
                + "\"confirmTransactionPin\":\"123456\"}";
    }

    private String validPhone() {
        long value = Math.floorMod(
                UUID.randomUUID().getMostSignificantBits(),
                10_000_000_000L
        );
        return String.format("%010d", value);
    }
}
