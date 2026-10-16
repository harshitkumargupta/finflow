package com.finflow.controller;

import com.finflow.dto.NotificationResponse;
import com.finflow.dto.NotificationStateRequest;
import com.finflow.service.FinanceNotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class FinanceNotificationController {
    private final FinanceNotificationService notificationService;

    public FinanceNotificationController(FinanceNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Object list(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        if (!Paging.requested(page, size)) return notificationService.list(false);
        var pageable = Paging.of(page, size);
        return notificationService.list(false, pageable.getPageNumber(), pageable.getPageSize());
    }

    @GetMapping("/unread")
    public Object unread(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        if (!Paging.requested(page, size)) return notificationService.list(true);
        var pageable = Paging.of(page, size);
        return notificationService.list(true, pageable.getPageNumber(), pageable.getPageSize());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PatchMapping("/{id}")
    public NotificationResponse updateState(@PathVariable Long id,
                                             @Valid @RequestBody NotificationStateRequest request) {
        return notificationService.setRead(id, request.read());
    }

    @PutMapping("/read-all")
    public Map<String, Long> markAllRead() {
        return Map.of("updated", notificationService.markAllRead());
    }
}
