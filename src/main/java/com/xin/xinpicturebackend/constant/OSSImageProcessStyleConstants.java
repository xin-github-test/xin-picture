package com.xin.xinpicturebackend.constant;

/**
 * 阿里对象存储图像处理参数
 */
public interface OSSImageProcessStyleConstants {
    //获取图片信息
    String IMAGE_INFO = "image/info";
    //将图片格式转换成webp
    String IMAGE_FORMAT_TO_WEBP = "?x-oss-process=style/format_change";
}
