package com.finflow.controller;

import com.finflow.dto.ReceiptScanResponse;
import com.finflow.exception.InvalidReceiptException;
import com.finflow.service.CurrentUserService;
import com.finflow.service.ReceiptScanService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptScanService scanService;
    private final CurrentUserService currentUserService;

    public ReceiptController(ReceiptScanService scanService, CurrentUserService currentUserService) {
        this.scanService = scanService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(value = "/scan", consumes = "multipart/form-data")
    public ReceiptScanResponse scan(@RequestParam("file") MultipartFile file) {
        currentUserService.get();
        if (file == null || file.isEmpty()) {
            throw new InvalidReceiptException("Choose an image to scan.");
        }
        try {
            return scanService.scan(file.getBytes(), file.getContentType());
        } catch (IOException exception) {
            throw new InvalidReceiptException("The uploaded file could not be read.");
        }
    }
}
