package com.kjweb.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(CustomAppException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomAppException ex,
                                                                     HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        log.error("[API:{}] {} - path: {}", errorCode.getCode(), ex.getDetailMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(errorCode.getStatus())
                .body(Map.of(
                        "success", false,
                        "code", errorCode.getCode(),
                        "message", ex.getDetailMessage(),
                        "path", request.getRequestURI(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex,
                                                               HttpServletRequest request) {
        log.error("[API:500] {} - path: {}", ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "success", false,
                        "code", "E500",
                        "message", ex.getMessage() == null ? "서버 오류가 발생했습니다" : ex.getMessage(),
                        "path", request.getRequestURI(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
