package com.finflow;

import com.finflow.dto.SimulateMandateRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmountJsonBindingTest {
    @Test
    void stringAmountsBindToBigDecimalWithoutFloatingPoint() {
        String json = """
                {"merchantName":"Netflix","amount":"649.10","maximumAmount":"0.30",
                 "frequency":"MONTHLY","nextPaymentDate":"2099-01-01"}
                """;
        SimulateMandateRequest request = JsonMapper.builder().findAndAddModules().build()
                .readValue(json, SimulateMandateRequest.class);
        assertEquals(new BigDecimal("649.10"), request.amount());
        assertEquals(new BigDecimal("0.30"), request.maximumAmount());
    }
}
