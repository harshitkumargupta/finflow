package com.finflow;

import com.finflow.entity.User;
import com.finflow.repository.*;
import com.finflow.service.FinanceNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

class NotificationBatchingTest {

    @Test
    void usersAreProcessedInKeysetBatchesOf200() {
        List<Integer> requestedSizes = new ArrayList<>();
        List<Long> users = LongStream.rangeClosed(1, 450).boxed().toList();
        UserRepository userRepository = proxy(UserRepository.class, (method, args) -> {
            if (!method.equals("findByIdGreaterThanOrderByIdAsc")) return null;
            long after = (Long) args[0];
            int size = ((Pageable) args[1]).getPageSize();
            requestedSizes.add(size);
            return users.stream().filter(id -> id > after).limit(size).map(this::user).toList();
        });
        FinanceNotificationService service = new FinanceNotificationService(
                null, null, null, null, null, null, null, null,
                proxy(AutopayAuthorizationRepository.class, (method, args) -> List.of()),
                proxy(BillRepository.class, (method, args) -> List.of()),
                proxy(BudgetRepository.class, (method, args) -> List.of()),
                proxy(SavingsGoalRepository.class, (method, args) -> List.of()),
                userRepository);

        assertEquals(200L, service.generateForUserBatch(null));
        assertEquals(400L, service.generateForUserBatch(200L));
        assertNull(service.generateForUserBatch(400L));
        assertEquals(List.of(200, 200, 200), requestedSizes);
    }

    private User user(long id) {
        User user = new User("User " + id, id + "@example.test", "9876543210", "hash", LocalDateTime.now());
        try { var field = User.class.getDeclaredField("id"); field.setAccessible(true); field.set(user, id); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        return user;
    }

    private interface Handler { Object handle(String method, Object[] args); }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            Object result = handler.handle(method.getName(), args);
            if (result == null && List.class.isAssignableFrom(method.getReturnType())) return List.of();
            if (result == null && method.getReturnType() == boolean.class) return false;
            return result;
        });
    }
}
