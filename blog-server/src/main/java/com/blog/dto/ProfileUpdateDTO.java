package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateDTO {

    @Size(max = 30, message = "昵称不能超过 30 字符")
    private String nickname;

    @Size(max = 500)
    @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "头像链接必须以 http/https 开头")
    private String avatar;

    @Size(max = 100)
    @Email(message = "邮箱格式不正确")
    private String email;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
