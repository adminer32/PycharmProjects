package schedule

import (
	"saaes-backend/internal/model"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

func QuerySchedules(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		date := c.Query("date")
		if date == "" {
			date = time.Now().Format("2006-01")
		}
		userID, _ := c.Get("user_id")
		var schedules []model.Schedule
		deps.DB.Where("student_id = ? AND DATE_FORMAT(schedule_date, '%Y-%m') = ?", userID, date).
			Order("schedule_date ASC, time ASC").Find(&schedules)
		result := make(map[string][]map[string]interface{})
		for _, s := range schedules {
			key := s.ScheduleDate.Format("2006-01-02")
			result[key] = append(result[key], map[string]interface{}{
				"id": s.ID, "schedule_date": key, "time": s.Time, "type": s.Type, "content": s.Content,
			})
		}
		response.Success(c, result)
	}
}

func SaveSchedule(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			ID           uint   `json:"id"`
			Time         string `json:"time" binding:"required"`
			Type         string `json:"type"`
			Content      string `json:"content"`
			ScheduleDate string `json:"scheduleDate" binding:"required"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少必填参数")
			return
		}
		userID, _ := c.Get("user_id")
		scheduleDate, _ := time.Parse("2006-01-02", req.ScheduleDate)

		if req.ID != 0 {
			deps.DB.Model(&model.Schedule{}).Where("id = ?", req.ID).Updates(map[string]interface{}{
				"time": req.Time, "type": req.Type, "content": req.Content, "schedule_date": scheduleDate,
			})
		} else {
			schedule := model.Schedule{
				StudentID: userID.(uint), ScheduleDate: scheduleDate,
				Time: req.Time, Type: ifBlank(req.Type, "warning"), Content: req.Content,
			}
			deps.DB.Create(&schedule)
			req.ID = schedule.ID
		}
		response.Success(c, map[string]interface{}{
			"id": req.ID, "schedule_date": req.ScheduleDate,
			"time": req.Time, "type": req.Type, "content": req.Content,
		})
	}
}

func DeleteSchedules(deps *router.Deps) gin.HandlerFunc {
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
		deps.DB.Delete(&model.Schedule{}, "id IN ?", idList)
		response.Success(c, nil)
	}
}

func ifBlank(s, alt string) string {
	if s == "" {
		return alt
	}
	return s
}