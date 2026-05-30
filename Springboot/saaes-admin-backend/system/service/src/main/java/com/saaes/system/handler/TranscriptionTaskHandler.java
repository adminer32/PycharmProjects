package com.saaes.system.handler;

import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.aliyuncs.exceptions.ClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.utils.QwenClient;
import com.saaes.common.core.utils.TranscriptionTaskUtils;
import com.saaes.common.core.web.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Component
@Transactional
public class TranscriptionTaskHandler extends BaseServiceImpl {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    private final Map<String, ScheduledFuture<?>> taskQueue = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(3); // 限制 AI 处理线程池大小


    public TranscriptionTaskHandler() {
        taskScheduler.initialize();
    }

    private CompletableFuture<GenerationResult> asyncCallQwen(QwenClient qwenClient, String prompt, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return qwenClient.callQwenMaxLatest(prompt, content);
            } catch (NoApiKeyException | InputRequiredException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * 添加任务到队列并定期检查状态
     */
    public void addTaskToQueue(String taskId) {
        if (taskQueue.containsKey(taskId)) {
            log.warn("任务 {} 已在队列中，跳过重复添加", taskId);
            return;
        }

        ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndProcessTask(taskId);
            } catch (Exception e) {
                log.error("查询任务 {} 失败", taskId, e);
            }
        }, 4000); // **每 4 秒检查一次**

        taskQueue.put(taskId, scheduledTask);
    }

    /**
     * 查询任务状态
     */
    private void checkAndProcessTask(String taskId) {
        String resultJson;
        try {
            resultJson = TranscriptionTaskUtils.getTaskResult(taskId);
            if (resultJson == null || resultJson.isEmpty()) return;
        } catch (ClientException e) {
            log.error("获取任务 {} 状态失败", taskId, e);
            return;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(resultJson);
            String taskStatus = jsonNode.get("Data").get("TaskStatus").asText();

            if ("COMPLETED".equals(taskStatus)) {
                processCompletedTask(jsonNode, taskId);
            } else if ("FAILED".equals(taskStatus)) {
                processFailedTask(jsonNode, taskId);
            }
        } catch (Exception e) {
            log.error("解析任务 {} 结果失败", taskId, e);
        }
    }

    /**
     * 处理已完成任务，存入数据库
     */
    @Transactional
    public void processCompletedTask(JsonNode jsonNode, String taskId) {
        log.info("任务 {} 完成，开始解析 JSON 并调用 AI 优化", taskId);

        QwenClient qwenClient = new QwenClient();
        JsonNode resultNode = jsonNode.get("Data").get("Result");

        // 获取各字段 URL
        String meetingAssistanceUrl = resultNode.get("MeetingAssistance").asText("");
        String autoChaptersUrl = resultNode.get("AutoChapters").asText("");
        String summarizationUrl = resultNode.get("Summarization").asText("");

        // 请求原始 JSON 内容
        String meetingAssistanceJson = fetchJson(meetingAssistanceUrl);
        String autoChaptersJson = fetchJson(autoChaptersUrl);
        String summarizationJson = fetchJson(summarizationUrl);

        try {
            // 提取对应字段
            String meetingAssistanceContent = extractField(meetingAssistanceJson, "MeetingAssistance");
            String autoChaptersContent = extractField(autoChaptersJson, "AutoChapters");
            String summarizationContent = extractField(summarizationJson, "Summarization");

            boolean contentValid = StringUtils.hasText(meetingAssistanceContent)
                    && StringUtils.hasText(autoChaptersContent)
                    && StringUtils.hasText(summarizationContent)
                    && !"{\"Keywords\":[]}".equals(meetingAssistanceContent);

            if (contentValid) {

                // 提示词优化
                final String prompt = "以下是通义听悟视频转写API返回的JSON内容，与毽球运动相关，请优化其中语义不通或不自然的表达，仅修改文本内容，输出必须是原始数据结构，保持结构不变，不添加markdown语法或任何解释说明。";
                final String optimizeHint = "请保持原始数据结构，仅优化语义不通或有语病的文本部分，不添加markdown语法或注释。";

                // 异步调用 Qwen AI
                CompletableFuture<GenerationResult> futureMeeting = asyncCallQwen(qwenClient, prompt, meetingAssistanceContent + optimizeHint)
                        .exceptionally(ex -> {
                            log.error("任务 {} 的 MeetingAssistance 优化失败", taskId, ex);
                            return null;
                        });

                CompletableFuture<GenerationResult> futureChapters = asyncCallQwen(qwenClient, prompt, autoChaptersContent + optimizeHint)
                        .exceptionally(ex -> {
                            log.error("任务 {} 的 AutoChapters 优化失败", taskId, ex);
                            return null;
                        });

//            CompletableFuture<GenerationResult> futureSummary = asyncCallQwen(qwenClient, prompt, summarizationContent + optimizeHint)
//                    .exceptionally(ex -> {
//                        log.error("任务 {} 的 Summarization 优化失败", taskId, ex);
//                        return null;
//                    });

//            CompletableFuture.allOf(futureMeeting, futureChapters, futureSummary).join();
                CompletableFuture.allOf(futureMeeting, futureChapters).join();

                // 提取 AI 优化后的 JSON 字符串
                String aiMeetingAssistanceJson = Optional.ofNullable(futureMeeting.get())
                        .map(r -> {
                            try {
                                return CommonUtil.extractQwenContent(JsonUtils.toJson(r));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .orElse("{}");

                String aiAutoChaptersJson = Optional.ofNullable(futureChapters.get())
                        .map(r -> {
                            try {
                                return CommonUtil.extractQwenContent(JsonUtils.toJson(r));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .orElse("{}");

//            String aiSummarizationJson = Optional.ofNullable(futureSummary.get())
//                    .map(r -> {
//                        try {
//                            return CommonUtil.extractQwenContent(JsonUtils.toJson(r));
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    })
//                    .orElse("{}");

                // 存入数据库
                String sql = "update ai_note_info set auto_chapters = (:auto_chapters), summarization = (:summarization), meeting_assistance = (:meeting_assistance), task_status = (:task_status) where task_id = (:task_id)";
                Map<String, Object> paramMap = Map.of(
                        "auto_chapters", compressAndEscape(aiAutoChaptersJson),
//                    "summarization", compressAndEscape(aiSummarizationJson),
                        "summarization", compressAndEscape(summarizationContent),
                        "meeting_assistance", compressAndEscape(aiMeetingAssistanceJson),
                        "task_status", "已完成",
                        "task_id", taskId
                );

                int count = primaryNPJdbcTemplate.update(sql, paramMap);
                removeTaskFromQueue(taskId);
                if (count <= 0) {
                    throw new MyException("更新任务：" + taskId + "数据时失败");
                }

            } else {
                // 存入数据库
                String sql = "update ai_note_info set task_status = (:task_status) where task_id = (:task_id)";
                Map<String, Object> paramMap = Map.of(
                        "task_status", "已完成",
                        "task_id", taskId
                );

                int count = primaryNPJdbcTemplate.update(sql, paramMap);
                removeTaskFromQueue(taskId);
                if (count <= 0) {
                    throw new MyException("更新任务：" + taskId + "数据时失败");
                }
            }

            log.info("任务 {} 已完成并成功入库", taskId);

        } catch (Exception e) {
            log.error("任务 {} 在处理 JSON 或调用 AI 过程中发生异常", taskId, e);
            String failSql = "update ai_note_info set task_status = (:task_status) where task_id = (:task_id)";
            Map<String, Object> failMap = Map.of("task_status", "失败", "task_id", taskId);
            primaryNPJdbcTemplate.update(failSql, failMap);
        }
    }


    /**
     * 使用 Hutool 请求 JSON 内容
     */
    private String fetchJson(String url) {
        try {
            return HttpUtil.get(url);
        } catch (Exception e) {
            log.error("请求 URL 失败: {}", url, e);
            return "{}"; // 返回空 JSON，避免 null 异常
        }
    }

    /**
     * 提取 JSON 节点中的特定字段
     */
    private String extractField(String json, String fieldName) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.has(fieldName) ? node.get(fieldName).toString() : "";
    }

    /**
     * 压缩并转义 JSON 内容
     */
    private String compressAndEscape(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // 使用 fastjson2 压缩 JSON 并转义特殊字符
        return JSON.toJSONString(content,
                JSONWriter.Feature.BrowserCompatible,
                JSONWriter.Feature.WriteNonStringKeyAsString,
                JSONWriter.Feature.WriteMapNullValue,
                JSONWriter.Feature.WriteNullStringAsEmpty,
                JSONWriter.Feature.WriteNullNumberAsZero,
                JSONWriter.Feature.WriteNullBooleanAsFalse,
                JSONWriter.Feature.WriteNullListAsEmpty);
    }

    /**
     * 处理失败任务
     */
    private void processFailedTask(JsonNode jsonNode, String taskId) {
        log.error("任务 {} 失败: {}", taskId, jsonNode.get("Data").get("ErrorMessage").asText());
        String failSql = "update ai_note_info set task_status = (:task_status) where task_id = (:task_id)";
        Map<String, Object> failMap = Map.of("task_status", "失败", "task_id", taskId);
        primaryNPJdbcTemplate.update(failSql, failMap);
        removeTaskFromQueue(taskId);
    }

    /**
     * 移除队列中的任务
     */
    private void removeTaskFromQueue(String taskId) {
        ScheduledFuture<?> scheduledTask = taskQueue.remove(taskId);
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            log.info("任务 {} 已从队列中移除", taskId);
        }
    }

}
