package student

import (
	"saaes-backend/internal/model"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"strconv"

	"github.com/gin-gonic/gin"
)

func GetLearningStats(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		studentID := parseUint(c.Param("studentId"))
		var stats struct {
			TotalHours   float64 `json:"total_hours"`
			CheckinDays  int     `json:"checkin_days"`
			ActionTypes  int     `json:"action_types"`
			AvgScore     float64 `json:"avg_score"`
			OverallLevel string  `json:"overall_level"`
		}
		deps.DB.Raw(`
			SELECT COALESCE(SUM(learning_hours), 0) as total_hours,
			       COUNT(DISTINCT DATE(checkin_date)) as checkin_days,
			       COUNT(DISTINCT action_type) as action_types,
			       COALESCE(AVG(score), 0) as avg_score
			FROM student_learning WHERE student_id = ?`, studentID).Scan(&stats)
		if stats.AvgScore >= 90 {
			stats.OverallLevel = "优秀"
		} else if stats.AvgScore >= 70 {
			stats.OverallLevel = "良好"
		} else if stats.AvgScore >= 60 {
			stats.OverallLevel = "及格"
		} else {
			stats.OverallLevel = "需加强"
		}
		response.Success(c, stats)
	}
}

func GetActionScores(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		studentID := parseUint(c.Param("studentId"))
		var scores []struct {
			ActionType string  `json:"action_type"`
			AvgScore   float64 `json:"avg_score"`
			Count      int     `json:"count"`
		}
		deps.DB.Raw(`SELECT action_type, AVG(score) as avg_score, COUNT(*) as count
			FROM action_scores WHERE student_id = ?
			GROUP BY action_type ORDER BY avg_score DESC`, studentID).Scan(&scores)
		response.Success(c, scores)
	}
}

func GetShuttlecockSkills(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		studentID := parseUint(c.Param("studentId"))
		var skills []model.ShuttlecockSkill
		deps.DB.Where("student_id = ?", studentID).Find(&skills)
		response.Success(c, skills)
	}
}

func GetWeeklyTrend(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		studentID := parseUint(c.Param("studentId"))
		var trend []struct {
			Week     string  `json:"week"`
			AvgScore float64 `json:"avg_score"`
			Hours    float64 `json:"hours"`
		}
		deps.DB.Raw(`SELECT DATE_FORMAT(checkin_date, '%Y-%u') as week,
			AVG(score) as avg_score, SUM(duration) as hours
			FROM student_checkin WHERE student_id = ?
			GROUP BY week ORDER BY week DESC LIMIT 8`, studentID).Scan(&trend)
		response.Success(c, trend)
	}
}

func GetCheckinRecords(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		studentID := parseUint(c.Param("studentId"))
		var records []model.StudentCheckin
		deps.DB.Where("student_id = ?", studentID).
			Order("checkin_date DESC").Limit(30).Find(&records)
		response.Success(c, records)
	}
}

