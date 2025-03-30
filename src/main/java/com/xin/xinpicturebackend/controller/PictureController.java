package com.xin.xinpicturebackend.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xin.xinpicturebackend.annotation.AuthCheck;
import com.xin.xinpicturebackend.common.BaseResponse;
import com.xin.xinpicturebackend.common.DeleteRequest;
import com.xin.xinpicturebackend.common.ResultUtils;
import com.xin.xinpicturebackend.constant.PictureConstant;
import com.xin.xinpicturebackend.constant.UserConstant;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.model.dto.picture.*;
import com.xin.xinpicturebackend.model.entity.Picture;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.enums.PictureReviewStatusEnum;
import com.xin.xinpicturebackend.model.vo.PictureTagCategory;
import com.xin.xinpicturebackend.model.vo.PictureVO;
import com.xin.xinpicturebackend.service.PictureService;
import com.xin.xinpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 本地缓存初始化
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L)  //最大1w条数据
            .expireAfterWrite(Duration.ofMinutes(5))  //缓存5分钟之后过期
            .build();
    /**
     * 上传图片(可重新上传)
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
    /**
     * 通过url上传图片(可重新上传)
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl  , pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
    /**
     * 更新图片（仅管理员，因为可修改的字段较多）
     * @param pictureUpdateRequest 当前图片
     * @param request 当前请求
     * @return 自定义返回
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将实体和DTO进行转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        //将list字段转化为String
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //数据校验
        pictureService.validPicture(picture);
        //判断是否存在
        Long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(null == oldPicture, ErrorCode.NOT_FOUND_ERROR);
        //补充审核字段
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        //操作数据库
        boolean res = pictureService.updateById(picture);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR,"图片更新失败！");
        return ResultUtils.success(true);

    }

    /**
     * 根据id查询图片信息（未脱敏版，仅管理员可用）
     * @param id 图片id
     * @param request 当前请求
     * @return 图片信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        if (ObjectUtil.isEmpty(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断是否存在
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(null == picture, ErrorCode.NOT_FOUND_ERROR, "图片不存在！");
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片信息（脱敏版）
     * @param id 图片id
     * @param request 请求
     * @return 图片信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //判断是否存在
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(null == picture, ErrorCode.NOT_FOUND_ERROR, "图片不存在！");
        //图片未过审
        if(!picture.getReviewStatus().equals(PictureReviewStatusEnum.PASS.getValue())) {
          throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"图片未过审！");
        }
        //返回脱敏后的数据
        return ResultUtils.success(PictureVO.objToVo(picture));
    }

    /**
     * 分页获取图片列表：未脱敏版（仅管理员）
     * @param pictureQueryRequest 查询请求
     * @param request 当前请求
     * @return 图片分页列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        //查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表：脱敏版
     * @param pictureQueryRequest 查询图片请求
     * @param request 当前请求
     * @return 脱敏后分页图片列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > PictureConstant.GET_IMG_LIMIT_NUM, ErrorCode.PARAMS_ERROR);

        //普通用户默认只能看到 审核通过 的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        //返回脱敏后的信息
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表：脱敏版使用缓存)
     * @param pictureQueryRequest 查询图片请求
     * @param request 当前请求
     * @return 脱敏后分页图片列表
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > PictureConstant.GET_IMG_LIMIT_NUM, ErrorCode.PARAMS_ERROR);

        //普通用户默认只能看到 审核通过 的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //==================redis缓存 start=======================================
        //查询数据库前，先查询缓存，若是缓存未命中再查数据库
        //TODO 可以抽取成一个manager
        //1.构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = String.format("xinpicture:listPictureVOByPage:%s",hashKey);
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String cachedValue = opsForValue.get(redisKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            //命中缓存
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            //直接返回缓存
            return ResultUtils.success(cachePage);
        }
        //==================redis缓存 end=======================================

        //查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        //获取脱敏后的信息
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        //==================redis缓存 start=======================================
        //将数据存入redis缓存并设置过期时间 5-10分钟
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //防止缓存雪崩，过期时间设置随机值
        int cacheExpireTime = 300 + RandomUtil.randomInt(0,300);
        opsForValue.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        //==================redis缓存 end=======================================

        return ResultUtils.success(pictureVOPage);
    }
    /**
     * 分页获取图片列表：脱敏版使用缓存)
     * @param pictureQueryRequest 查询图片请求
     * @param request 当前请求
     * @return 脱敏后分页图片列表
     */
    @PostMapping("/list/page/vo/caffe")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCaffeCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > PictureConstant.GET_IMG_LIMIT_NUM, ErrorCode.PARAMS_ERROR);

        //普通用户默认只能看到 审核通过 的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //==================caffeine缓存 start=======================================
        //查询数据库前，先查询缓存，若是缓存未命中再查数据库
        //TODO 可以抽取成一个manager
        //1.构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("listPictureVOByPage:%s",hashKey);
        //从本地缓存中获取
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            //命中缓存
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            //直接返回缓存
            return ResultUtils.success(cachePage);
        }
        //==================caffeine缓存 end=======================================

        //查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        //获取脱敏后的信息
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        //==================caffeine缓存 start=======================================
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //存入本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        //==================caffeine缓存 end=======================================

        return ResultUtils.success(pictureVOPage);
    }
    /**
     * 分页获取图片列表：脱敏版使用多级缓存)
     * @param pictureQueryRequest 查询图片请求
     * @param request 当前请求
     * @return 脱敏后分页图片列表
     */
    @PostMapping("/list/page/vo/multicache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithMultiCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > PictureConstant.GET_IMG_LIMIT_NUM, ErrorCode.PARAMS_ERROR);

        //普通用户默认只能看到 审核通过 的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //==================多级缓存 start=======================================
        //查询数据库前，先查询缓存，若是缓存未命中再查数据库
        //TODO 可以抽取成一个manager
        //1.构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("xinpicture:listPictureVOByPage:%s",hashKey);
        //1.先从本地缓存中获取
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            //命中缓存
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            //直接返回缓存
            return ResultUtils.success(cachePage);
        }
        //2.本地缓存未命中，继续从Redis中查询
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            //redis缓存命中，更新本地缓存
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            //直接返回缓存
            return ResultUtils.success(cachePage);
        }
        //==================多级缓存 end=======================================

        //3.本地缓存和redis均未命中，查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        //获取脱敏后的信息
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        //==================多级缓存 start=======================================
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //更新 Redis 缓存
        int redisCacheExpireTime = 300 + RandomUtil.randomInt(0,300);
        opsForValue.set(cacheKey, cacheValue, redisCacheExpireTime, TimeUnit.SECONDS);
        //更新 本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        //==================多级caffeine缓存 end=======================================

        return ResultUtils.success(pictureVOPage);
    }
    /**
     * 编辑图片（仅本人和管理员）
     * @param pictureEditRequest 新图片的信息
     * @param request 当前请求
     * @return 是否编辑成功
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //参数校验
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        //将list转化为String
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //补充字段
        //编辑时间
        picture.setEditTime(new Date());
        pictureService.validPicture(picture);
        //获取当前登陆用户（仅管理员和本人能编辑）
        User loginUser = userService.getLoginUser(request);
        //补充审核字段
        pictureService.fillReviewParams(picture, loginUser);
        //判断是否存在
        Long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //权限校验(仅本人和管理员能修改)
        if (!loginUser.getId().equals(oldPicture.getUserId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //操作数据库
        boolean res = pictureService.updateById(picture);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR,"图片更新失败！");
        return ResultUtils.success(true);
    }
    /**
     * 删除图片（仅本人和管理员）
     * @param deleteRequest 删除的图片信息
     * @param request 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误异常！");
        }
        //权限校验（由于用户和管理员都能删除图片，因此需要更加细粒度的控制权限）
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //权限校验
        //只有本人和管理员能删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //操作数据库
        boolean res = pictureService.removeById(id);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "图片删除失败！");
        return ResultUtils.success(true);
    }

    /**
     * 获取图片默认tags和目录（前期使用）
     * @return 默认tags和category
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        pictureTagCategory.setTagList(PictureConstant.DEFAULT_IMG_TAG_LIST);
        pictureTagCategory.setCategoryList(PictureConstant.DEFAULT_IMG_CATEGORY_LIST);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     * @param pictureReviewRequest 待审核的信息
     * @param request 当前请求
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }
}
