package com.blog.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IpRegionUtilTest {
    @Test
    void foreignLocationsIncludeCountryStateAndCityInBothDatabaseFormats() {
        assertEquals("United States · California · Los Angeles",
                IpRegionUtil.parseRegion("United States|0|California|Los Angeles|Example ISP", true));
        assertEquals("美国 · 加利福尼亚州 · 洛杉矶",
                IpRegionUtil.parseRegion("美国|加利福尼亚州|洛杉矶|Example ISP|US", true));
        assertEquals("United States",
                IpRegionUtil.parseRegion("United States|0|California|Los Angeles|Example ISP", false));
    }

    @Test
    void missingAndDuplicateLayersDoNotInventLocations() {
        assertEquals("Singapore", IpRegionUtil.parseRegion("Singapore|Singapore|Singapore|ISP|SG", true));
        assertEquals("France · Paris", IpRegionUtil.parseRegion("France|0|0|Paris|ISP", true));
        assertEquals("Germany", IpRegionUtil.parseRegion("Germany|0|0|ISP|DE", true));
        assertEquals("Germany", IpRegionUtil.parseRegion("Germany", true));
        assertEquals("", IpRegionUtil.parseRegion(null, true));
        assertEquals("", IpRegionUtil.parseRegion("0|0|0|0|0", true));
    }

    @Test
    void domesticProvinceAndCityBehaviorIsPreserved() {
        assertEquals("广东省·广州市", IpRegionUtil.parseRegion("中国|0|广东省|广州市|电信", true));
        assertEquals("广东省", IpRegionUtil.parseRegion("中国|广东省|广州市|电信|CN", false));
        assertEquals("北京市", IpRegionUtil.parseRegion("中国|北京市|Beijing|ISP|CN", true));
    }
}
