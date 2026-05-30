package com.saaes.system.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.saaes.common.core.web.MyException;
import com.saaes.system.service.AIAnalysisInfoService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class AIAnalysisHandler {

    // 可根据需求调整线程数
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public void submit(AIAnalysisInfoService analysisInfoService, String taskId) {
        executorService.submit(() -> {
            try {
                analysisInfoService.addAIAnalysisTask(taskId);
            } catch (Exception e) {
                try {
                    analysisInfoService.save(Map.of("task_id", taskId, "task_status", "失败"));
                } catch (JsonProcessingException ex) {
                    throw new MyException("AI分析失败,请重新分析");
                }
                System.err.println("AI分析任务失败，taskId=" + taskId + "，原因：" + e.getMessage());
            }
        });
    }
}

