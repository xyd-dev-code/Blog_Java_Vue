package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * 邮箱订阅（原 RSS 订阅的替代方案）。
 * 采用双重确认（double opt-in）：提交后落库为待确认(status=0)，
 * 发送确认邮件，用户点击链接后_status 置 1。未配置 SMTP 时自动直接确认，保证功能可用。
 */
@TableName("email_subscription")
public class EmailSubscription {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订阅邮箱（统一小写存储） */
    private String email;
    /** 0=待确认 1=已确认 */
    private Integer status;
    /** 确认令牌（UUID 去横杠），确认后保留以便幂等 */
    @JsonIgnore
    private String token;
    /** 订阅来源：web / admin ... */
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime confirmTime;
    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
