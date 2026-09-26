package com.investment.analysis.common;

/**
 * 指数数据获取/处理异常。
 */
public class IndexDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IndexDataException(String message) {
        super(message);
    }

    public IndexDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
