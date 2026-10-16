package com.finflow;

import com.finflow.service.FinanceNotificationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationDeduplicationTest {
    @Test
    void repeatedScheduledEventProducesTheSameKey() {
        assertEquals(
                FinanceNotificationService.scheduledEventKey("GOAL_MILESTONE", 7L, "75"),
                FinanceNotificationService.scheduledEventKey("GOAL_MILESTONE", 7L, "75"));
    }
}
