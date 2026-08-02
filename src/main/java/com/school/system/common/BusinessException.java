package com.school.system.common;

import lombok.Getter;

/**
 * 可预期的业务失败。控制器不再吞掉异常，由全局处理器统一转换为 HTTP 响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
}
