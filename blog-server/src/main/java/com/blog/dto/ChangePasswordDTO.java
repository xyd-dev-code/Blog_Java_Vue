package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordDTO {
    @NotBlank
    @Size(min = 1, max = 128, message = "旧密码长度 1~128")
    private String oldPassword;

    @NotBlank
    @Size(min = 8, max = 128, message = "新密码长度 8~128")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,128}$",
             message = "新密码必须同时包含字母和数字")
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}