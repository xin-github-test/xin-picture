package com.xin.xinpicturebackend.CrawStrategy;

import com.xin.xinpicturebackend.CrawStrategy.annotation.StrategyType;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加载使用了@StrategyType注解的类，并保存在map中
 */
public class StrategyFactory {
    private final static ConcurrentHashMap<String, Strategy> strategyMap = new ConcurrentHashMap<>();
    static {
        //通过SPI机制自动扫描 /META-INF/services/接口全类名 这个文件中配置的接口实现类的全类名并反射成对象
        ServiceLoader.load(Strategy.class).forEach(strategy -> {
            StrategyType annotation = strategy.getClass().getAnnotation(StrategyType.class);
            if (annotation != null) {
                strategyMap.put(annotation.value(), strategy);
            }
        });
    }

    /**
     * 根据注解中的类型自动获取map中的对象
     * @param strategyType  获取对象的类型
     * @return  某个对象
     */
    public static Strategy getStrategy(String strategyType) {
        Strategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown strategy type: " + strategyType);
        }

        return strategy;
    }
}
