package websocket

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"saaes-backend/internal/model"
	"saaes-backend/internal/router"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{CheckOrigin: func(r *http.Request) bool { return true }}

func HandleAIChat(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
		if err != nil {
			return
		}
		defer conn.Close()
		ai := newAIClient(deps)

		for {
			_, message, err := conn.ReadMessage()
			if err != nil {
				return
			}
			var msg map[string]interface{}
			if err := json.Unmarshal(message, &msg); err != nil {
				continue
			}

			switch msg["type"] {
			case "init":
				taskID, _ := msg["task_id"].(string)
				var task model.AITranscriptionTask
				deps.DB.Where("task_id = ?", taskID).First(&task)
				conn.WriteJSON(map[string]interface{}{
					"type":        "init",
					"status":      "success",
					"has_context": task.Transcription != "",
				})

			case "chat":
				content, _ := msg["content"].(string)
				taskID, _ := msg["task_id"].(string)
				reply, err := ai.chat(c.Request.Context(), content, taskID)
				status := "success"
				if err != nil {
					reply, status = "AI 服务暂时不可用", "error"
				}
				conn.WriteJSON(map[string]interface{}{
					"type":   "chat",
					"content": reply,
					"status": status,
				})
			}
		}
	}
}

func HandleTeacherNotification(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
		if err != nil {
			return
		}
		defer conn.Close()

		ticker := time.NewTicker(30 * time.Second)
		defer ticker.Stop()

		go func() {
			for range ticker.C {
				conn.WriteJSON(map[string]interface{}{"type": "pong"})
			}
		}()
		for {
			if _, _, err := conn.ReadMessage(); err != nil {
				break
			}
		}
	}
}

type aiClient struct {
	deps *router.Deps
}

func newAIClient(deps *router.Deps) *aiClient { return &aiClient{deps: deps} }

func (a *aiClient) chat(ctx context.Context, userMsg, taskID string) (string, error) {
	msgs := []map[string]string{{"role": "system", "content": `你是一位专业的毽球运动教练，名字叫"翎析"。你的职责是：根据用户的动作分析数据提供专业的指导建议，用通俗易懂的语言解释动作要领，给出具体的改进方法，鼓励用户坚持练习。回复要简洁有力，不啰嗦。`}}

	if taskID != "" {
		var task model.AITranscriptionTask
		if err := a.deps.DB.Where("task_id = ?", taskID).First(&task).Error; err == nil && task.Transcription != "" {
			msgs = append(msgs, map[string]string{
				"role":    "system",
				"content": "【视频转录内容】\n" + task.Transcription + "\n\n请基于以上视频转录内容回答用户的问题。",
			})
		}
	}
	msgs = append(msgs, map[string]string{"role": "user", "content": userMsg})

	body, _ := json.Marshal(map[string]interface{}{
		"model":    "deepseek-chat",
		"messages": msgs,
		"stream":   false,
	})

	req, _ := http.NewRequestWithContext(ctx, "POST",
		a.deps.Config.DeepSeek.BaseURL+"/chat/completions", bytes.NewBuffer(body))
	req.Header.Set("Authorization", "Bearer "+a.deps.Config.DeepSeek.APIKey)
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var result map[string]interface{}
	json.NewDecoder(resp.Body).Decode(&result)

	if choices, ok := result["choices"].([]any); ok && len(choices) > 0 {
		if msg, ok := choices[0].(map[string]any); ok {
			content, _ := msg["message"].(map[string]any)["content"].(string)
			return content, nil
		}
	}
	return "", nil
}