package com.finflow.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    public EmailProvider emailProvider(
            @Value("${finflow.mail.host:}") String host,
            @Value("${finflow.mail.port:587}") int port,
            @Value("${finflow.mail.username:}") String username,
            @Value("${finflow.mail.password:}") String password,
            @Value("${finflow.mail.from:}") String from,
            @Value("${finflow.mail.starttls:true}") boolean starttls) {
        if (host == null || host.isBlank()) {
            log.info("Email provider: LOG (MAIL_HOST not set). Emails are logged, not delivered.");
            return new LogEmailProvider();
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(port);
        boolean auth = username != null && !username.isBlank();
        if (auth) {
            sender.setUsername(username.trim());
            sender.setPassword(password);
        }
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        String sendFrom = from != null && !from.isBlank() ? from.trim() : (auth ? username.trim() : "no-reply@finflow.local");
        log.info("Email provider: SMTP host={} port={} from={}", host.trim(), port, sendFrom);
        return new SmtpEmailProvider(sender, sendFrom);
    }
}
