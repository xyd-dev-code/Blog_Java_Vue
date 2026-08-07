package com.blog.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        log.warn("Biz: {}", e.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (e.getCode() == 401) status = HttpStatus.UNAUTHORIZED;
        else if (e.getCode() == 403) status = HttpStatus.FORBIDDEN;
        else if (e.getCode() == 404) status = HttpStatus.NOT_FOUND;
        else if (e.getCode() == 429) status = HttpStatus.TOO_MANY_REQUESTS;
        else if (e.getCode() == 503) status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(R.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("参数错误");
        return ResponseEntity.badRequest().body(R.fail(400, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("参数错误");
        return ResponseEntity.badRequest().body(R.fail(400, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse("参数错误");
        return ResponseEntity.badRequest().body(R.fail(400, msg));
    }

    /**
     * Spring 6.1 起，@RequestParam/@PathVariable 上的 @Min/@Max/@Size 等约束
     * 不再抛 ConstraintViolationException，而是 HandlerMethodValidationException。
     * 少了这个处理器就会掉进下面的 Exception 兜底，把 400 的参数错误报成 500。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<R<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        String msg = e.getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() == null ? "参数错误" : err.getDefaultMessage())
                .orElse("参数错误");
        return ResponseEntity.badRequest().body(R.fail(400, msg));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(405, "请求方法不允许"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(R.fail(400, "缺少必要参数"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(R.fail(400, "请求体格式错误"));
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<R<Void>> handleAuth(Exception e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(401, "用户名或密码错误"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.fail(403, "没有权限"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleAll(Exception e) {
        // 完整 stack 仅写服务端日志;对外只返回通用文案,避免泄露 SQL/pandoc/路径等内部细节。
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(500, "服务器内部错误"));
    }
}
