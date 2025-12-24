package com.example.modulith.poc.channel.web.dto.common;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * エラーレスポンス
 */
public record ErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        String path,
        List<ErrorDetail> details
) {
    public ErrorResponse(String code, String message, String path) {
        this(code, message, OffsetDateTime.now(), path, null);
    }

    public ErrorResponse(String code, String message, String path, List<ErrorDetail> details) {
        this(code, message, OffsetDateTime.now(), path, details);
    }
}
