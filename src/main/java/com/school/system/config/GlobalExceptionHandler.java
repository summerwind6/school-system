package com.school.system.config;

import com.school.system.common.Result;
import com.school.system.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Result<String>> handleForbidden() {
        return ResponseEntity.status(403).body(Result.error(403, "无权执行此操作"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<String>> handleInvalidParameter() {
        return ResponseEntity.badRequest().body(Result.error(400, "请求参数不合法"));
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<String>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(Result.error(e.getStatus(), e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() == null ? "请求参数不合法" : e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.badRequest().body(Result.error(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception e) {
        log.error("请求处理失败", e);
        return ResponseEntity.internalServerError().body(Result.error(500, "系统繁忙，请稍后重试"));
    }

    @ExceptionHandler({TransientDataAccessException.class, CannotGetJdbcConnectionException.class,
            CannotCreateTransactionException.class})
    public ResponseEntity<Result<String>> handleDatabaseUnavailable(Exception e) {
        log.warn("数据库暂时不可用", e);
        return ResponseEntity.status(503).body(Result.error(503, "系统繁忙，请稍后重试"));
    }
}
