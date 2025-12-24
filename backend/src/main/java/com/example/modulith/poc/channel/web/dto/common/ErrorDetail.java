package com.example.modulith.poc.channel.web.dto.common;

/**
 * エラー詳細
 */
public record ErrorDetail(
        String field,
        String message
) {
}
