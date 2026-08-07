package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public class LoginDTO {
    @NotBlank
    @Size(min = 1, max = 64, message = "用户名长度需在 1~64")
    private String username;
    @NotBlank
    @Size(min = 1, max = 128, message = "密码长度需在 1~128")
    private String password;

    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return this.password; }
    public void setPassword(String password) { this.password = password; }
}