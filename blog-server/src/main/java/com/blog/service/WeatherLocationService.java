package com.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/** 可选的区县 IP 数据源。未配置时由浏览器直接使用免密钥城市接口。 */
@Service
public class WeatherLocationService {
    private final String apiKey;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final Cache<String, Optional<Location>> cache = Caffeine.newBuilder()
            .maximumSize(2000).expireAfterWrite(Duration.ofMinutes(10)).build();

    public WeatherLocationService(@Value("${BLOG_WEATHER_IP2LOCATION_KEY:}") String apiKey, ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.mapper = mapper;
    }

    // 只返回天气所需的数据，不向前端暴露访客 IP、API Key 或上游错误正文。
    public record Location(String city, String district, double lat, double lon) {}

    public Location locate(String ip) {
        if (apiKey == null || apiKey.isBlank() || !isPublicIp(ip)) return null;
        return cache.get(ip, this::lookup).orElse(null);
    }

    private Optional<Location> lookup(String ip) {
        try {
            // 固定 HTTPS 上游，IP 必须显式传入，禁止使用服务器自己的出口位置。
            String url = "https://api.ip2location.io/?ip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return Optional.empty();
            return Optional.ofNullable(parseLocation(mapper.readTree(response.body())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            // 上游不可用/套餐无权限时安静降级，不泄露凭证或访客 IP。
            return Optional.empty();
        }
    }

    static Location parseLocation(JsonNode data) {
        if (data == null || data.hasNonNull("error")) return null;
        String city = clean(data.path("city_name").asText());
        String district = clean(data.path("district").asText());
        // 该接口在城市未知时可能返回首都坐标，不能把它当访客所在地。
        if (city.isEmpty() || !data.path("latitude").isNumber() || !data.path("longitude").isNumber()) return null;
        double lat = data.path("latitude").asDouble();
        double lon = data.path("longitude").asDouble();
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) return null;
        return new Location(city, district, lat, lon);
    }

    private static String clean(String value) {
        String s = value == null ? "" : value.trim();
        return s.equals("-") || s.equals("0") || s.equalsIgnoreCase("N/A") ? "" : s;
    }

    static boolean isPublicIp(String value) {
        // 先限制为 IP 字面量，避免 InetAddress 对用户控制的域名发起 DNS 查询。
        if (value == null || !value.matches("[0-9a-fA-F:.]{3,45}")) return false;
        if (!value.contains(":") && !value.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) return false;
        try {
            InetAddress address = InetAddress.getByName(value);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
            byte[] bytes = address.getAddress();
            if (bytes.length == 4) {
                int first = bytes[0] & 255;
                int second = bytes[1] & 255;
                return first != 0 && first < 224 && !(first == 100 && second >= 64 && second <= 127)
                        && !(first == 198 && (second == 18 || second == 19));
            }
            // IPv6 仅接受全球单播 2000::/3；排除 ULA/映射私网等本地来源。
            return (bytes[0] & 0xe0) == 0x20;
        } catch (Exception e) {
            return false;
        }
    }
}
