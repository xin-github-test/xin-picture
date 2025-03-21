package com.xin.xinpicturebackend.manager;



import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.xin.xinpicturebackend.config.CosClientConfig;
import com.xin.xinpicturebackend.constant.PictureConstant;
import com.xin.xinpicturebackend.constant.OSSImageProcessStyleConstants;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

/**
 * Cos 对象存储操作
 *
 * @author <a href="https://github.com/lixin">程序员xin</a>
 * @from <a href="https://xin.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * @param multipartFile 文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 图片信息
     */
    public UploadPictureResult uploadPictureResult (MultipartFile multipartFile, String uploadPathPrefix) {
        //校验图片
        validPicture(multipartFile);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        //尽量不要使用原始文件名称，防止名称中的字符和后续操作有冲突，增加安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));

        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try{
            file = File.createTempFile(uploadPath,null);
            multipartFile.transferTo(file);
            //TODO (putObjectResult)上传对象返回的结果是啥? 上传到oss
            cosManager.putObject(uploadPath, file);
            UploadPictureResult uploadPictureResult = cosManager.processImgByStyle(uploadPath, OSSImageProcessStyleConstants.IMAGE_INFO);
            //将基本信息封装到UploadPictureResult(已封装部分信息)
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            //返回可访问的地址
            return uploadPictureResult;
        }catch (Exception e) {
            log.error("图片上传到对象存储失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");

        } finally {
            this.deleteTempFile(file);
        }
    }

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

    /**
     * 图片校验
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        //1.校验文件大小
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > 2 * PictureConstant.ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
        //2.校验图片后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //允许上传的文件集合
        ThrowUtils.throwIf(!PictureConstant.ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误！");
    }
}
