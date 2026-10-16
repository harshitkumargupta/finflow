package com.finflow.receipt;

public interface ReceiptScanProvider {
    String name();

    boolean available();

    OcrResult extract(byte[] image);
}
