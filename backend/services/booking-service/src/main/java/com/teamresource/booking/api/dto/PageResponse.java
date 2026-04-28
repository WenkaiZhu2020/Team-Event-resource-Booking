package com.teamresource.booking.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> fromPage(Page<T> pageData) {
        return new PageResponse<>(
                pageData.getContent(),
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
