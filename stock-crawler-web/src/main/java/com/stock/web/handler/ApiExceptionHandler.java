package com.stock.web.handler;

import com.stock.crawler.model.DataResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * JSON API 统一异常响应。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<DataResult<Void>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        String source = status == HttpStatus.BAD_REQUEST && "Invalid stock code".equals(reason)
                ? "validation:stock-code"
                : "validation:request";
        return ResponseEntity.status(status).body(DataResult.failure(source, reason, 0));
    }
}