func SavePracticeRecord(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			StudentID  uint   `json:"student_id" binding:"required"`
			ActionType string `json:"action_type" binding:"required"`
			VideoURL   string `json:"video_url"`
			ResultData string `json:"result_data"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "参数不完整")
			return
		}
		record := model.PracticeRecord{
			StudentID: req.StudentID, ActionType: req.ActionType,
			VideoURL: req.VideoURL, ResultData: req.ResultData,
		}
		deps.DB.Create(&record)
		c.JSON(200, gin.H{"success": true, "record_id": record.ID})
	}
}

func GetPracticeRecords(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		studentID := parseUint(c.Param("studentId"))
		var records []model.PracticeRecord
		deps.DB.Where("student_id = ?", studentID).Order("created_at DESC").Find(&records)
		response.Success(c, records)
	}
}

func GetPracticeRecord(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		recordID := parseUint(c.Param("recordId"))
		var record model.PracticeRecord
		if err := deps.DB.First(&record, recordID).Error; err != nil {
			response.Error(c, "记录不存在")
			return
		}
		response.Success(c, record)
	}
}

func DeletePracticeRecord(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		recordID := parseUint(c.Param("recordId"))
		deps.DB.Delete(&model.PracticeRecord{}, recordID)
		c.JSON(200, gin.H{"success": true})
	}
}

func GetPhysique(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		studentID := parseUint(c.Query("student_id"))
		if studentID == 0 {
			if id, exists := c.Get("user_id"); exists {
				studentID = id.(uint)
			}
		}
		var physique model.StudentPhysique
		if err := deps.DB.Where("student_id = ?", studentID).First(&physique).Error; err != nil {
			response.Success(c, nil)
			return
		}
		response.Success(c, physique)
	}
}

func CreatePhysique(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var physique model.StudentPhysique
		if err := c.ShouldBindJSON(&physique); err != nil {
			response.BadRequest(c, "参数错误")
			return
		}
		deps.DB.Create(&physique)
		response.SuccessWithMsg(c, "体质数据创建成功", gin.H{"id": physique.ID})
	}
}

func UpdatePhysique(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var physique map[string]interface{}
		if err := c.ShouldBindJSON(&physique); err != nil {
			response.BadRequest(c, "参数错误")
			return
		}
		studentID := parseUint(c.Query("student_id"))
		deps.DB.Model(&model.StudentPhysique{}).Where("student_id = ?", studentID).Updates(physique)
		response.SuccessWithMsg(c, "体质数据更新成功", nil)
	}
}

func UpdatePhysiqueData(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		UpdatePhysique(deps)(c)
	}
}

func GetProfile(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		userID, _ := c.Get("user_id")
		var profile model.StudentProfile
		if err := deps.DB.Where("user_id = ?", userID).First(&profile).Error; err != nil {
			response.Success(c, gin.H{})
			return
		}
		response.Success(c, profile)
	}
}

func CreateProfile(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var profile model.StudentProfile
		if err := c.ShouldBindJSON(&profile); err != nil {
			response.BadRequest(c, "参数错误")
			return
		}
		deps.DB.Create(&profile)
		response.Success(c, profile)
	}
}

func GetUserProfile(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		userID, _ := c.Get("user_id")
		var user model.User
		deps.DB.First(&user, userID)
		var profile model.StudentProfile
		deps.DB.Where("user_id = ?", userID).First(&profile)
		response.Success(c, gin.H{
			"userId": user.ID, "avatar": user.Avatar, "name": profile.RealName,
			"username": user.Username, "code": profile.StudentID,
			"telephone": user.Telephone, "email": user.Email,
		})
	}
}

func GetMotionNameAnalysis(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		motionName := c.Query("motionName")
		var result struct {
			AvgFluencyScore      float64 `json:"avgFluencyScore"`
			AvgOverallScore      float64 `json:"avgOverallScore"`
			AvgProficiencyScore float64 `json:"avgProficiencyScore"`
			AvgStabilityScore    float64 `json:"avgStabilityScore"`
		}
		deps.DB.Raw(`SELECT AVG(fluency_score), AVG(overall_score),
			AVG(proficiency_score), AVG(stability_score)
			FROM ai_analysis_result
			WHERE JSON_EXTRACT(analysis_data, '$.action') = ?`, motionName).Scan(&result)
		response.Success(c, result)
	}
}

func GetHistoricalTrends(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		type Trend struct {
			AnalysisDate         string  `json:"analysis_date"`
			AvgOverallScore     float64 `json:"avgOverallScore"`
			AvgStabilityScore   float64 `json:"avgStabilityScore"`
			AvgFluencyScore     float64 `json:"avgFluencyScore"`
			AvgProficiencyScore float64 `json:"avgProficiencyScore"`
		}
		var trends []Trend
		deps.DB.Raw(`SELECT DATE(created_at) as analysis_date,
			AVG(overall_score), AVG(stability_score), AVG(fluency_score), AVG(proficiency_score)
			FROM ai_analysis_result
			WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
			GROUP BY DATE(created_at) ORDER BY analysis_date`).Scan(&trends)
		response.Success(c, trends)
	}
}

func GetStatsByCreateBy(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var result map[string]map[string]int
		deps.DB.Raw(`SELECT 'notes' as item, COUNT(*) as total_count,
			SUM(CURDATE() = DATE(create_time)) as today_count,
			SUM(create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)) as last_30_days_count
			FROM ai_transcription_task WHERE 1=1
			UNION ALL
			SELECT 'analysis' as item, COUNT(*) as total_count,
			SUM(CURDATE() = DATE(created_at)) as today_count,
			SUM(created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)) as last_30_days_count
			FROM ai_analysis_result WHERE 1=1`).Scan(&result)
		response.Success(c, result)
	}
}

func parseUint(s string) uint {
	v, _ := strconv.ParseUint(s, 10, 64)
	return uint(v)
}