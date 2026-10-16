package com.finflow.email;

import java.util.List;

public record EmailMessage(String to, String subject, String text, String html, List<InlineImage> images) {
    public EmailMessage {
        images = images == null ? List.of() : List.copyOf(images);
    }
}
