package content

import (
	"encoding/json"
	"saaes-backend/internal/model"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"strconv"

	"github.com/gin-gonic/gin"
)

// POST /api/analysis/save
func SaveAnalysis(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			StudentID    uint        `json:"student_id" binding:"required"`
			AnalysisData interface{} `json:"analysis_data"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "参数不完整")
			return
		}
		data, _ := json.Marshal(req.AnalysisData)
		record := model.MotionAnalysisData{StudentID: req.StudentID, AnalysisData: string(data)}
		if err := deps.DB.Create(&record).Error; err != nil {
			response.Error(c, "保存失败: "+err.Error())
			return
		}
		response.SuccessWithMsg(c, "分析数据保存成功", gin.H{"id": record.ID})
	}
}

// GET /api/analysis/history
func GetAnalysisHistory(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		studentID := parseUint(c.Query("student_id"))
		if studentID == 0 {
			response.BadRequest(c, "缺少 student_id")
			return
		}
		var records []model.MotionAnalysisData
		if err := deps.DB.Where("student_id = ?", studentID).
			Order("created_at DESC").Limit(50).Find(&records).Error; err != nil {
			response.Error(c, err.Error())
			return
		}
		result := make([]map[string]interface{}, 0, len(records))
		for _, r := range records {
			var data interface{}
			json.Unmarshal([]byte(r.AnalysisData), &data)
			result = append(result, map[string]interface{}{
				"id": r.ID, "student_id": r.StudentID,
				"analysis_data": data,
				"created_at":    r.CreatedAt.Format("2006-01-02 15:04:05"),
			})
		}
		response.Success(c, gin.H{"records": result})
	}
}

// GET /api/analysis/queryByTaskId
func QueryByTaskId(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		taskID := c.Query("taskId")
		if taskID == "" {
			response.BadRequest(c, "缺少 taskId")
			return
		}
		var result model.AIAnalysisResult
		if err := deps.DB.Where("task_id = ?", taskID).First(&result).Error; err != nil {
			response.Success(c, gin.H{})
			return
		}
		response.Success(c, result)
	}
}

// GET /api/analysis/generateTrainingPlan
func GenerateTrainingPlan(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		response.Success(c, gin.H{"items": []map[string]string{
			{"schedule_date": "2026-06-01", "time": "09:00", "type": "warning", "content": "盘踢训练 30分钟"},
			{"schedule_date": "2026-06-02", "time": "09:00", "type": "warning", "content": "磕踢训练 30分钟"},
		}})
	}
}

// GET /api/content/queryCategory
func QueryCategory(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.JSON(200, response.Result{Status: "success", Data: []map[string]interface{}{
			{"name": "盘踢", "id": 1}, {"name": "磕踢", "id": 2},
			{"name": "拐踢", "id": 3}, {"name": "绷踢", "id": 4},
		}})
	}
}

// POST /api/content/insert
func InsertContent(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		aiContentInfo := c.PostForm("aiContentInfo")
		file, _ := c.FormFile("file")
		motionName := c.PostForm("motionName")

		var parsed map[string]interface{}
		json.Unmarshal([]byte(aiContentInfo), &parsed)

		content := model.AIContentInfo{Type: "card", Sender: "user"}
		if taskID, ok := parsed["task_id"].(string); ok {
			content.TaskID = taskID
		}
		if text, ok := parsed["text"].(string); ok {
			content.Text = text
		}
		deps.DB.Create(&content)
		c.JSON(200, response.Result{Status: "success", Data: gin.H{
			"analysis_id": content.ID,
			"file_uploaded": file != nil,
			"motion_name":  motionName,
		}})
	}
}

func parseUint(s string) uint {
	v, _ := strconv.ParseUint(s, 10, 64)
	return uint(v)
}