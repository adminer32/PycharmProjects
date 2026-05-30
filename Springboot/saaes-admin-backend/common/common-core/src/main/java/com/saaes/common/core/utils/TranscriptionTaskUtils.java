package com.saaes.common.core.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.DefaultProfile;
import com.saaes.common.core.web.MyException;

/**
 * @desc 音视频文件离线转写任务工具类
 */
public class TranscriptionTaskUtils {

    private static final String REGION = "cn-beijing";  // 区域
    private static final String appKey = "CU2MUSqKsrtKvavt";
    private static final String ACCESS_KEY_ID = "LTAI5t9vx4Xd4Vk9jnFqVVEG";  // Access Key ID
    private static final String ACCESS_KEY_SECRET = "UL8EaIJstUYikH94hwlaSoQ7hOanUJ";  // Access Key Secret

    /**
     * 提交离线转写任务
     *
     * @param audioUrl    音频文件的URL
     * @param parameters  离线任务的参数
     * @return 任务ID
     * @throws ClientException 请求异常
     */
    public static String submitOfflineTranscriptionTask(String audioUrl, JSONObject parameters) throws ClientException {
        // 创建API请求
        CommonRequest request = createCommonRequest("tingwu.cn-beijing.aliyuncs.com", "2023-09-30", ProtocolType.HTTPS, MethodType.PUT, "/openapi/tingwu/v2/tasks");
        request.putQueryParameter("type", "offline");

        // 构建请求体
        JSONObject root = new JSONObject();
        root.put("AppKey", appKey);

        JSONObject input = new JSONObject();
        input.fluentPut("FileUrl", audioUrl)
                .fluentPut("SourceLanguage", "cn")
                .fluentPut("TaskKey", "task" + System.currentTimeMillis());
        root.put("Input", input);

        // 设置其他参数
        root.put("Parameters", parameters);

        // 设置请求内容
        request.setHttpContent(root.toJSONString().getBytes(), "utf-8", FormatType.JSON);

        // 配置API客户端
        DefaultProfile profile = DefaultProfile.getProfile(REGION, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
        IAcsClient client = new DefaultAcsClient(profile);

        // 发送请求并获取响应
        CommonResponse response = client.getCommonResponse(request);
        JSONObject body = JSONObject.parseObject(response.getData());
        JSONObject data = body.getJSONObject("Data");
        return data.getString("TaskId");
    }

    public static String getTaskResult (String taskId) throws ClientException {
        String queryUrl = String.format("/openapi/tingwu/v2/tasks" + "/%s", taskId);

        CommonRequest request = createCommonRequest("tingwu.cn-beijing.aliyuncs.com", "2023-09-30", ProtocolType.HTTPS, MethodType.GET, queryUrl);
        // TODO 请通过环境变量设置您的AccessKeyId、AccessKeySecret
        DefaultProfile profile = DefaultProfile.getProfile("cn-beijing", ACCESS_KEY_ID, ACCESS_KEY_SECRET);
        IAcsClient client = new DefaultAcsClient(profile);
        CommonResponse response = client.getCommonResponse(request);

        try {
            JSONObject rootJson = JSONObject.parseObject(response.getData());
            JSONObject dataJson = rootJson.getJSONObject("Data");
            JSONObject resultJson = dataJson.getJSONObject("Result");
            String status = dataJson.get("TaskStatus").toString();
            if(status.equals("success")){
                System.out.println(1);
            }

        } catch (Exception e){
            throw new MyException("任务识别失败，请重新提交Ai笔记任务" + e.getMessage());
        }

        System.out.println(response.getData());
        return response.getData();
    }

    /**
     * 构建请求参数
     *
     * @return 参数
     */
    public static JSONObject buildRequestParameters() {
        JSONObject parameters = new JSONObject();

        // 音视频转换：可选
//        JSONObject transcoding = new JSONObject();
//        transcoding.put("TargetAudioFormat", "mp3");
//        transcoding.put("SpectrumEnabled", false);
//        parameters.put("Transcoding", transcoding);

        // 语音识别：可选
//        JSONObject transcription = new JSONObject();
//        transcription.put("DiarizationEnabled", true);
//        JSONObject speakerCount = new JSONObject();
//        speakerCount.put("SpeakerCount", 2);
//        transcription.put("Diarization", speakerCount);
//        parameters.put("Transcription", transcription);

        // 翻译：可选
//        JSONObject translation = new JSONObject();
//        JSONArray langArry = new JSONArray();
//        langArry.add("en");
//        translation.put("TargetLanguages", langArry);
//        parameters.put("Translation", translation);
//        parameters.put("TranslationEnabled", true);

        // 章节速览：可选
        parameters.put("AutoChaptersEnabled", true);

        // 智能纪要：可选
        parameters.put("MeetingAssistanceEnabled", true);
        JSONObject meetingAssistance = new JSONObject();
        JSONArray mTypes = new JSONArray().fluentAdd("Actions").fluentAdd("KeyInformation");
        meetingAssistance.put("Types", mTypes);
        parameters.put("MeetingAssistance", meetingAssistance);

        // 摘要相关：可选
        parameters.put("SummarizationEnabled", true);
        JSONObject summarization = new JSONObject();
        JSONArray sTypes = new JSONArray().fluentAdd("Paragraph").fluentAdd("Conversational").fluentAdd("QuestionsAnswering").fluentAdd("MindMap");
        summarization.put("Types", sTypes);
        parameters.put("Summarization", summarization);

        // PPT抽取：可选
//        parameters.put("PptExtractionEnabled", true);

        // 口语书面化：可选
//        parameters.put("TextPolishEnabled", true);

        return parameters;
    }

    /**
     * 创建API请求
     *
     * @param domain  域名
     * @param version 版本
     * @param protocolType 协议类型
     * @param method 请求方法
     * @param uri URI路径
     * @return CommonRequest
     */
    private static CommonRequest createCommonRequest(String domain, String version, ProtocolType protocolType, MethodType method, String uri) {
        CommonRequest request = new CommonRequest();
        request.setSysDomain(domain);
        request.setSysVersion(version);
        request.setSysProtocol(protocolType);
        request.setSysMethod(method);
        request.setSysUriPattern(uri);
        request.setHttpContentType(FormatType.JSON);
        return request;
    }
}

