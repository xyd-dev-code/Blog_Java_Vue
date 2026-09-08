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
 * 兼容旧格式及“国家|省|市|ISP|ISO”新格式，提供省份与省市明细。</p>
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
        return parseRegion(searchRegion(ip), false);
    }

    /** 用于访客明细：国内显示省市，国外显示国家、州/省、城市。 */
    public String resolveLocation(String ip) {
        return parseRegion(searchRegion(ip), true);
    }

    private String searchRegion(String ip) {
        if (!available || ip == null || ip.isBlank() || isPrivateOrLocal(ip)) return "";
        try {
            return searcher.search(ip);
        } catch (Exception e) {
            log.debug("[IpRegion] IP 地域解析失败: {}", e.getClass().getSimpleName());
            return "";
        }
    }

    static String parseRegion(String region, boolean includeCity) {
        if (region == null || region.isBlank() || "0".equals(region)) return "";
        String[] parts = region.split("\\|", -1);
        String country = clean(parts[0]);
        // 新格式：国家|省|市|ISP|ISO；旧格式：国家|0|省|市|ISP。
        boolean modern = parts.length == 5 && parts[4].matches("[A-Z]{2}");
        boolean domestic = "中国".equals(country) || "China".equalsIgnoreCase(country)
                || (modern && "CN".equals(parts[4]));
        int provinceIndex = modern ? 1 : 2;
        int cityIndex = modern ? 2 : 3;
        String province = parts.length > provinceIndex ? clean(parts[provinceIndex]) : "";
        String city = parts.length > cityIndex ? clean(parts[cityIndex]) : "";
        if (!domestic) {
            if (!includeCity) return country;
            // 只使用地理字段，不将 ISP 或 ISO 编码当作城市；缺失层级跳过。
            java.util.List<String> location = new java.util.ArrayList<>();
            for (String part : new String[] { country, province, city }) {
                if (!part.isEmpty() && location.stream().noneMatch(part::equalsIgnoreCase)) {
                    location.add(part);
                }
            }
            return String.join(" · ", location);
        }
        if (!includeCity) return normalize(province);
        if (province.isEmpty()) return city;
        // 直辖市只显示一次，避免“北京市·Beijing”或“北京市·北京市”。
        if (province.matches("(北京|上海|天津|重庆)市?")) {
            return province.endsWith("市") ? province : province + "市";
        }
        if (city.isEmpty() || province.equals(city)) return province;
        return province + "·" + city;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() || "0".equals(value.trim()) ? "" : value.trim();
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
