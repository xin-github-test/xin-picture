package com.xin.xinpicturebackend.constant;

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
}
