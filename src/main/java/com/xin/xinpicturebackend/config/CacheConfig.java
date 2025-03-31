package com.xin.xinpicturebackend.config;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 自定义缓存配置类
 */
@Configuration
@ConfigurationProperties(prefix = "xin.picture")
@Data
public class CacheConfig {
    /**
     * 是否开启redis缓存
     */
    private boolean enableRedisCache;
    /**
     * 是否开启本地缓存
     */
    private boolean enableCaffeineCache;
    /**
     * 是否开启多级缓存（redis + caffeine）
     */
    private boolean enableMultiCache;

    @PostConstruct
    private void init() {
        if (ObjectUtil.isEmpty(enableCaffeineCache)) {
            enableCaffeineCache = enableRedisCache && enableCaffeineCache;
        }
    }
}
