package com.xxl.job.core.biz.exceptions;

public class ApiInvokeException extends Exception {
    public ApiInvokeException() {
    }

    public ApiInvokeException(String message) {
        super(message);
    }

    public ApiInvokeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiInvokeException(Throwable cause) {
        super(cause);
    }

    public ApiInvokeException(String message, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
