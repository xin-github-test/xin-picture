package com.xin.xinpicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.xin.xinpicturebackend.constant.OSSImageProcessStyleConstants;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（step1）
 */
@Slf4j
public class GetImagePageUrlApi {
    //TODO 发送请求时需要的请求头（应该不是固定的，但是暂时没有时间逆向其算法）
    private static final String AcsToken = "1744691284152_1744726856431_qkiZWjwSocGud5yB4AT1poWQk16pkPlFY9OaU9X4VF+jd0korLlkOEZz95vyr2UQPCuWVLLQvHkv5lA6W4zF7Xf5C8+Gt6rrCy9ca2Ch5si0DIj6UIf3VeZ4gIznalv5SmOMrn3cTtojI39U0OlJg7yMN2mTvP3C1XNcJIxzxR1EsFzVMDckOEak0e2BQQZDQKci2INASMlw3oZSucYE1pez5ko8lJ9GmYcgFVNlkZTRQ1dEoMRPmiD2V0jRQjwZIIOn+55PJgEibInG4IoXagu6ZAGi25u+KsX1WzbKesTKgR3Dses3X/uFt4HC+Qt+Y4K+UZd1JExN+DzXdygCmlSMGMjRkXPgcugZwSbvwk1U2EQ0d03NRyqZ2l1jIqWI1vGPZYRZRTGpk7XL2NhJWXOJp7jCEz8U4oxkJU2rn5b/E4754V0p4HESV9WjxpMlV7sfgFafxQTk6C4CbvarEw==";
    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        //由于以图搜图只能使用png格式的，因此此处使用oss自带的格式转换
        formData.put("image", imageUrl+ OSSImageProcessStyleConstants.IMAGE_FORMAT_TO_PNG);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        //https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=11584179066904145355&sign=12673505deede35d37f1a01744725328&tk=c8c25&tn=pc&tpl_from=pc
        try {
            // 2. 发送 POST 请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .header("Acs-Token", AcsToken)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            // 判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            String responseBody = response.body();
            Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);

            // 3. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 对 URL 进行解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        String result = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果 URL：" + result);
    }
}

