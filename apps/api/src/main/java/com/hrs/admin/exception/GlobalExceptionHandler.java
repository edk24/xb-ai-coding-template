package com.hrs.admin.exception;

import com.hrs.admin.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity
            .status(e.httpStatus())
            .body(ApiResponse.error(e.httpStatus() == 401 ? 401 : 1, e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException manv && manv.getBindingResult().hasFieldErrors()) {
            message = manv.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        } else if (e instanceof BindException bind && bind.getBindingResult().hasFieldErrors()) {
            message = bind.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        }
        return ResponseEntity.status(422).body(ApiResponse.error(1, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(1, e.getMessage() == null ? "系统错误" : e.getMessage()));
    }
}
