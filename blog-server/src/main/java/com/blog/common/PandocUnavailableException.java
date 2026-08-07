package com.blog.common;

/** 标识 pandoc 二进制缺失 / 不可执行 — 上游应映射为 HTTP 503 */
public class PandocUnavailableException extends BizException {
    public PandocUnavailableException(String message) {
        super(503, message);
    }
}
