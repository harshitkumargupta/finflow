package com.finflow.controller;

import com.finflow.exception.InvalidPageRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

final class Paging {

    static final int DEFAULT_SIZE = 50;
    static final int MAX_SIZE = 100;

    private Paging() {
    }

    static boolean requested(Integer page, Integer size) {
        return page != null || size != null;
    }

    static Pageable of(Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0 || s < 1 || s > MAX_SIZE) {
            throw new InvalidPageRequestException("page must be >= 0 and size must be between 1 and " + MAX_SIZE);
        }
        return PageRequest.of(p, s);
    }
}
