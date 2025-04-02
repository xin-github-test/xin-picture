package com.xin.xinpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.xinpicturebackend.model.dto.picture.*;
import com.xin.xinpicturebackend.model.entity.Picture;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 新
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-03-19 23:18:19
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param inputSource  输入源
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 将图片对象转成VO(对图片信息进行脱敏)
     * @param picture 图片信息
     * @param request 当前请求
     * @return 脱敏后的图像信息
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);
    /**
     * 将图片对象转成VO(对信息进行脱敏)（分页数据）
     * @param picturePage picture分页数据
     * @param request 请求
     * @return 图片对象VO分页数据
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 将图片查询对象转化成queryWrapper对象
     * @param pictureQueryRequest 图片查询对象
     * @return   queryWrapper对象
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
    /**
     * 对图片参数进行校验
     * @param picture 图片对象
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 待审核的信息
     * @param loginUser 当前用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     *  填充审核参数
     * @param picture 待填充的图片
     * @param loginUser 当前用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     * @param pictureUploadByBatchRequest 抓取图片请求
     * @param loginUser 当前登陆用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清理图片文件
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 删除图片
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 校验空间图片的权限
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);
}
