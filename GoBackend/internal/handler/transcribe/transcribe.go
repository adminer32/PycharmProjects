package transcribe

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"saaes-backend/internal/model"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"time"

	"github.com/gin-gonic/gin"
)

func TranscribeVideo(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			VideoURL string `json:"video_url" binding:"required"`
			TaskID   string `json:"task_id"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 video_url")
			return
		}
		transcription := "【自动转录内容】视频内容正在转写中..."
		if req.TaskID != "" {
			deps.DB.Model(&model.AITranscriptionTask{}).Where("task_id = ?", req.TaskID).
				Update("transcription", transcription)
		}
		response.Success(c, gin.H{"transcription": transcription, "task_id": req.TaskID})
	}
}

func AnalyzeTranscription(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Transcription string `json:"transcription" binding:"required"`
			TaskID        string `json:"task_id"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 transcription")
			return
		}
		systemPrompt := `你是一个专业的视频内容分析助手。请根据视频转录内容：
1. 生成自动章节（JSON数组，每项包含 start, end, title）
2. 生成内容摘要（200字以内）
3. 生成关键要点列表

请以 JSON 格式返回：{"chapters": [...], "summary": "...", "keywords": [...]}`

		apiKey := deps.Config.DeepSeek.APIKey
		body, _ := json.Marshal(map[string]interface{}{
			"model": "deepseek-chat",
			"messages": []map[string]string{
				{"role": "system", "content": systemPrompt},
				{"role": "user", "content": req.Transcription},
			},
		})
		ctx, cancel := context.WithTimeout(c.Request.Context(), 60*time.Second)
		defer cancel()

		httpReq, _ := http.NewRequestWithContext(ctx, "POST",
			deps.Config.DeepSeek.BaseURL+"/chat/completions", bytes.NewBuffer(body))
		httpReq.Header.Set("Authorization", "Bearer "+apiKey)
		httpReq.Header.Set("Content-Type", "application/json")

		resp, err := http.DefaultClient.Do(httpReq)
		if err != nil {
			response.Error(c, "分析失败")
			return
		}
		defer resp.Body.Close()

		var result map[string]interface{}
		json.NewDecoder(resp.Body).Decode(&result)
		content := ""
		if choices, ok := result["choices"].([]any); ok && len(choices) > 0 {
			if msg, ok := choices[0].(map[string]any); ok {
				content, _ = msg["message"].(map[string]any)["content"].(string)
			}
		}
		if req.TaskID != "" {
			deps.DB.Model(&model.AITranscriptionTask{}).Where("task_id = ?", req.TaskID).
				Updates(map[string]interface{}{"auto_chapters": content, "task_status": "已完成"})
		}
		response.Success(c, gin.H{"analysis": content})
	}
}

func FullProcessVideo(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			TaskID   string `json:"task_id"`
			VideoURL string `json:"video_url" binding:"required"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 video_url")
			return
		}
		taskID := req.TaskID
		if taskID == "" {
			taskID = fmt.Sprintf("task_%s", time.Now().Format("20060102150405"))
		}
		deps.DB.Model(&model.AITranscriptionTask{}).Where("task_id = ?", taskID).
			Updates(map[string]interface{}{"task_status": "处理中", "video_url": req.VideoURL})
		response.Success(c, gin.H{"task_id": taskID, "status": "processing", "message": "视频正在处理中"})
	}
}

func HealthCheck(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "ok"})
	}
}