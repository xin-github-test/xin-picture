package com.xin.xinpicturebackend.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface PictureConstant {
    /**
     * 限制图片上传大小基本单位（M）
     */
    long ONE_M = 1024 * 1024;

    /**
     * 允许上传的图片类型
     */
    List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
    /**
     * 图片简介长度限制
     */
    int INTRODUCTION_LEN = 800;

    /**
     * 图片url长度限制
     */
    int IMG_URL_LEN = 1024;
    /**
     * 限制普通用户一次性获取图片的数量
     */
    int GET_IMG_LIMIT_NUM = 20;

    /**
     * 图片默认标签（前期使用）
     */
    List<String> DEFAULT_IMG_TAG_LIST = Arrays.asList("热门","搞笑","生活","高清","艺术","校园","背景","简历","创意");
    /**
     * 图片默认目录（前期使用）
     */
    List<String> DEFAULT_IMG_CATEGORY_LIST = Arrays.asList("模板","电商","表情包","素材","海报");
    /**
     * 允许的图片类型
     */
    List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg","image/jpg","image/png","image/webp");

    /**
     * 批量抓取图片数量,默认10条（不要太高）
     */
    Integer CRAWLING_IMG_COUNT = 10;

    /**
     * 抓取图片的网址（使用bing搜索的图片）
     */
    List<String> CRAWLING_BASE_URL_LIST = new ArrayList<>(Arrays.asList("https://cn.bing.com/images/async", "https://en.freejpg.com.ar/"));
}
