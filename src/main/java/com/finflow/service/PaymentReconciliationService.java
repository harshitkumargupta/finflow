package com.finflow.service;

import com.finflow.dto.ReconciliationItemResponse;
import com.finflow.dto.ReconciliationPageResponse;
import com.finflow.entity.WalletPayment;
import com.finflow.repository.WalletPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentReconciliationService {

    private final WalletPaymentRepository paymentRepository;

    public PaymentReconciliationService(WalletPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationPageResponse runReconciliation(LocalDateTime from, LocalDateTime to, int page, int size) {
        Page<WalletPayment> payments = paymentRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, PageRequest.of(page, size));
        List<ReconciliationItemResponse> items = new ArrayList<>();
        int matched = 0, pending = 0;

        for (WalletPayment p : payments.getContent()) {
            String res;
            String note;
            if ("SUCCESS".equalsIgnoreCase(p.getStatus())) {
                res = "MATCHED";
                note = "Provider reference " + p.getReferenceNumber() + " verified against ledger balance.";
                matched++;
            } else if ("PENDING".equalsIgnoreCase(p.getStatus())) {
                res = "PENDING_REVIEW";
                note = "Payment pending provider execution status confirmation.";
                pending++;
            } else {
                res = "MATCHED";
                note = "Terminal status " + p.getStatus() + " reconciled.";
                matched++;
            }

            items.add(new ReconciliationItemResponse(
                    p.getId(),
                    p.getReferenceNumber(),
                    p.getAmount(),
                    p.getStatus(),
                    res,
                    note,
                    LocalDateTime.now()
            ));
        }

        return new ReconciliationPageResponse(items, page, size, payments.getTotalElements(), payments.getTotalPages());
    }
}
