package com.xin.xinpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.xin.xinpicturebackend.constant.PictureConstant;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * URL 图片上传
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate{
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
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
                    ThrowUtils.throwIf(contentLength > 5*PictureConstant.ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
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

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception{
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl,file);
    }
}
