package com.xin.xinpicturebackend.manager;



import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Cos 对象存储操作
 * @deprecated 已废弃，改为使用upload包的模板方法优化
 */
@Service
@Slf4j
@Deprecated
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
            //上传到oss
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
     * 通过url上传图片
     * @param fileUrl  文件地址
     * @param uploadPathPrefix 上传路径前缀
     * @return 文件信息
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        //校验url
        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR);
        validPicture(fileUrl);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = FileUtil.mainName(fileUrl);
        //拼接文件地址
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;

        try {
            file = File.createTempFile(uploadPath, null);
            //下载文件
            HttpUtil.downloadFile(fileUrl, file);
            //上传到oss
            cosManager.putObject(uploadPath, file);
            UploadPictureResult uploadPictureResult = cosManager.processImgByStyle(uploadPath, OSSImageProcessStyleConstants.IMAGE_INFO);
            //将基本信息封装到UploadPictureResult(已封装部分信息)
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            //返回可访问的地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }

    }

    /**
     * 根据url校验文件
     * @param fileUrl
     */
    private void validPicture(String fileUrl) {
        //校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR,"文件地址为空！");
        //校验url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            //格式有异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确！");
        }
        //校验url协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址！" );

        //发送 HEAD 请求验证文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            //文件存在，文件类型校验、文件大小校验
            String contentType = httpResponse.header("Content-Type");
            //不为空才校验，为空就不校验，走正常流程（允许为空）
            if (StrUtil.isNotBlank(contentType)) {
            //允许的图片类型
                ThrowUtils.throwIf(!PictureConstant.ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),ErrorCode.PARAMS_ERROR,"文件类型错误！");
            }
            //校验图片大小
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    ThrowUtils.throwIf(contentLength > 2*PictureConstant.ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
                }catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
                }
                            }
        } finally {
            //释放资源
            if (httpResponse != null) {
                httpResponse.close();
            }
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
