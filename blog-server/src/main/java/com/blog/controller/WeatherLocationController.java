package com.blog.controller;

import com.blog.common.R;
import com.blog.security.ClientIpResolver;
import com.blog.service.WeatherLocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherLocationController {
    private final ClientIpResolver ipResolver;
    private final WeatherLocationService locations;

    public WeatherLocationController(ClientIpResolver ipResolver, WeatherLocationService locations) {
        this.ipResolver = ipResolver;
        this.locations = locations;
    }

    @GetMapping("/api/v1/site/weather-location")
    public ResponseEntity<R<WeatherLocationService.Location>> location(HttpServletRequest request) {
        // 不能被 CDN/共享代理缓存，否则会把上一位访客的位置发给其他访客。
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(R.ok(locations.locate(ipResolver.resolve(request))));
    }
}
