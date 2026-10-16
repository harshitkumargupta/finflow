package com.finflow.email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender sender;
    private final String from;

    public SmtpEmailProvider(JavaMailSender sender, String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public String name() {
        return "SMTP";
    }

    @Override
    public boolean deliversMail() {
        return true;
    }

    @Override
    public void send(EmailMessage message) throws Exception {
        MimeMessage mime = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(message.to());
        helper.setSubject(message.subject());
        helper.setText(message.text(), message.html());
        for (InlineImage image : message.images()) {
            helper.addInline(image.contentId(), new ByteArrayResource(image.bytes()), image.contentType());
        }
        sender.send(mime);
    }
}
