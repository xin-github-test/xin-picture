package com.xin.xinpicturebackend.manager;


import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.*;

import com.xin.xinpicturebackend.config.CosClientConfig;
import com.xin.xinpicturebackend.constant.OSSImageProcessStyleConstants;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.model.dto.file.UploadPictureResult;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cos 对象存储操作
 *
 * @author <a href="https://github.com/lixin">程序员xin</a>
 * @from <a href="https://xin.icu">编程导航知识星球</a>
 */
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private OSS cosClient;

    /**
     * 上传对象
     *
     * @param key           唯一键
     * @param localFilePath 本地文件路径
     * @return
     */
    public PutObjectResult putObject(String key, String localFilePath) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                new File(localFilePath));
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 文件的下载
     * @param key
     * @return
     */
    public OSSObject getObject(String key){
        return cosClient.getObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 下载对象到本地文件
     * @param key
     * @param localFilePath
     */
    public void download(String key, String localFilePath) {
        cosClient.getObject(new GetObjectRequest(cosClientConfig.getBucket(), key), new File(localFilePath));
    }

    /**
     * 删除对象
     * @param key
     */
    public void deleteObject(String key) {
        // 5. 删除文件
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 批量删除文件
     * @param keyList
     * @return
     */
    public DeleteObjectsResult deleteObjects(List<String> keyList){
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(cosClientConfig.getBucket()).withKeys(keyList).withEncodingType("url");
        return cosClient.deleteObjects(deleteObjectsRequest);
    }

    /**
     * 删除指定目录
     * @param delPrefix
     * @return
     */
    public void deleteDir(String delPrefix) throws OSSException, ClientException {
        String nextMarker = null;
        ObjectListing objectListing;
        do {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(cosClientConfig.getBucket())
                    .withPrefix(delPrefix)
                    .withMarker(nextMarker);

            objectListing = cosClient.listObjects(listObjectsRequest);
            if (objectListing.getObjectSummaries().size() > 0) {
                List<String> keys = new ArrayList<>();
                for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                    System.out.println("key name: " + s.getKey());
                    keys.add(s.getKey());
                }
                DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(cosClientConfig.getBucket()).withKeys(keys).withEncodingType("url");
                cosClient.deleteObjects(deleteObjectsRequest);
            }

            nextMarker = objectListing.getNextMarker();
        } while (objectListing.isTruncated());
    }

    /**
     * 通过style对图像进行处理
     * @param url oss对象url
     * @param style 处理的操作
     * @return json
     */
    public UploadPictureResult processImgByStyle(String url, String style) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), url);
        getObjectRequest.setProcess(style);
        OSSObject object = cosClient.getObject(getObjectRequest);
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        try {
            String resJson = IOUtils.readStreamAsString(object.getObjectContent(), "UTF-8");
            Map map = JSONUtil.toBean(resJson, Map.class);
            //将图片基本信息进行封装
            String fileSize = (String) ((Map) map.get("FileSize")).get("value");
            String format = (String) ((Map) map.get("Format")).get("value");
            String imageHeight = (String) ((Map) map.get("ImageHeight")).get("value");
            String imageWidth = (String) ((Map) map.get("ImageWidth")).get("value");
            uploadPictureResult.setPicSize(Long.parseLong(fileSize));
            Integer width = Integer.parseInt(imageWidth);
            Integer height = Integer.parseInt(imageHeight);
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(NumberUtil.round(width * 1.0 / height,
                    2).doubleValue());
            uploadPictureResult.setPicFormat(format);
            //获取图片主色调
            Map<String, Object> res = getResByStyleUsingPost(cosClientConfig.getHost() + url, OSSImageProcessStyleConstants.GET_IMAGE_AVE);
            uploadPictureResult.setPicColor((String) res.get("RGB"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取图像信息失败!");
        }
        return uploadPictureResult;
    }

    public Map<String,Object> getResByStyleUsingPost(String imgUrl, String style){
        ThrowUtils.throwIf(imgUrl == null || style == null, ErrorCode.OPERATION_ERROR);
        HttpResponse response = HttpRequest.get(imgUrl + style)
                .timeout(5000)
                .execute();
        // 判断响应状态
        if (HttpStatus.HTTP_OK != response.getStatus()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        }
        // 解析响应
        String responseBody = response.body();
        return JSONUtil.toBean(responseBody, Map.class);
    }
}
