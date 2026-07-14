package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.SiteConfig;
import com.blog.mapper.SiteConfigMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SiteConfigService {
    private final SiteConfigMapper siteConfigMapper;

    public SiteConfigService(SiteConfigMapper siteConfigMapper) {
        this.siteConfigMapper = siteConfigMapper;
    }

    public Map<String, String> allAsMap() {
        Map<String, String> map = new HashMap<>();
        siteConfigMapper.selectList(null).forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return map;
    }

    public String get(String key, String def) {
        SiteConfig c = siteConfigMapper.selectOne(
            new LambdaQueryWrapper<SiteConfig>().eq(SiteConfig::getConfigKey, key)
        );
        return c == null ? def : c.getConfigValue();
    }

    public void save(Map<String, String> data) {
        if (data == null) return;
        for (Map.Entry<String, String> e : data.entrySet()) {
            SiteConfig c = siteConfigMapper.selectOne(
                new LambdaQueryWrapper<SiteConfig>().eq(SiteConfig::getConfigKey, e.getKey())
            );
            if (c == null) {
                c = new SiteConfig();
                c.setConfigKey(e.getKey());
                c.setConfigValue(e.getValue());
                siteConfigMapper.insert(c);
            } else {
                c.setConfigValue(e.getValue());
                siteConfigMapper.updateById(c);
            }
        }
    }
}