package com.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherLocationServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void keepsExplicitDistrictAndCityWithoutInventingOne() throws Exception {
        var district = WeatherLocationService.parseLocation(mapper.readTree("""
                {"city_name":"长沙市","district":"雨花区","latitude":28.13,"longitude":113.03}
                """));
        assertNotNull(district);
        assertEquals("长沙市", district.city());
        assertEquals("雨花区", district.district());
        var cityOnly = WeatherLocationService.parseLocation(mapper.readTree("""
                {"city_name":"长沙市","latitude":28.13,"longitude":113.03}
                """));
        assertNotNull(cityOnly);
        assertEquals("", cityOnly.district());
    }

    @Test
    void rejectsUnknownCitiesCapitalFallbackAndInvalidCoordinates() throws Exception {
        for (String json : new String[] {
                "{\"city_name\":\"-\",\"latitude\":39.9,\"longitude\":116.4}",
                "{\"city_name\":\"长沙\",\"latitude\":null,\"longitude\":113}",
                "{\"city_name\":\"长沙\",\"latitude\":128,\"longitude\":113}",
                "{\"error\":{\"error_code\":10000},\"city_name\":\"长沙\",\"latitude\":28,\"longitude\":113}"
        }) assertNull(WeatherLocationService.parseLocation(mapper.readTree(json)));
    }

    @Test
    void onlyAcceptsPublicIpLiterals() {
        for (String ip : new String[] {"8.8.8.8", "172.15.0.1", "172.32.0.1", "2606:4700:4700::1111"}) {
            assertTrue(WeatherLocationService.isPublicIp(ip), ip);
        }
        for (String ip : new String[] {"localhost", "example.com", "127.0.0.1", "::1", "192.168.1.1",
                "10.1.1.1", "172.16.0.1", "172.31.255.255", "100.64.0.1", "169.254.1.1", "0.0.0.0",
                "fd00::1", "fe80::1", "::ffff:192.168.1.1", "999.1.1.1", "8.8.8.8, 1.1.1.1"}) {
            assertFalse(WeatherLocationService.isPublicIp(ip), ip);
        }
    }

    @Test
    void unconfiguredProviderReturnsNoLocationWithoutNetworkRequest() {
        assertNull(new WeatherLocationService("", mapper).locate("8.8.8.8"));
        assertNull(new WeatherLocationService("unused-test-key", mapper).locate("127.0.0.1"));
    }
}
