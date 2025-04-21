package com.xin.xinpicturebackend.api.aliyunai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.xin.xinpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.xin.xinpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.xin.xinpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    //读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    //创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    //查询任务状态地址
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     * @param createOutPaintingTaskRequest 创建任务的请求
     * @return 返回任务创建的结果
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR, "扩图参数为空");

        //发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer" + apiKey)
//                一定要开启异步（阿里云只支持异步访问）
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

        //处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常:{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败！");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            //只有当请求异常时，code才会有返回值，正常是不会返回code的
            if (createOutPaintingTaskResponse.getCode() != null) {
                String errorMessage = createOutPaintingTaskResponse.getMessage();
                log.error("请求异常:{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败！" + errorMessage);
            }

            return createOutPaintingTaskResponse;
        }
    }

    /**
     * 根据任务ID获取外部绘画任务的详细信息
     *
     * @param taskId 绘画任务的唯一标识符
     * @return 返回一个包含绘画任务详细信息的响应对象
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR, "任务ID为空");

        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        //发送请求并处理响应
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer" + apiKey)
                .header("Content-Type", "application/json")
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常:{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败！");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }

    }
}
