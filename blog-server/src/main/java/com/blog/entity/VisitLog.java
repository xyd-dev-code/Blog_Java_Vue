package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("visit_log")
public class VisitLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 客户端 IP(已尽量去伪 XFF,落到直连 IP) */
    private String ip;
    /** 设备大类:PC/Mobile/Tablet */
    private String deviceType;
    /** 操作系统:Windows/macOS/iOS/Android/Linux... */
    private String os;
    /** 浏览器:Chrome/Safari/Firefox/Edge/QQ/WeChat... */
    private String browser;
    /** 访问路径(如 /api/v1/articles/foo) */
    private String path;
    /** 原始 User-Agent(全文) */
    private String userAgent;
    /** 访问时间 */
    private LocalDateTime visitTime;
    /** 省份/地区(基于 ip2region 离线库解析,数据文件缺失时为空) */
    private String province;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getVisitTime() { return visitTime; }
    public void setVisitTime(LocalDateTime visitTime) { this.visitTime = visitTime; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
}