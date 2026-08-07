package com.blog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ShareClickReq {
    @NotNull @Positive
    private Long articleId;

    @Size(max = 20, message = "渠道名过长")
    @Pattern(regexp = "^(wechat|weibo|qq|douban|copy|link|native)?$",
             message = "不支持的渠道")
    private String channel;

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}