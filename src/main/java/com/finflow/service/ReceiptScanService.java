package com.finflow.service;

import com.finflow.dto.ReceiptFieldResponse;
import com.finflow.dto.ReceiptScanResponse;
import com.finflow.exception.InvalidReceiptException;
import com.finflow.receipt.ParsedField;
import com.finflow.receipt.ParsedReceipt;
import com.finflow.receipt.ReceiptScanProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ReceiptScanService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");
    private static final String UNAVAILABLE = "Receipt scanning is unavailable on this server. Configure an OCR provider to enable it.";

    private final ReceiptScanProvider provider;
    private final ReceiptParser parser;
    private final long maxBytes;

    public ReceiptScanService(ReceiptScanProvider provider, ReceiptParser parser,
                              @Value("${finflow.receipt.max-bytes:5000000}") long maxBytes) {
        this.provider = provider;
        this.parser = parser;
        this.maxBytes = maxBytes;
    }

    public boolean available() {
        return provider.available();
    }

    public ReceiptScanResponse scan(byte[] bytes, String contentType) {
        if (!provider.available()) {
            return ReceiptScanResponse.unavailable(UNAVAILABLE);
        }
        if (bytes == null || bytes.length == 0) {
            throw new InvalidReceiptException("The uploaded file is empty.");
        }
        if (bytes.length > maxBytes) {
            throw new InvalidReceiptException("The image is larger than the " + (maxBytes / 1_000_000) + " MB limit.");
        }
        String normalizedType = contentType == null ? "" : contentType.toLowerCase().trim();
        if (!ALLOWED_TYPES.contains(normalizedType)) {
            throw new InvalidReceiptException("Only PNG, JPEG or WebP images are supported.");
        }
        ParsedReceipt parsed = parser.parse(provider.extract(bytes));
        return new ReceiptScanResponse(true,
                "Draft extracted from your receipt. Review each field before saving.",
                field(parsed.merchant()),
                field(parsed.date()),
                field(parsed.total()),
                field(parsed.upiId()));
    }

    private ReceiptFieldResponse field(ParsedField<?> parsed) {
        return new ReceiptFieldResponse(parsed.value(), parsed.confidence(), parsed.flagged());
    }
}
