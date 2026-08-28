package com.blog.util;

import com.blog.config.BlogProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 ip2region 离线库的 IP 归属地解析工具。
 *
 * <p>数据文件(ip2region.xdb)需自行下载并放入 classpath 或通过配置指定路径:
 * <pre>
 *   blog:
 *     ip2-region:
 *       db-path: classpath:ip2region.xdb
 * </pre>
 * 文件不存在时,所有解析方法返回空字符串,不影响业务。</p>
 *
 * <p>解析结果格式示例(原始): 中国|0|广东省|广州市|电信
 * 本工具对国内 IP 返回省份(第 3 段),对国外 IP 返回国家(第 1 段)。</p>
 */
@Component
public class IpRegionUtil {

    private static final Logger log = LoggerFactory.getLogger(IpRegionUtil.class);

    private final BlogProperties props;
    private Searcher searcher;
    private boolean available = false;

    public IpRegionUtil(BlogProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        String configured = props.getIp2Region().getDbPath();
        if (configured == null || configured.isBlank()) {
            log.warn("[IpRegion] 未配置 ip2region.xdb 路径,省份解析功能不可用");
            return;
        }

        Resource resource;
        if (configured.startsWith("classpath:")) {
            resource = new ClassPathResource(configured.substring("classpath:".length()));
        } else {
            resource = new FileSystemResource(configured);
        }

        if (!resource.exists()) {
            log.warn("[IpRegion] 配置的数据文件不存在，省份解析功能不可用");
            return;
        }

        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            this.searcher = Searcher.newWithBuffer(bytes);
            this.available = true;
            log.info("[IpRegion] ip2region 数据加载成功");
        } catch (IOException e) {
            log.warn("[IpRegion] 读取数据文件失败: {}", e.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("[IpRegion] 初始化 Searcher 失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * 解析 IP 对应的省份/地区。
     * 国内 IP 返回省份(如"北京"、"广东"),国外 IP 返回国家名,本地/内网/解析失败返回空字符串。
     */
    public String resolveProvince(String ip) {
        if (!available || ip == null || ip.isBlank()) return "";
        // 本地/内网 IP 不解析
        if (isPrivateOrLocal(ip)) return "";
        try {
            String region = searcher.search(ip);
            if (region == null || region.isBlank() || "0".equals(region)) return "";
            String[] parts = region.split("\\|");
            if (parts.length < 3) return parts[0];
            // 国内：中国|0|省份|城市|ISP -> 取省份；国外：国家|... -> 取国家
            if ("中国".equals(parts[0])) {
                return normalize(parts[2]);
            }
            return normalize(parts[0]);
        } catch (Exception e) {
            log.debug("[IpRegion] IP 地域解析失败: {}", e.getClass().getSimpleName());
            return "";
        }
    }

    private static String normalize(String s) {
        if (s == null || s.isBlank() || "0".equals(s)) return "";
        // 直辖市/特别行政区去掉末尾"市"字,保持统一（如"北京市"->"北京"）
        if (s.endsWith("市") && (s.startsWith("北京") || s.startsWith("上海") || s.startsWith("天津") || s.startsWith("重庆"))) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static boolean isPrivateOrLocal(String ip) {
        return ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("10.")
                || ip.startsWith("172.") || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)
                || "0.0.0.0".equals(ip) || ip.isBlank();
    }
}
