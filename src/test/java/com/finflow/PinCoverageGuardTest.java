package com.finflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PinCoverageGuardTest {
    @Test
    void moneyOutServiceMethodsRemainBehindPinVerification() throws Exception {
        Path root = Path.of("src/main/java/com/finflow/service");
        assertMethodContains(root.resolve("PaymentDomainService.java"), "executePayment", "pinVerificationService.verify");
        assertMethodContains(root.resolve("WalletLedgerService.java"), "transfer", "pinVerificationService.verify");
        assertMethodContains(root.resolve("AccountService.java"), "withdraw", "pinVerificationService.verify");
        assertMethodContains(root.resolve("AccountService.java"), "spendFamilyAllowance", "familyRequest.pin()");
        assertMethodContains(root.resolve("FamilyService.java"), "upsertCategoryLimit", "request.pin()");
    }

    private void assertMethodContains(Path file, String method, String expected) throws Exception {
        String source = Files.readString(file);
        int start = source.indexOf(method + "(");
        assertTrue(start >= 0, "Missing method " + method);
        int end = source.indexOf("\n    }", start);
        assertTrue(end > start, "Could not find method end for " + method);
        assertTrue(source.substring(start, end).contains(expected), "PIN guard missing from " + method);
    }
}
