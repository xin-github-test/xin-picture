package com.xin.xinpicturebackend.model.dto.picture;

import com.xin.xinpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {
    /**
     * 图片id
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
    /**
     * 图片大小
     */
    private Long picSize;
    /**
     * 图片宽度
     */
    private Integer picWidth;
    /**
     * 图片高度
     */
    private Integer picHeight;
    /**
     * 图片比例
     */
    private Double picScale;
    /**
     * 图片格式
     */
    private String picFormat;
    /**
     * 搜索词，同时搜名称，简介等
     */
    private String searchText;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 是否只查询spaceId为null的数据
     */
    private boolean nullSpaceId;
    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 开始编辑时间
     */
    private Date startEditTime;
    /**
     * 结束编辑时间
     */
    private Date endEditTime;
    private static final long serialVersionUID = 1L;
}
