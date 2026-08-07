package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员后台回复评论/留言的请求体。
 */
public class AdminReplyDTO {
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容过长")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
