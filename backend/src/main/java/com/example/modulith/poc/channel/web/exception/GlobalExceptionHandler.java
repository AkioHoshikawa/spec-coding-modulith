package com.example.modulith.poc.channel.web.exception;

import com.example.modulith.poc.channel.web.dto.common.ErrorDetail;
import com.example.modulith.poc.channel.web.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * グローバル例外ハンドラー
 * <p>
 * アプリケーション全体の例外を捕捉し、適切なHTTPレスポンスに変換する。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * バリデーション例外を処理
     *
     * @param ex      バリデーション例外
     * @param request HTTPリクエスト
     * @return 400 Bad Requestレスポンス
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {

        List<ErrorDetail> details = new ArrayList<>();
        ex.getParameterValidationResults().stream().forEach(r -> {
            if (r instanceof ParameterErrors paramErrors) {
                paramErrors.getFieldErrors().stream()
                        .map(error -> new ErrorDetail(
                                error.getField(),
                                error.getDefaultMessage()
                        ))
                        .forEach(details::add);

            }
        });

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "入力値が正しくありません",
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * その他の例外を処理
     *
     * @param ex      例外
     * @param request HTTPリクエスト
     * @return 500 Internal Server Errorレスポンス
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "システムエラーが発生しました",
                request.getRequestURI()
        );

        // ログ出力
        LOGGER.error("unhandled exception: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
