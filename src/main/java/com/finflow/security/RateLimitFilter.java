package com.finflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private record Window(long startedAt, int count) {}
    private static final int MAX_TRACKED_KEYS = 10_000;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int loginLimit;
    private final int registerLimit;
    private final int pinLimit;
    private final int qrLimit;
    private final int otpLimit;
    private final long windowSeconds;

    public RateLimitFilter(@Value("${finflow.rate-limit.login-per-window:10}") int loginLimit,
                           @Value("${finflow.rate-limit.register-per-window:5}") int registerLimit,
                           @Value("${finflow.rate-limit.pin-per-window:12}") int pinLimit,
                           @Value("${finflow.rate-limit.qr-per-window:30}") int qrLimit,
                           @Value("${finflow.rate-limit.otp-per-window:5}") int otpLimit,
                           @Value("${finflow.rate-limit.window-seconds:60}") long windowSeconds) {
        this.loginLimit = loginLimit;
        this.registerLimit = registerLimit;
        this.pinLimit = pinLimit;
        this.qrLimit = qrLimit;
        this.otpLimit = otpLimit;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = 0;
        String scope = null;
        if (path.equals("/api/auth/login")) { limit = loginLimit; scope = "login:" + clientPathKey(request, path); }
        else if (path.equals("/api/auth/register")) { limit = registerLimit; scope = "register:" + clientPathKey(request, path); }
        else if (isPinPath(path)) { limit = pinLimit; scope = "pin:" + principalOrIp(request); }
        else if (isQrPath(path)) { limit = qrLimit; scope = "qr:" + principalOrIp(request); }
        else if (isOtpPath(path)) { limit = otpLimit; scope = "otp:" + principalOrIp(request); }
        if (scope == null || allowed(scope, limit)) { chain.doFilter(request, response); return; }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(windowSeconds));
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests. Try again later.\"}");
    }

    private boolean allowed(String key, int limit) {
        long now = Instant.now().getEpochSecond();
        if (windows.size() > MAX_TRACKED_KEYS) windows.values().removeIf(window -> now - window.startedAt() >= windowSeconds);
        Window result = windows.compute(key, (ignored, old) -> {
            if (old == null || now - old.startedAt() >= windowSeconds) return new Window(now, 1);
            return new Window(old.startedAt(), old.count() + 1);
        });
        return result.count() <= limit;
    }

    private String clientPathKey(HttpServletRequest request, String path) {
        return request.getRemoteAddr() + ":" + path;
    }
    private String principalOrIp(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() ? authentication.getName() : request.getRemoteAddr();
    }
    private boolean isPinPath(String path) {
        return path.endsWith("/withdraw") || path.endsWith("/transfer") || path.endsWith("/confirm")
                || path.endsWith("/approve") || path.contains("/family/members/");
    }
    private boolean isQrPath(String path) {
        return path.endsWith("/wallet/resolve") || path.endsWith("/wallet/qr/scan") || path.endsWith("/payments/resolve-recipient");
    }
    private boolean isOtpPath(String path) {
        return path.equals("/api/users/me/phone-verification");
    }
}
