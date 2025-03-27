package com.xin.xinpicturebackend.model.dto.picture;

import com.xin.xinpicturebackend.constant.PictureConstant;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量导入图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
    /**
     * 搜索词
     */
    private String searchText = "动漫高清壁纸";

    /**
     * 抓取数量
     */
    private Integer count = PictureConstant.CRAWLING_IMG_COUNT;

    /**
     * 设置批量抓取的图片名称前缀
     */
    private String namePrefix = "xin";
    /**
     * 设置批量导入图片的标签
     */
    private List<String> tags;
    /**
     * 设置批量导入图片的分类
     */
    private String category;

    private static final long serialVersionUID = 1L;
}
