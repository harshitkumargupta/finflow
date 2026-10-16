package com.finflow.dto;

import org.springframework.data.domain.Page;

import java.util.function.Function;

public final class PageResponseMapper {

    private PageResponseMapper() {
    }

    public static <E, R> PageResponse<R> map(Page<E> page, Function<E, R> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
