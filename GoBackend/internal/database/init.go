package database

import (
	"fmt"
	"saaes-backend/internal/config"
	"saaes-backend/internal/model"

	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func Init(cfg *config.DatabaseConfig) (*gorm.DB, error) {
	dsn := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?charset=%s&parseTime=True&loc=Local",
		cfg.User, cfg.Password, cfg.Host, cfg.Port, cfg.DBName, cfg.Charset)

	db, err := gorm.Open(mysql.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	if err != nil {
		return nil, fmt.Errorf("连接数据库失败: %w", err)
	}

	sqlDB, err := db.DB()
	if err != nil {
		return nil, err
	}
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetMaxOpenConns(100)

	if err := db.AutoMigrate(
		&model.User{},
		&model.StudentProfile{},
		&model.JWTBlacklist{},
		&model.JWTIP{},
		&model.VideoProgress{},
		&model.CourseCategory{},
		&model.CourseInfo{},
		&model.StudentPhysique{},
		&model.PracticeRecord{},
		&model.Schedule{},
		&model.AITranscriptionTask{},
		&model.AIContentInfo{},
		&model.AICardInfo{},
		&model.AIAnalysisResult{},
		&model.AINoteInfo{},
		&model.VideoHistory{},
		&model.MotionAnalysisData{},
		&model.ScheduleEvent{},
	); err != nil {
		return nil, fmt.Errorf("自动迁移失败: %w", err)
	}

	return db, nil
}