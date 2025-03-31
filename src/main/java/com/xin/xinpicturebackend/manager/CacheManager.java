package com.xin.xinpicturebackend.manager;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xin.xinpicturebackend.config.CacheConfig;
import com.xin.xinpicturebackend.constant.ProjectConstant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 *需求：自由控制是否开启redis缓存， 本地 caffeine缓存 以及是否同时开启，使用多级缓存
 * 目前只支持 key 和 value 均为 String 类型
 */
@Component
public class CacheManager {
    @Resource
    StringRedisTemplate stringRedisTemplate;

    /**
     * 本地缓存初始化
     */
    @Resource
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L)  //最大1w条数据
            .expireAfterWrite(Duration.ofMinutes(5))  //缓存5分钟之后过期
            .build();

    //缓存配置
    @Resource
    private CacheConfig cacheConfig;

    //依次获取缓存
    public String getCache(String key) {
        //未开启缓存,直接返回null
        if ((!cacheConfig.isEnableRedisCache()) && (!cacheConfig.isEnableCaffeineCache())) {
            return null;
        }

        //开启多级缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        if (cacheConfig.isEnableMultiCache()) {
            //1.首先查询本地缓存
            String cached = LOCAL_CACHE.getIfPresent(key);
            if (cached != null) {
                //命中直接返回即可
                return cached;
            }
            //2.本地缓存未命中，再查询redis缓存
            cached = opsForValue.get(key);
            if (cached != null) {
                //命中后，更新本地缓存,再返回
                LOCAL_CACHE.put(key, cached);
                return cached;
            }
            //都未命中，返回空，查询数据库
            return null;
        }
        //只开启 redis 和 caffeine 其中一种缓存
        return cacheConfig.isEnableCaffeineCache() ? LOCAL_CACHE.getIfPresent(key) : opsForValue.get(key);
    }

    //更新缓存
    public void updateCache(String key, String value) {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        //更新 Redis 缓存,并设置随机过期时间（固定 5 分钟 + 随机（0-5分钟）），尽量避免缓存雪崩
        int redisCacheExpireTime = 300 + RandomUtil.randomInt(0,300);
        opsForValue.set(key, value, redisCacheExpireTime, TimeUnit.SECONDS);
        //更新 本地缓存
        LOCAL_CACHE.put(key, value);
    }

    /**
     * 根据key删除缓存
     * 注意：数据库中的与缓存相关的数据发生变动时，都需要删除并更新缓存
     * 为保证数据库数据更新完毕时，缓存与数据的一致性，采用：延迟双删策略,即：
     * 1）调用 deleteCache 删除缓存
     * 2）更新数据库
     * 3）time.sleep()
     * 4）调用 deleteCache 再次删除缓存
     * @param key
     */
    public void deleteCache(String key) {
        stringRedisTemplate.delete(key);
        LOCAL_CACHE.invalidate(key);
    }

    //从redis中获取数据
    private String getCacheByRedis(String key) {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String cachedValue = opsForValue.get(key);
        return cachedValue;
    }

    //从本地缓存中获取数据
    private String getCacheByCaffeine(String key) {
        String cachedValue = LOCAL_CACHE.getIfPresent(key);
        return cachedValue;
    }

    /**
     * 构建缓存key
     * @param operationRequest  接受参数的请求
     * @param methodName  当前方法的名称
     * @return  缓存key
     */
    public String getKey(Object operationRequest, String methodName) {
        //构建缓存key
        String queryCondition = JSONUtil.toJsonStr(operationRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format(ProjectConstant.PROJECT_NAME + ":" + methodName + ":%s",hashKey);
        return cacheKey;
    }
}
