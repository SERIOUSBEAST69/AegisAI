package com.trustai.exception;

import com.trustai.utils.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public R<?> handleBiz(BizException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<?> handle(Exception e) {
        return R.error(50000, e.getMessage());
    }
}
