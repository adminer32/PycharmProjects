package ai

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"saaes-backend/internal/model"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

func Chat(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Message string `json:"message" binding:"required"`
			Model   string `json:"model"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 message")
			return
		}
		apiKey := deps.Config.DeepSeek.APIKey
		if apiKey == "" {
			response.Error(c, "DeepSeek API 密钥未配置")
			return
		}
		msgs := []map[string]string{
			{"role": "system", "content": "你是翎析，一位专业的毽球运动教练。"},
			{"role": "user", "content": req.Message},
		}
		body, _ := json.Marshal(map[string]interface{}{
			"model":    ifBlank(req.Model, "deepseek-chat"),
			"messages": msgs,
			"stream":   false,
		})
		ctx, cancel := context.WithTimeout(c.Request.Context(), 60*time.Second)
		defer cancel()
		req2, _ := http.NewRequestWithContext(ctx, "POST",
			deps.Config.DeepSeek.BaseURL+"/chat/completions",
			bytes.NewBuffer(body))
		req2.Header.Set("Authorization", "Bearer "+apiKey)
		req2.Header.Set("Content-Type", "application/json")
		resp, err := http.DefaultClient.Do(req2)
		if err != nil {
			response.Error(c, "AI 服务暂时不可用: "+err.Error())
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
		c.JSON(200, gin.H{"success": true, "message": content, "model": ifBlank(req.Model, "deepseek-chat")})
	}
}

func CoachChat(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Message       string `json:"message" binding:"required"`
			ActionContext *struct {
				ActionType      string  `json:"action_type"`
				HipScore        float64 `json:"hip_score"`
				KneeScore       float64 `json:"knee_score"`
				AnkleScore      float64 `json:"ankle_score"`
				FootHeightScore float64 `json:"foot_height_score"`
				OverallScore    float64 `json:"overall_score"`
				Feedback        string  `json:"feedback"`
			} `json:"action_context"`
			Model string `json:"model"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 message")
			return
		}
		systemPrompt := `你是一位专业的毽球运动教练，名字叫"翎析"。你的职责是：
1. 根据用户的动作分析数据提供专业的指导建议
2. 用通俗易懂的语言解释动作要领
3. 给出具体的改进方法
4. 鼓励用户坚持练习
5. 回复要简洁有力，不啰嗦
6. 如果用户没有提供动作数据，引导用户提供或描述自己的动作问题
你的回复应该专业、亲切、有耐心，像一位经验丰富的教练在面对面指导学生。`
		msgs := []map[string]string{{"role": "system", "content": systemPrompt}}
		if req.ActionContext != nil {
			ctx := fmt.Sprintf("【当前动作分析数据】\n动作类型: %s\n髋关节评分: %.1f\n膝关节评分: %.1f\n踝关节评分: %.1f\n抬脚高度评分: %.1f\n综合评分: %.1f\n%s",
				req.ActionContext.ActionType, req.ActionContext.HipScore, req.ActionContext.KneeScore,
				req.ActionContext.AnkleScore, req.ActionContext.FootHeightScore,
				req.ActionContext.OverallScore, req.ActionContext.Feedback)
			msgs = append(msgs, map[string]string{"role": "system", "content": ctx})
		}
		msgs = append(msgs, map[string]string{"role": "user", "content": req.Message})
		body, _ := json.Marshal(map[string]interface{}{
			"model": ifBlank(req.Model, "deepseek-chat"), "messages": msgs,
		})
		ctx2, cancel := context.WithTimeout(c.Request.Context(), 60*time.Second)
		defer cancel()
		req2, _ := http.NewRequestWithContext(ctx2, "POST",
			deps.Config.DeepSeek.BaseURL+"/chat/completions",
			bytes.NewBuffer(body))
		req2.Header.Set("Authorization", "Bearer "+deps.Config.DeepSeek.APIKey)
		req2.Header.Set("Content-Type", "application/json")
		resp, err := http.DefaultClient.Do(req2)
		if err != nil {
			response.Error(c, "AI 服务暂时不可用")
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
		c.JSON(200, gin.H{"success": true, "message": content, "model": ifBlank(req.Model, "deepseek-chat")})
	}
}

func ListModels(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.JSON(200, gin.H{"success": true, "models": []string{"deepseek-chat", "deepseek-coder"}})
	}
}

func HealthCheck(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.JSON(200, gin.H{"success": true, "status": "ok"})
	}
}

