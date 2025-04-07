package com.xin.xinpicturebackend.CrawStrategy;

import java.util.List;

/**
 *抽象策略层
 */
public interface Strategy {
    /**
     * 抓取图片
     * @param searchText 搜索词
     * @param count 抓取数量
     */
    /**
     * 抓取图片
     * @param searchText 搜索词
     * @param count 抓取数量
     * @return 返回图片的url
     */
    List<String> crawing(String searchText, Integer count);
}
