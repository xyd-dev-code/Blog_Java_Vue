package com.blog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 客户端 IP 解析:
 * - 不再直接信任 X-Forwarded-For,那是客户端可伪造的 header
 * - 在反代/网关后面部署时,前置 nginx 应配置:
 *     proxy_set_header X-Real-IP $remote_addr;
 *     proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
 * - 本 resolver 的约定:直连来源是<b>可信反代</b>(同机 nginx,即 127.0.0.1/::1)时,
 *   信任反代用 $remote_addr 写入的 X-Real-IP / X-Forwarded-For(客户端无法伪造,
 *   因为 nginx 会覆盖/追加)。其余情况用 getRemoteAddr() 防伪造。
 *
 * <p>说明:也可用传统方案 server.tomcat.remoteip(RemoteIpValve)让容器把
 * getRemoteAddr() 改写成真实 IP,二者取其一即可;本实现不依赖该配置,
 * 只要 nginx 带上 X-Real-IP 即生效。</p>
 */
@Component
public class ClientIpResolver {

    /** 可信反代的来源 IP:同机 nginx 直连后端时的 TCP peer。 */
    private static final java.util.Set<String> TRUSTED_PROXIES = java.util.Set.of(
            "127.0.0.1", "::1", "0:0:0:0:0:0:0:1"
    );

    /**
     * 取请求来源 IP。
     * <ul>
     *   <li>直连来源是可信反代(127.0.0.1/::1):采用反代写入的真实访客 IP
     *       (X-Real-IP 优先,否则 X-Forwarded-For 末跳);取不到则回落到 getRemoteAddr()。</li>
     *   <li>其他直连来源(本机直接打后端、外部直连):用 getRemoteAddr() 防 XFF 伪造。</li>
     * </ul>
     */
    public String resolve(HttpServletRequest req) {
        if (req == null) return "0.0.0.0";
        String remote = req.getRemoteAddr();
        if (remote != null && TRUSTED_PROXIES.contains(remote)) {
            String forwarded = resolveFromForwarded(req);
            if (!"0.0.0.0".equals(forwarded)) {
                return forwarded;
            }
        }
        return (remote == null || remote.isBlank()) ? "0.0.0.0" : remote;
    }

    /**
     * 从反代写入的头里取真实访客 IP:优先 X-Real-IP(由 nginx 用 $remote_addr 写入,
     * 不可伪造);否则取 X-Forwarded-For 的<b>末跳</b>(nginx 用
     * $proxy_add_x_forwarded_for 追加的那一跳,客户端无法控制)。都取不到回落 getRemoteAddr()。
     *
     * <p>本方法由 {@link #resolve(HttpServletRequest)} 在"直连来源是可信反代"时调用,
     * 不要求运维额外配置 server.tomcat.remoteip。</p>
     */
    public String resolveFromForwarded(HttpServletRequest req) {
        if (req == null) return "0.0.0.0";
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            int lastComma = xff.lastIndexOf(',');
            String last = (lastComma >= 0 ? xff.substring(lastComma + 1) : xff).trim();
            if (!last.isEmpty()) return last;
        }
        String xri = req.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        String remote = req.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "0.0.0.0" : remote;
    }
}
