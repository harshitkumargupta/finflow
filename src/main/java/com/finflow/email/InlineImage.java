package com.finflow.email;

public record InlineImage(String contentId, String contentType, byte[] bytes) {
}
