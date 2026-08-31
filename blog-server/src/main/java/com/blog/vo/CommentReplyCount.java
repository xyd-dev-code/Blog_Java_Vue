package com.blog.vo;

import lombok.Data;

@Data
public class CommentReplyCount {
    private Long parentId;
    private long count;
}
