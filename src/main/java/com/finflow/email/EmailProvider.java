package com.finflow.email;

public interface EmailProvider {
    String name();

    boolean deliversMail();

    void send(EmailMessage message) throws Exception;
}
