package video

import (
	"saaes-backend/internal/model"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
)

func GetProgress(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		userID := parseUintS(c.Query("user_id"))
		if userID == 0 {
			response.BadRequest(c, "缺少 user_id")
			return
		}
		var progress []model.VideoProgress
		deps.DB.Where("user_id = ?", userID).Find(&progress)
		result := make(map[string]map[string]interface{})
		for _, p := range progress {
			result[p.VideoPath] = map[string]interface{}{
				"progress": p.Progress, "updateTime": p.UpdatedAt.Format("2006-01-02 15:04:05"),
			}
		}
		response.Success(c, result)
	}
}

func SaveProgress(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			UserID    uint   `json:"user_id" binding:"required"`
			VideoPath string `json:"video_path" binding:"required"`
			Progress  int    `json:"progress"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "参数不完整")
			return
		}
		var existing model.VideoProgress
		if err := deps.DB.Where("user_id = ? AND video_path = ?", req.UserID, req.VideoPath).First(&existing).Error; err == nil {
			deps.DB.Model(&existing).Updates(map[string]interface{}{"progress": req.Progress})
		} else {
			deps.DB.Create(&model.VideoProgress{UserID: req.UserID, VideoPath: req.VideoPath, Progress: req.Progress})
		}
		response.SuccessWithMsg(c, "进度保存成功", nil)
	}
}

func SaveHistory(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			ID      uint   `json:"id"`
			Content string `json:"content" binding:"required"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 content")
			return
		}
		userID, _ := c.Get("user_id")
		historyID := req.ID
		if historyID == 0 {
			record := model.VideoHistory{StudentID: userID.(uint), Content: req.Content}
			deps.DB.Create(&record)
			historyID = record.ID
		} else {
			deps.DB.Model(&model.VideoHistory{}).Where("id = ?", historyID).Update("content", req.Content)
		}
		response.Success(c, gin.H{"history_id": historyID})
	}
}

func QueryHistory(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			HistoryID uint `json:"historyId"`
		}
		c.ShouldBindJSON(&req)
		query := deps.DB.Model(&model.VideoHistory{})
		if req.HistoryID != 0 {
			query = query.Where("history_id = ?", req.HistoryID)
		}
		var histories []model.VideoHistory
		query.Order("created_at DESC").Find(&histories)
		response.Success(c, histories)
	}
}

func GetHistoryDetail(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		historyID := c.Query("historyId")
		if historyID == "" {
			response.BadRequest(c, "缺少 historyId")
			return
		}
		var contents []model.AIContentInfo
		deps.DB.Where("history_id = ?", historyID).Order("created_at ASC").Find(&contents)
		var result []map[string]interface{}
		for _, item := range contents {
			result = append(result, map[string]interface{}{
				"id": item.ID, "type": item.Type, "text": item.Text,
				"card": item.Card, "sender": item.Sender,
				"time": item.CreatedAt.Format("15:04"),
			})
		}
		response.Success(c, gin.H{"contentList": result})
	}
}

func DeleteHistory(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		ids := c.Query("ids")
		if ids == "" {
			response.BadRequest(c, "缺少 ids")
			return
		}
		var idList []uint
		for _, id := range strings.Split(ids, ",") {
			if v, err := strconv.ParseUint(strings.TrimSpace(id), 10, 64); err == nil {
				idList = append(idList, uint(v))
			}
		}
		deps.DB.Delete(&model.VideoHistory{}, "id IN ?", idList)
		response.SuccessWithMsg(c, "删除成功", nil)
	}
}

func parseUintS(s string) uint {
	v, _ := strconv.ParseUint(s, 10, 64)
	return uint(v)
}