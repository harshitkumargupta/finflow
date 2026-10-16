package com.finflow.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(LogEmailProvider.class);

    @Override
    public String name() {
        return "LOG";
    }

    @Override
    public boolean deliversMail() {
        return false;
    }

    @Override
    public void send(EmailMessage message) {
        log.info("Email not sent (SMTP not configured). to={} subject=\"{}\"\n{}",
                EmailService.maskEmail(message.to()), message.subject(), message.text());
    }
}
