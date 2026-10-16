package com.finflow.payment;

public interface CardPaymentProvider {
    String providerName();

    ProviderAuthorization authorize(String providerTokenReference, String paymentReference);

    record ProviderAuthorization(String providerReference, String status) {
    }
}
