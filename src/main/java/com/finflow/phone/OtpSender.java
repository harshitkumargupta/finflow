package com.finflow.phone;

import com.finflow.entity.User;

public interface OtpSender {
    String channel();

    boolean delivers();

    void sendOtp(User user, String code, long ttlMinutes);
}
