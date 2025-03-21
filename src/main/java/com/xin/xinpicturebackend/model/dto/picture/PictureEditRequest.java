package com.xin.xinpicturebackend.model.dto.picture;

import lombok.Data;

import java.util.List;

/**
 * 图片编辑请求
 */
@Data
public class PictureEditRequest {
    /**
     * id
     */
    private Long id;
    /**
     * 图片名称
     */
    private String name;
    /**
     * 图片简介
     */
    private String introduction;
    /**
     * 图片分类
     */
    private String category;
    /**
     * 图片标签
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
