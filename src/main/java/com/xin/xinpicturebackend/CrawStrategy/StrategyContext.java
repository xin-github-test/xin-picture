package com.xin.xinpicturebackend.CrawStrategy;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 策略调用环境类
 */
@Component
public class StrategyContext {

    private final Strategy strategy;

    //构造注入
    public StrategyContext(@Value("${craw.strategy:bing}") String strategyType) {
        this.strategy = StrategyFactory.getStrategy(strategyType);
    }
    /**
     * 调用具体的策略
     */
    public List<String> crawing(String searchText, Integer count) {
        return strategy.crawing(searchText, count);
    }
}
