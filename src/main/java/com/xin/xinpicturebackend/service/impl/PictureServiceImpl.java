package com.xin.xinpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.xinpicturebackend.CrawStrategy.StrategyContext;
import com.xin.xinpicturebackend.constant.PictureConstant;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.manager.CosManager;
import com.xin.xinpicturebackend.manager.upload.FilePictureUpload;
import com.xin.xinpicturebackend.manager.upload.PictureUploadTemplate;
import com.xin.xinpicturebackend.manager.upload.UrlPictureUpload;
import com.xin.xinpicturebackend.mapper.PictureMapper;
import com.xin.xinpicturebackend.model.dto.file.UploadPictureResult;
import com.xin.xinpicturebackend.model.dto.picture.*;
import com.xin.xinpicturebackend.model.entity.Picture;
import com.xin.xinpicturebackend.model.entity.Space;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.enums.PictureReviewStatusEnum;
import com.xin.xinpicturebackend.model.vo.PictureVO;
import com.xin.xinpicturebackend.model.vo.UserVO;
import com.xin.xinpicturebackend.service.PictureService;
import com.xin.xinpicturebackend.service.SpaceService;
import com.xin.xinpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
* @author 新
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-03-19 23:18:19
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService {
    @Resource
    UserService userService;
    @Resource
    FilePictureUpload filePictureUpload;
    @Resource
    UrlPictureUpload urlPictureUpload;
    @Resource
    CosManager cosManager;
    @Resource
    SpaceService spaceService;
    @Resource
    TransactionTemplate transactionTemplate;
    @Resource
    StrategyContext strategyContext;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
        //校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在！");
            //校验是否有权限，仅空间管理员能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限！");
            }
            //校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "可上传的图片数量不足！");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量不足！");
            }

        }
        //2.判断是新增还是更新
        Long pictureId = pictureUploadRequest.getId();
        //若是更新,判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在!");
            //仅本人和管理员可编辑图片
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            //此时再校验，更新时的spaceId和数据库中老数据的spaceId是否一致
            if (spaceId != null && !oldPicture.getSpaceId().equals(spaceId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新时的空间ID有误！");
            }
            spaceId = oldPicture.getSpaceId();
        }

        //3.上传图片,得到图片信息
        String uploadPathPrefix;

        if (spaceId == null) {
            //按照用户id划分目录,同时 public 代表可以公共访问的图片
            uploadPathPrefix = String.format("public/%s",loginUser.getId());
        } else {
            //按照空间划分目录
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        //根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPictureResult(inputSource, uploadPathPrefix);
        //4.操作数据库
        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        //支持自定义名称
        String picName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            //如果传入了自定义名称，使用自定义名称
            picName = pictureUploadRequest.getPicName();
        }
        String tags = pictureUploadRequest.getTags();
        String category = pictureUploadRequest.getCategory();
        if (StrUtil.isNotBlank(tags)) {
            //设置标签(如果传了的话)
            picture.setTags(tags);
        }
        if (StrUtil.isNotBlank(category)) {
            //设置分类(如果传了的话)
            picture.setCategory(category);
        }
        picture.setName(picName);
        picture.setSpaceId(spaceId);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        //补充审核参数
        this.fillReviewParams(picture,loginUser);
        //如果pictureId不是空,则为更新,否则为新增
        if (pictureId != null) {
            //更新还需要补充额外信息 id  和  编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            //新增或者更新 (根据是否有id自动确定是新增还是更新)
            boolean res = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "图片上传失败,数据库操作失败!");
            //更新空间使用额度 （私有空间才更新额度）
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败！");
            }
                        return picture;
        });

        return PictureVO.objToVo(picture);
    }

    /**
     * 将图片对象转成VO(对信息进行脱敏)（单条数据）
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //关联查询用户信息
        Long userId = picture.getUserId();
        if (null != userId && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 将图片对象转成VO(对信息进行脱敏)（分页数据）
     * @param picturePage picture分页数据
     * @param request 请求
     * @return 图片对象VO分页数据
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //对象列表 => 对象VO列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //1.关联查询用户信息
        //收集picturList中所涉及的所有userId并去重
        Set<Long> userIdSet= pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        //获取 userId:User 的 Map
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));

        //2.为pictureVO填充user信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 将查询请求转化成查询对象
     * @param pictureQueryRequest 图片查询对象
     * @return 查询对象
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        //从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        //补充开始编辑和结束时间的查询
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        //补充审核条件，普通用户只能看到审核通过后的图片
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        //封装queryWrapper
        //从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
                    .or()
                    .like("category", searchText)
                    .or()
                    .like("tags", "\"" + searchText + "\"")
                    );
        }
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id",id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        //补充审核条件
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);

        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picScale), "picScale", picScale);
        //补充审核条件
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        //补充通过编辑时间进行范围查询
        queryWrapper.ge(ObjectUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjectUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        //Json数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        //排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 对图片参数进行校验
     * @param picture 图片对象
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //修改数据时， id不能为空， 有参数则校验
        ThrowUtils.throwIf(ObjectUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空！");
        //若是url不为空，则校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > PictureConstant.IMG_URL_LEN, ErrorCode.PARAMS_ERROR, "url过长！");
        }
        //若是简介不为空，则校验
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > PictureConstant.INTRODUCTION_LEN, ErrorCode.PARAMS_ERROR, "图片简介过长!");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(null == pictureReviewRequest, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(null == oldPicture, ErrorCode.NOT_FOUND_ERROR);
        //3.校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            //审核状态是否重复，重复则抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请勿重复审核！");
        }
        //4.数据库操作
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        //设置默认值
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean res = this.updateById(updatePicture);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
    }
    /**
     *  填充审核参数
     * @param picture 待填充的图片
     * @param loginUser 当前用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        //管理员自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }

    }

    /**
     * 批量抓取和保存图片
     * @param pictureUploadByBatchRequest 抓取图片请求
     * @param loginUser 当前登陆用户
     * @return 成功保存图片数量
     */
    @Deprecated
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        String tags = JSONUtil.toJsonStr(pictureUploadByBatchRequest.getTags());
        String category = pictureUploadByBatchRequest.getCategory();
        if (StrUtil.isBlank(namePrefix)) {
            //如果未自定义名称，设置默认值(搜索词)
            namePrefix = searchText;
        }
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多抓取30条！");
        //2.抓取内容
        String webAddr = PictureConstant.CRAWLING_BASE_URL_LIST.get(0);
        String fetchUrl = String.format(webAddr + "?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败,",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败！");
        }
        //3.解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjectUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取元素失败！");
        }
        //获取所有的图片元素
        Elements imgElementList = div.select("img.mimg");
        //遍历所有的图片元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                //获取失败，跳过，处理下一条
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            //处理图像地址，因为地址可能存在一些歧义字符（&）
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0,questionMarkIndex);
            }
            //4.上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            //设置默认名称
            pictureUploadRequest.setPicName(namePrefix + "-" + (uploadCount + 1));
            //设置标签和分类
            if (StrUtil.isNotBlank(tags)) {
                pictureUploadRequest.setTags(tags);
            }
            if (StrUtil.isNotBlank(category)) {
                pictureUploadRequest.setCategory(category);
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}",pictureVO.getId());
                uploadCount++;
            }catch (Exception e) {
                //上传失败，跳过，处理下一条
                log.error("图片上传失败，",e);
                continue;
            }
            //5.控制数量
            if (uploadCount >= count) {
                break;
            }

        }
        //4.返回成功上传的图片数量
        return uploadCount;
    }
    /**
     * 批量抓取和保存图片
     * @param pictureUploadByBatchRequest 抓取图片请求
     * @param loginUser 当前登陆用户
     * @return 成功保存图片数量
     */
    @Override
    public Integer uploadCrawingPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        String searchText = pictureUploadByBatchRequest.getSearchText();

        //2.限制爬虫
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多抓取30条！");

        List<String> picUrlList = strategyContext.crawing(searchText, count);

        //4.根据url批量上传图片并返回成功上传的图片的数量
        CompletableFuture<Integer> future = uploadPicsByUrl(picUrlList, pictureUploadByBatchRequest, loginUser);
        try {
            return future.get();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"图片异步上传失败！");
        }
    }
    /**
     * 清理图片
     * @param oldPicture 旧图片
     */
    @Async   //表示该方法异步执行，想要启用该注解，需要在项目启动类上@EnableAsync
    @Override
    public void clearPictureFile(Picture oldPicture) {
        //判断图片是否被多条记录引用
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl)
                .count();
        //有多个记录引用
        if (count > 1) {
            return;
        }
        cosManager.deleteObject(pictureUrl);
    }
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //权限校验
        checkPictureAuth(loginUser, oldPicture);

        if (oldPicture.getSpaceId() != null) {
            //私有空间,更新额度
            //开启事务
            transactionTemplate.execute(status -> {
                //操作数据库
                boolean res = this.removeById(pictureId);
                ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "图片删除失败！");
                //更新额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败！");
                return true;
            });
        } else {
            //公共图库，直接删除即可
            //操作数据库
            boolean res = this.removeById(pictureId);
            ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR, "图片删除失败！");
        }
    }
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser){
        //参数校验
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        //将list转化为String
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //补充字段
        //编辑时间
        picture.setEditTime(new Date());
        this.validPicture(picture);
        //判断是否存在
        Long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //权限校验(仅本人和管理员能修改)
        checkPictureAuth(loginUser, oldPicture);
        //补充审核字段
        fillReviewParams(picture, loginUser);
        //操作数据库
        boolean res = this.updateById(picture);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR,"图片更新失败！");
    }

    /**
     * 判断图片与用户之间权限关系
     * @param loginUser 当前登录的用户
     * @param picture 待验证权限的图片
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            //公共图库，仅本人和管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            //私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 根据url批量上传图片到oss（异步 + 流）
     *
     * @param picUrlList                  待上传的图片url列表
     * @param pictureUploadByBatchRequest 前端上传图片所携带的参数
     * @param loginUser                   当前登录的用户
     * @return 返回成功上传的图片的数量
     */
    public CompletableFuture<Integer> uploadPicsByUrl(List<String> picUrlList, PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            //如果未自定义名称，设置默认值(搜索词)
            namePrefix = pictureUploadByBatchRequest.getSearchText();
        }
        String tags = JSONUtil.toJsonStr(pictureUploadByBatchRequest.getTags());
        String category = pictureUploadByBatchRequest.getCategory();

        // 使用流来异步上传每个图片
        String finalNamePrefix = namePrefix;
        List<CompletableFuture<Void>> futures = picUrlList.stream()
                .map(fileUrl -> CompletableFuture.runAsync(() -> {
                   // 上传图片
                   PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
                   pictureUploadRequest.setFileUrl(fileUrl);
                   // 设置默认名称
                   pictureUploadRequest.setPicName(finalNamePrefix + "-" + (picUrlList.indexOf(fileUrl) + 1));
                   // 设置标签和分类
                   if (StrUtil.isNotBlank(tags)) {
                       pictureUploadRequest.setTags(tags);
                   }
                   if (StrUtil.isNotBlank(category)) {
                       pictureUploadRequest.setCategory(category);
                   }
                   try {
                       //上传图片
                       PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                       log.info("图片上传成功，id = {}", pictureVO.getId());
                   } catch (Exception e) {
                       // 上传失败，跳过
                       log.error("图片上传失败，", e);
                   }
               }))
                .collect(Collectors.toList());
        // 使用 allOf 等待所有的异步任务完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> (int) futures.stream().filter(CompletableFuture::isDone).count());
    }
}




