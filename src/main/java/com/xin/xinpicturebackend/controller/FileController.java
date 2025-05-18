package com.xin.xinpicturebackend.controller;

import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.OSSObject;
import com.xin.xinpicturebackend.annotation.AuthCheck;
import com.xin.xinpicturebackend.common.BaseResponse;
import com.xin.xinpicturebackend.common.ResultUtils;
import com.xin.xinpicturebackend.constant.UserConstant;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     * @param multipartFile
     * @return
     */
    @RequestMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //文件目录
        String fileName = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);
        File file = null;
        try{
            file = File.createTempFile(fileName,null);
            multipartFile.transferTo(file);
            //上传到oss
            cosManager.putObject(filePath, file);
            //返回可访问的地址
            return ResultUtils.success(filePath);
        }catch (Exception e) {
            log.error("file upload error, filePath = {}",filePath,e);
            cosManager.deleteObject(filePath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");

        } finally {
            //记得删除临时文件
            if (file != null) {
                //删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filePath = {}", filePath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     * @param filePath
     * @param response
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RequestMapping("/test/download")
    public void testDownloadFile(String filePath, HttpServletResponse response) throws IOException {
        InputStream ossObjectInput = null;
        try {
            OSSObject ossObject = cosManager.getObject(filePath);
            ossObjectInput = ossObject.getObjectContent();
            byte[] bytes = IOUtils.readStreamAsByteArray(ossObjectInput);
            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename="+ filePath);
            //写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filePath = {}",filePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");

        } finally {
            //释放流
            if (null != ossObjectInput){
                ossObjectInput.close();
            }
        }

    }
}
