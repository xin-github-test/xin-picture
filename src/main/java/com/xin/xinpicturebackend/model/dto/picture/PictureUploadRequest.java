package com.xin.xinpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
@Data
public class PictureUploadRequest implements Serializable {
    /**
     * 图片id
     */
    private Long id;
    /**
     * 文件url地址
     */
    private String fileUrl;
    /**
     * 图片名称
     */
    private String picName;
    /**
     * 图片标签
     */
    private String tags;
    /**
     * 图片分类
     */
    private String category;
    /**
     * 空间id,默认为0 公共空间
     */
    private Long spaceId = 0L;
    private static final long serialVersionUID = 1L;
}
