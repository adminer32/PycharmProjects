package com.saaes.common.core.utils;

import java.util.Arrays;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.saaes.common.core.Constant;
import org.springframework.stereotype.Component;

@Component
public class QwenClient {

    private static final String apiKey = Constant.DASH_SCOPE_API_KEY;

    private static final String model = Constant.DASH_SCOPE_MODEL;

    /**
     * 调用Qwen模型并返回结果
     *
     * @param systemContent 系统消息内容
     * @param userContent 用户消息内容
     * @return GenerationResult 返回生成的结果
     * @throws ApiException API异常
     * @throws NoApiKeyException API密钥异常
     * @throws InputRequiredException 输入参数异常
     */
    public GenerationResult callQwenMaxLatest(String systemContent, String userContent) throws ApiException, NoApiKeyException, InputRequiredException {

        Generation gen = new Generation();

        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemContent)
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userContent)
                .build();

//        ResponseFormat jsonMode = ResponseFormat.builder().type("json_object").build();

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
//                .responseFormat(jsonMode) //返回Json数据模式
                .build();

        return gen.call(param);
    }

    public GenerationResult callQwenMaxLatest(String systemContent, String userContent, String customModel) throws ApiException, NoApiKeyException, InputRequiredException {

        Generation gen = new Generation();

        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemContent)
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userContent)
                .build();

//        ResponseFormat jsonMode = ResponseFormat.builder().type("json_object").build();

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(customModel == null ? Constant.DASH_SCOPE_MODEL : customModel)
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
//                .responseFormat(jsonMode) //返回Json数据模式
                .build();

        return gen.call(param);
    }
}