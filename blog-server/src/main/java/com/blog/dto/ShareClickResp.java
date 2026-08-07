package com.blog.dto;

public class ShareClickResp {
    private Integer shareCount;
    private boolean counted;

    public ShareClickResp() {}
    public ShareClickResp(Integer shareCount, boolean counted) {
        this.shareCount = shareCount;
        this.counted = counted;
    }
    public Integer getShareCount() { return shareCount; }
    public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }
    public boolean isCounted() { return counted; }
    public void setCounted(boolean counted) { this.counted = counted; }
}