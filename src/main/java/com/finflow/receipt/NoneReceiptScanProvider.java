package com.finflow.receipt;

import com.finflow.exception.ProviderNotConfiguredException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "finflow.receipt.provider", havingValue = "NONE", matchIfMissing = true)
public class NoneReceiptScanProvider implements ReceiptScanProvider {

    @Override
    public String name() {
        return "NONE";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public OcrResult extract(byte[] image) {
        throw new ProviderNotConfiguredException("Receipt scanning is unavailable - configure RECEIPT_SCAN_PROVIDER.");
    }
}
