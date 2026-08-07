package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentReportDTO {
    @NotBlank
    @Size(max = 50, message = "举报理由过长")
    private String reason;

    @Size(max = 500, message = "补充说明不能超过 500 字")
    private String detail;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
