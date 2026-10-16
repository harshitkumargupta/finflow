package com.finflow.dto;

public record ReceiptScanResponse(boolean available, String message,
                                  ReceiptFieldResponse merchant,
                                  ReceiptFieldResponse date,
                                  ReceiptFieldResponse total,
                                  ReceiptFieldResponse upiId) {

    public static ReceiptScanResponse unavailable(String message) {
        return new ReceiptScanResponse(false, message, null, null, null, null);
    }
}
