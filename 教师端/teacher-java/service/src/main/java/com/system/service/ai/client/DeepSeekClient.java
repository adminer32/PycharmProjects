package com.system.service.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.service.ai.config.DeepSeekConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekClient {

    @Autowired
    private DeepSeekConfig config;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeepSeekClient() {
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(120000);
        restTemplate.setRequestFactory(factory);
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String systemPrompt, String userMessage) {
        try {
            String url = config.getBaseUrl() + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(systemMessage);
            messages.add(userMsg);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("调用 DeepSeek API: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).path("message").path("content").asText();
                    log.info("DeepSeek API 调用成功，返回内容长度: {}", content.length());
                    return content;
                }
            }

            log.error("DeepSeek API 返回异常: {}", response.getBody());
            throw new RuntimeException("DeepSeek API 返回格式错误");

        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败", e);
            throw new RuntimeException("AI服务暂时不可用: " + e.getMessage());
        }
    }
}
