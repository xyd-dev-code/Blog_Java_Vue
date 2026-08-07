package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("comment_report")
public class CommentReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private String reason;
    private String detail;
    private String email;
    private String ip;
    private Integer status;
    private LocalDateTime createTime;

    // 后台列表用：被举报留言的昵称/摘要(非表字段)
    @TableField(exist = false) private String commentNickname;
    @TableField(exist = false) private String commentExcerpt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getCommentNickname() { return commentNickname; }
    public void setCommentNickname(String commentNickname) { this.commentNickname = commentNickname; }
    public String getCommentExcerpt() { return commentExcerpt; }
    public void setCommentExcerpt(String commentExcerpt) { this.commentExcerpt = commentExcerpt; }
}