func QueryPersonalNotes(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Param struct {
				Title      string `json:"title"`
				ID         uint   `json:"id"`
				TaskStatus string `json:"task_status"`
				TaskID     string `json:"task_id"`
			} `json:"param"`
		}
		c.ShouldBindJSON(&req)
		query := deps.DB.Model(&model.AITranscriptionTask{}).Where("deleted = 0")
		if req.Param.Title != "" {
			query = query.Where("title LIKE ?", "%"+req.Param.Title+"%")
		}
		if req.Param.ID != 0 {
			query = query.Where("id = ?", req.Param.ID)
		}
		if req.Param.TaskStatus != "" {
			query = query.Where("task_status = ?", req.Param.TaskStatus)
		}
		if req.Param.TaskID != "" {
			query = query.Where("task_id = ?", req.Param.TaskID)
		}
		var tasks []model.AITranscriptionTask
		query.Order("create_time DESC").Find(&tasks)
		notes := make([]map[string]interface{}, len(tasks))
		for i, t := range tasks {
			notes[i] = map[string]interface{}{
				"id": t.ID, "taskId": t.TaskID,
				"createTime": t.CreatedAt.Format("2006-01-02 15:04:05"),
				"videoUrl": t.VideoURL, "title": t.Title,
				"autoChapters": t.AutoChapters, "summarization": t.Summarization,
				"meetingAssistance": t.MeetingAssist, "personalNote": t.PersonalNote,
				"taskStatus": t.TaskStatus,
			}
		}
		response.Success(c, gin.H{"list": notes})
	}
}

func AddTranscriptionTask(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Title    string `json:"title"`
			VideoURL string `json:"video_url"`
		}
		c.ShouldBindJSON(&req)
		taskID := fmt.Sprintf("task_%s", time.Now().Format("20060102150405"))
		task := model.AITranscriptionTask{
			TaskID: taskID, Title: ifBlank(req.Title, "未命名任务"),
			VideoURL: req.VideoURL, TaskStatus: "进行中",
		}
		deps.DB.Create(&task)
		c.JSON(200, response.Result{Status: "success", Data: map[string]interface{}{
			"id": task.ID, "taskId": task.TaskID,
			"createTime": task.CreatedAt.Format("2006-01-02 15:04:05"),
			"videoUrl": task.VideoURL, "title": task.Title,
			"autoChapters": "", "summarization": "",
			"meetingAssistance": "", "personalNote": "",
			"taskStatus": task.TaskStatus,
		}})
	}
}

func UpdateNote(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			ID           uint   `json:"id" binding:"required"`
			Title        string `json:"title"`
			PersonalNote string `json:"personal_note"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 id")
			return
		}
		updates := map[string]interface{}{}
		if req.Title != "" {
			updates["title"] = req.Title
		}
		if req.PersonalNote != "" {
			updates["personal_note"] = req.PersonalNote
		}
		if len(updates) == 0 {
			response.BadRequest(c, "无更新内容")
			return
		}
		result := deps.DB.Model(&model.AITranscriptionTask{}).Where("id = ?", req.ID).Updates(updates)
		if result.Error != nil || result.RowsAffected == 0 {
			response.Error(c, "更新失败")
			return
		}
		response.SuccessWithMsg(c, "更新成功", nil)
	}
}

func DeleteNotes(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		ids := c.Query("ids")
		if ids == "" {
			response.BadRequest(c, "缺少 ids")
			return
		}
		var idList []uint
		for _, id := range strings.Split(ids, ",") {
			var v uint
			fmt.Sscanf(strings.TrimSpace(id), "%d", &v)
			if v != 0 {
				idList = append(idList, v)
			}
		}
		deps.DB.Model(&model.AITranscriptionTask{}).Where("id IN ?", idList).Update("deleted", 1)
		response.SuccessWithMsg(c, "删除成功", nil)
	}
}

func QueryTaskStatus(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		taskID := c.Query("taskId")
		if taskID == "" {
			response.BadRequest(c, "缺少 taskId")
			return
		}
		var task model.AITranscriptionTask
		if err := deps.DB.Where("task_id = ?", taskID).First(&task).Error; err != nil {
			response.Success(c, nil)
			return
		}
		c.JSON(200, response.Result{Status: "success", Data: map[string]interface{}{
			"id": task.ID, "taskId": task.TaskID,
			"createTime": task.CreatedAt.Format("2006-01-02 15:04:05"),
			"videoUrl": task.VideoURL, "title": task.Title,
			"autoChapters": task.AutoChapters, "summarization": task.Summarization,
			"meetingAssistance": task.MeetingAssist, "personalNote": task.PersonalNote,
			"taskStatus": task.TaskStatus,
		}})
	}
}

func NotePolishing(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Message string `json:"message" binding:"required"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 message")
			return
		}
		systemPrompt := "你是一个专业的笔记润色助手，请将用户提供的笔记内容润色，使其更加清晰、简洁、有条理。"
		msgs := []map[string]string{
			{"role": "system", "content": systemPrompt},
			{"role": "user", "content": req.Message},
		}
		body, _ := json.Marshal(map[string]interface{}{"model": "deepseek-chat", "messages": msgs})
		ctx, cancel := context.WithTimeout(c.Request.Context(), 30*time.Second)
		defer cancel()
		req2, _ := http.NewRequestWithContext(ctx, "POST",
			deps.Config.DeepSeek.BaseURL+"/chat/completions",
			bytes.NewBuffer(body))
		req2.Header.Set("Authorization", "Bearer "+deps.Config.DeepSeek.APIKey)
		req2.Header.Set("Content-Type", "application/json")
		resp, err := http.DefaultClient.Do(req2)
		if err != nil {
			response.Error(c, "润色失败")
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
		response.Success(c, content)
	}
}

func ifBlank(s, alt string) string {
	if strings.TrimSpace(s) == "" {
		return alt
	}
	return s
}