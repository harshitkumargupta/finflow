package com.finflow.payment;

public interface UPIPaymentProvider {
    String providerName();

    ProviderAuthorization authorize(String providerPaymentReference);

    record ProviderAuthorization(String providerReference, String status) {
    }
}
