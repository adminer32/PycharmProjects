package course

import (
	"os"
	"path/filepath"
	"saaes-backend/internal/model"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"strconv"

	"github.com/gin-gonic/gin"
)

func QueryCourses(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Param struct {
				ParentCategoryName string `json:"parent_category_name"`
			} `json:"param"`
		}
		c.ShouldBindJSON(&req)

		parentMapping := map[string][]string{
			"基本课程": {"基本动作", "基本技术", "比赛规则", "战术运用"},
			"综合课程": {"单人专项动作", "系列课程"},
		}
		foldersToScan := parentMapping[req.Param.ParentCategoryName]
		if foldersToScan == nil {
			for _, children := range parentMapping {
				foldersToScan = append(foldersToScan, children...)
			}
		}

		var categories []map[string]interface{}
		for _, folderName := range foldersToScan {
			folderPath := filepath.Join(deps.Config.VideoBasePath, folderName)
			if _, err := os.Stat(folderPath); os.IsNotExist(err) {
				continue
			}
			entries, _ := os.ReadDir(folderPath)
			var courses []map[string]interface{}
			for i, entry := range entries {
				if entry.IsDir() || isVideoFile(entry.Name()) {
					courses = append(courses, map[string]interface{}{
						"id": i + 1, "name": stripExt(entry.Name()), "teacherName": "教练",
						"coverImageUrl": "", "enabled": true, "createTime": "",
						"content": "", "description": "",
						"videoUrl": "/videos/" + folderName + "/" + entry.Name(),
						"videoDuration": "00:00", "levelName": "初级",
					})
				}
			}
			if len(courses) > 0 {
				categories = append(categories, map[string]interface{}{
					"subCategoryId": folderName, "subCategoryName": folderName,
					"subCategoryCourse": courses,
				})
			}
		}

		total := 0
		for _, cat := range categories {
			total += len(cat["subCategoryCourse"].([]map[string]interface{}))
		}
		c.JSON(200, response.Result{Status: "success", Data: gin.H{"list": categories, "total": total}})
	}
}

func GetTeacherLessons(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		classID := c.Query("class_id")
		teacherID := c.Query("teacher_id")
		query := deps.DB.Model(&model.CourseInfo{})
		if classID != "" {
			query = query.Where("category_id = ?", parseUint(classID))
		}
		if teacherID != "" {
			query = query.Where("created_by = ?", parseUint(teacherID))
		}
		var courses []model.CourseInfo
		query.Where("enabled = 1").Find(&courses)
		response.Success(c, courses)
	}
}

func GetTeacherLesson(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		lessonID := parseUint(c.Param("lessonId"))
		var course model.CourseInfo
		if err := deps.DB.First(&course, lessonID).Error; err != nil {
			response.Error(c, "课程不存在")
			return
		}
		response.Success(c, course)
	}
}

func GetNoteById(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		id := c.Query("id")
		if id == "" {
			response.BadRequest(c, "缺少 id")
			return
		}
		var task model.AITranscriptionTask
		if err := deps.DB.Where("id = ? AND deleted = 0", parseUint(id)).First(&task).Error; err != nil {
			response.Error(c, "笔记不存在")
			return
		}
		response.Success(c, map[string]interface{}{
			"id": task.ID, "taskId": task.TaskID, "videoUrl": task.VideoURL,
			"title": task.Title, "autoChapters": task.AutoChapters,
			"summarization": task.Summarization, "meetingAssistance": task.MeetingAssist,
			"personalNote": task.PersonalNote, "taskStatus": task.TaskStatus,
			"createTime": task.CreatedAt.Format("2006-01-02 15:04:05"),
		})
	}
}

func parseUint(s string) uint {
	v, _ := strconv.ParseUint(s, 10, 64)
	return uint(v)
}

func isVideoFile(name string) bool {
	ext := filepath.Ext(name)
	return ext == ".mp4" || ext == ".avi" || ext == ".mov" || ext == ".mkv"
}

func stripExt(name string) string {
	for i := len(name) - 1; i >= 0; i-- {
		if name[i] == '.' {
			return name[:i]
		}
	}
	return name
}