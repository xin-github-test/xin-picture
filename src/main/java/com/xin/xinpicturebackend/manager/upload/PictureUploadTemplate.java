package com.xin.xinpicturebackend.manager.upload;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import com.xin.xinpicturebackend.config.CosClientConfig;
import com.xin.xinpicturebackend.constant.OSSImageProcessStyleConstants;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.manager.CosManager;
import com.xin.xinpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * @param inputSource 文件源
     * @param uploadPathPrefix 上传路径前缀
     * @return 图片信息
     */
    public UploadPictureResult uploadPictureResult (Object inputSource, String uploadPathPrefix) {
        //校验图片
        validPicture(inputSource);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);

        //尽量不要使用原始文件名称，防止名称中的字符和后续操作有冲突，增加安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));

        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try{
            file = File.createTempFile(uploadPath,null);
            //处理文件来源
            processFile(inputSource, file);
            //上传到oss
            cosManager.putObject(uploadPath, file);
            return buildResult(originalFilename, uploadPath);
        }catch (Exception e) {
            log.error("图片上传到对象存储失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");

        } finally {
            this.deleteTempFile(file);
        }
    }

    private UploadPictureResult buildResult(String originalFilename, String uploadPath) {
        UploadPictureResult uploadPictureResult = cosManager.processImgByStyle(uploadPath, OSSImageProcessStyleConstants.IMAGE_INFO);
        //将基本信息封装到UploadPictureResult(已封装部分信息)
        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        //返回可访问的地址
        return uploadPictureResult;
    }

    /**
     * 校验输入源（本地文件或url）
     * @param inputSource 输入源
     */
    protected abstract void validPicture(Object inputSource);
    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);
    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;
    /**
     * 删除临时文件
     * @param file 文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        //记得删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filePath = {}", file.getAbsoluteFile());
        }
    }


}
