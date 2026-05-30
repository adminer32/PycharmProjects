package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"saaes-backend/internal/cache"
	"saaes-backend/internal/config"
	"saaes-backend/internal/database"
	"saaes-backend/internal/handler/ai"
	"saaes-backend/internal/handler/content"
	"saaes-backend/internal/handler/course"
	"saaes-backend/internal/handler/schedule"
	"saaes-backend/internal/handler/student"
	"saaes-backend/internal/handler/transcribe"
	"saaes-backend/internal/handler/user"
	"saaes-backend/internal/handler/video"
	"saaes-backend/internal/middleware"
	"saaes-backend/internal/router"
	"saaes-backend/internal/websocket"

	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.Load()

	db, err := database.Init(&cfg.Database)
	if err != nil {
		log.Fatalf("数据库初始化失败: %v", err)
	}
	rdb := cache.InitRedis(&cfg.Redis)

	deps := &router.Deps{
		DB:     db,
		RDB:    rdb,
		Config: cfg,
	}

	r := gin.New()
	r.Use(gin.Logger(), gin.Recovery())
	r.Static("/videos", cfg.VideoBasePath)
	r.Static("/uploads", cfg.UploadsPath)

	// 公开路由
	r.POST("/token", user.Login(deps))
	r.GET("/token", user.CheckToken(deps))
	r.POST("/token/refresh", user.RefreshToken(deps))
	r.GET("/vtoken", user.CheckVToken(deps))
	r.POST("/vtoken", user.CreateVToken(deps))
	r.GET("/captcha", user.GetCaptcha(deps))
	r.POST("/file/operation/upload", user.UploadFile(deps))

	jwtAuth := middleware.JWTAuth(deps)

	api := r.Group("/api", jwtAuth)
	api.POST("/analysis/save", content.SaveAnalysis(deps))
	api.GET("/analysis/history", content.GetAnalysisHistory(deps))
	api.GET("/analysis/queryByTaskId", content.QueryByTaskId(deps))
	api.GET("/analysis/generateTrainingPlan", content.GenerateTrainingPlan(deps))
	api.GET("/physique/query", student.GetPhysique(deps))
	api.POST("/physique/create", student.CreatePhysique(deps))
	api.PUT("/physique/update", student.UpdatePhysique(deps))
	api.POST("/course/query", course.QueryCourses(deps))
	api.GET("/course/getNoteById", course.GetNoteById(deps))
	api.GET("/video/progress", video.GetProgress(deps))
	api.POST("/video/progress", video.SaveProgress(deps))
	api.POST("/video/history/save", video.SaveHistory(deps))
	api.POST("/video/history/query", video.QueryHistory(deps))
	api.GET("/video/history/detail", video.GetHistoryDetail(deps))
	api.DELETE("/video/history/del", video.DeleteHistory(deps))
	api.POST("/ai/NotePolishing", ai.NotePolishing(deps))
	api.GET("/visualization/getMotionNameAnalysis", student.GetMotionNameAnalysis(deps))
	api.GET("/visualization/queryHistoricalTrends", student.GetHistoricalTrends(deps))
	api.GET("/visualization/getStatsByCreateBy", student.GetStatsByCreateBy(deps))
	api.GET("/content/queryCategory", content.QueryCategory(deps))
	api.POST("/content/insert", content.InsertContent(deps))

	v0 := api.Group("/v0")
	v0AI := v0.Group("/ai")
	v0AI.POST("/chat", ai.Chat(deps))
	v0AI.POST("/coach/chat", ai.CoachChat(deps))
	v0AI.GET("/models", ai.ListModels(deps))
	v0AI.GET("/health", ai.HealthCheck(deps))
	v0AI.POST("/queryPersonalNotes", ai.QueryPersonalNotes(deps))
	v0AI.POST("/addTranscriptionTask", ai.AddTranscriptionTask(deps))
	v0AI.POST("/updateNote", ai.UpdateNote(deps))
	v0AI.DELETE("/del", ai.DeleteNotes(deps))
	v0AI.GET("/queryTaskStatus", ai.QueryTaskStatus(deps))

	st := v0.Group("/student")
	st.GET("/learning/stats/:studentId", student.GetLearningStats(deps))
	st.GET("/learning/action-scores/:studentId", student.GetActionScores(deps))
	st.GET("/learning/shuttlecock-skills/:studentId", student.GetShuttlecockSkills(deps))
	st.GET("/learning/weekly-trend/:studentId", student.GetWeeklyTrend(deps))
	st.GET("/learning/checkin-records/:studentId", student.GetCheckinRecords(deps))
	st.POST("/learning/practice-record", student.SavePracticeRecord(deps))
	st.GET("/learning/practice-records/:studentId", student.GetPracticeRecords(deps))
	st.GET("/learning/practice-record/:recordId", student.GetPracticeRecord(deps))
	st.DELETE("/learning/practice-record/:recordId", student.DeletePracticeRecord(deps))
	st.GET("/schedule/query", schedule.QuerySchedules(deps))
	st.POST("/schedule/save", schedule.SaveSchedule(deps))
	st.DELETE("/schedule/del", schedule.DeleteSchedules(deps))
	st.GET("/physique/", student.GetPhysique(deps))
	st.POST("/physique/", student.CreatePhysique(deps))
	st.PUT("/physique/", student.UpdatePhysiqueData(deps))
	st.GET("/auth/", student.GetProfile(deps))
	st.POST("/auth/", student.CreateProfile(deps))
	st.GET("/users/profile", student.GetUserProfile(deps))

	tch := v0.Group("/teacher")
	tch.GET("/lessons", course.GetTeacherLessons(deps))
	tch.GET("/lessons/:lessonId", course.GetTeacherLesson(deps))

	tr := v0.Group("/transcribe")
	tr.POST("/transcribe", transcribe.TranscribeVideo(deps))
	tr.POST("/analyze", transcribe.AnalyzeTranscription(deps))
	tr.POST("/full-process", transcribe.FullProcessVideo(deps))
	tr.GET("/health", transcribe.HealthCheck(deps))

	r.GET("/ws/ai", websocket.HandleAIChat(deps))
	r.GET("/ws/teacher/notification", websocket.HandleTeacherNotification(deps))

	srv := &http.Server{Addr: cfg.ServerAddr, Handler: r}
	go func() {
		log.Printf("服务启动于 %s", cfg.ServerAddr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("服务启动失败: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("服务关闭失败: %v", err)
	}
	log.Println("服务已关闭")
}