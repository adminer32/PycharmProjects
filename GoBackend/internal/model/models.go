package model

import (
	"time"

	"gorm.io/gorm"
)

// User 用户账号
type User struct {
	ID           uint           `gorm:"primaryKey" json:"id"`
	Username     string         `gorm:"uniqueIndex;size:50;not null" json:"username"`
	PasswordHash string         `gorm:"column:password_hash;size:128;not null" json:"-"`
	PasswordSalt string         `gorm:"column:password_salt;size:16;not null" json:"-"`
	Role         string         `gorm:"type:enum('ADMIN','TEACHER','STUDENT');default:'STUDENT'" json:"role"`
	Active       bool           `gorm:"default:false" json:"active"`
	Avatar       string         `gorm:"size:255" json:"avatar"`
	Email        string         `gorm:"size:255" json:"email"`
	Telephone    string         `gorm:"size:20" json:"telephone"`
	CreatedAt    time.Time      `json:"created_at"`
	UpdatedAt    time.Time      `json:"updated_at"`
	DeletedAt    gorm.DeletedAt `gorm:"index" json:"-"`
}

// StudentProfile 学生信息
type StudentProfile struct {
	UserID   uint   `gorm:"primaryKey" json:"user_id"`
	StudentID uint  `gorm:"uniqueIndex;not null" json:"student_id"`
	RealName string `gorm:"size:50;not null" json:"real_name"`
	Gender   string `gorm:"size:2;default:'未知'" json:"gender"`
	Grade    uint   `gorm:"not null" json:"grade"`
	Major    string `gorm:"size:50" json:"major"`
	ClassID  string `gorm:"column:class_id;size:20" json:"class_id"`
	Avatar   string `gorm:"size:255" json:"avatar"`
	Email    string `gorm:"size:255" json:"email"`
}

// TeacherProfile 教师信息
type TeacherProfile struct {
	ID         uint   `gorm:"primaryKey" json:"id"`
	TeacherID  uint   `gorm:"uniqueIndex;not null" json:"teacher_id"`
	Name       string `gorm:"size:50;not null" json:"name"`
	Specialty  string `gorm:"size:100" json:"specialty"`
	Department string `gorm:"size:100" json:"department"`
	Avatar     string `gorm:"size:255" json:"avatar"`
	Email      string `gorm:"size:255" json:"email"`
}

// JWTBlacklist JWT 黑名单（退出登录时加入）
type JWTBlacklist struct {
	ID           uint   `gorm:"primaryKey" json:"id"`
	TokenID      string `gorm:"uniqueIndex;size:64;not null" json:"token_id"`
	AccessToken  string `gorm:"type:text;not null" json:"access_token"`
	RefreshToken string `gorm:"type:text" json:"refresh_token"`
}

// JWTIP IP 绑定记录
type JWTIP struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	TokenID   string    `gorm:"uniqueIndex;size:64;not null" json:"token_id"`
	IPAddress string    `gorm:"size:45;not null" json:"ip_address"`
	CreatedAt time.Time `json:"created_at"`
}

// VideoProgress 视频观看进度
type VideoProgress struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	UserID    uint      `gorm:"uniqueIndex:uk_user_video;not null" json:"user_id"`
	VideoPath string    `gorm:"uniqueIndex:uk_user_video;size:500;not null" json:"video_path"`
	Progress  int       `gorm:"default:0" json:"progress"`
	CreatedAt time.Time `gorm:"autoCreateTime" json:"create_time"`
	UpdatedAt time.Time `gorm:"autoUpdateTime" json:"update_time"`
}

// CourseCategory 课程分类
type CourseCategory struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	ParentID  *uint          `json:"parent_id"`
	Name      string         `gorm:"size:255" json:"name"`
	Enabled   bool          `json:"enabled"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

// CourseInfo 课程信息
type CourseInfo struct {
	ID            uint           `gorm:"primaryKey" json:"id"`
	Name          string         `gorm:"size:255" json:"name"`
	CategoryID    *uint          `json:"category_id"`
	Content       string         `gorm:"type:text" json:"content"`
	Description   string         `gorm:"type:text" json:"description"`
	VideoURL      string         `gorm:"size:500" json:"video_url"`
	VideoDuration string         `gorm:"size:50" json:"video_duration"`
	TeacherName   string         `gorm:"size:255" json:"teacher_name"`
	CoverImageURL string         `gorm:"size:255" json:"cover_image_url"`
	LevelName     string         `gorm:"size:255" json:"level_name"`
	CreatedBy     *uint          `json:"create_by"`
	CreatedAt     *time.Time     `json:"create_time"`
	UpdatedAt     *time.Time     `json:"update_time"`
	Enabled       bool           `json:"enabled"`
	DeletedAt     gorm.DeletedAt `gorm:"index" json:"-"`
}

// StudentPhysique 学生体质数据
type StudentPhysique struct {
	ID                       uint      `gorm:"primaryKey" json:"id"`
	StudentID                uint      `gorm:"uniqueIndex;not null" json:"student_id"`
	Weight                   float64   `gorm:"type:decimal(5,2)" json:"weight"`
	BMI                       float64   `gorm:"type:decimal(4,1)" json:"bmi"`
	FatPercentage            float64   `gorm:"type:decimal(4,1)" json:"fat_percentage"`
	SkeletalMuscleMass       float64   `gorm:"type:decimal(5,1)" json:"skeletal_muscle_mass"`
	VisceralFatLevel         float64   `gorm:"type:decimal(3,1)" json:"visceral_fat_level"`
	LimbSkeletalMuscleIndex  float64   `gorm:"type:decimal(4,1)" json:"limb_skeletal_muscle_index"`
	EstWaistHipRatio         float64   `gorm:"type:decimal(4,2)" json:"estimated_waist_hip_ratio"`
	BodyType                 string    `gorm:"size:50" json:"body_type"`
	BodyShape                string    `gorm:"size:50" json:"body_shape"`
	BasalMetabolismRate      int       `json:"basal_metabolism_rate"`
	MoistureRate             float64   `gorm:"type:decimal(4,1)" json:"moisture_rate"`
	BoneSaltAmount           float64   `gorm:"type:decimal(5,2)" json:"bone_salt_amount"`
	ProteinPercentage        float64   `gorm:"type:decimal(4,1)" json:"protein_percentage"`
	LeanBodyMass             float64   `gorm:"type:decimal(5,1)" json:"lean_body_mass"`
	BodyAge                  int       `json:"body_age"`
	HeartRate                int       `json:"heart_rate"`
	SegmentMoisture          float64   `gorm:"type:decimal(5,2)" json:"segment_moisture"`
	SegmentProtein           float64   `gorm:"type:decimal(5,2)" json:"segment_protein"`
	SegmentFatMass           float64   `gorm:"type:decimal(5,2)" json:"segment_fat_mass"`
	SegmentBoneSalt          float64   `gorm:"type:decimal(5,2)" json:"segment_bone_salt"`
	SegmentFatTotal          float64   `gorm:"type:decimal(5,1)" json:"segment_fat_total"`
	SegmentFatRightUpper     float64   `gorm:"type:decimal(4,1)" json:"segment_fat_right_upper"`
	SegmentFatLeftUpper      float64   `gorm:"type:decimal(4,1)" json:"segment_fat_left_upper"`
	SegmentFatTrunk          float64   `gorm:"type:decimal(4,1)" json:"segment_fat_trunk"`
	SegmentFatRightLower     float64   `gorm:"type:decimal(4,1)" json:"segment_fat_right_lower"`
	SegmentFatLeftLower      float64   `gorm:"type:decimal(4,1)" json:"segment_fat_left_lower"`
	SegmentSkeletalMuscleTotal float64 `gorm:"type:decimal(5,1)" json:"segment_skeletal_muscle_total"`
	SegmentSkeletalRightUpper float64  `gorm:"type:decimal(4,1)" json:"segment_skeletal_right_upper"`
	SegmentSkeletalLeftUpper float64   `gorm:"type:decimal(4,1)" json:"segment_skeletal_left_upper"`
	SegmentSkeletalTrunk     float64   `gorm:"type:decimal(4,1)" json:"segment_skeletal_trunk"`
	SegmentSkeletalRightLower float64  `gorm:"type:decimal(4,1)" json:"segment_skeletal_right_lower"`
	SegmentSkeletalLeftLower float64   `gorm:"type:decimal(4,1)" json:"segment_skeletal_left_lower"`
	CreatedAt                *time.Time `json:"create_time"`
	UpdatedAt                *time.Time `json:"update_time"`
}

// PracticeRecord 练习记录
type PracticeRecord struct {
	ID         uint      `gorm:"primaryKey" json:"id"`
	StudentID  uint      `gorm:"not null;index" json:"student_id"`
	ActionType string    `gorm:"size:100;not null" json:"action_type"`
	VideoURL   string    `gorm:"size:500;not null" json:"video_url"`
	ResultData string    `gorm:"type:text" json:"result_data"`
	CreatedAt  time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// Schedule 日程计划
type Schedule struct {
	ID           uint      `gorm:"primaryKey" json:"id"`
	StudentID    uint      `gorm:"not null;index" json:"student_id"`
	ScheduleDate time.Time `gorm:"type:date;not null" json:"schedule_date"`
	Time         string    `gorm:"type:time;not null" json:"time"`
	Type         string    `gorm:"size:50;default:'warning'" json:"type"`
	Content      string    `gorm:"type:text" json:"content"`
	CreatedAt    time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// AITranscriptionTask AI 转录任务
type AITranscriptionTask struct {
	ID          uint      `gorm:"primaryKey" json:"id"`
	TaskID      string    `gorm:"uniqueIndex;size:64;not null" json:"task_id"`
	VideoURL    string    `gorm:"size:500" json:"video_url"`
	Transcription string  `gorm:"type:text" json:"transcription"`
	AutoChapters string  `gorm:"type:text" json:"auto_chapters"`
	Summarization  string `gorm:"type:text" json:"summarization"`
	MeetingAssist string `gorm:"column:meeting_assistance;type:text" json:"meeting_assistance"`
	PersonalNote  string `gorm:"column:personal_note;type:text" json:"personal_note"`
	TaskStatus    string `gorm:"size:50;default:'进行中'" json:"task_status"`
	Title        string `gorm:"size:255" json:"title"`
	Deleted      int     `gorm:"default:0" json:"deleted"`
	CreatedAt    time.Time `gorm:"autoCreateTime" json:"create_time"`
}

// AIContentInfo AI 内容记录（聊天）
type AIContentInfo struct {
	ID         uint      `gorm:"primaryKey" json:"id"`
	HistoryID  uint      `gorm:"index" json:"history_id"`
	TaskID     string    `gorm:"size:64" json:"task_id"`
	Type       string    `gorm:"size:20;default:'text'" json:"type"`
	Text       string    `gorm:"type:text" json:"text"`
	Card       string    `gorm:"type:json" json:"card"`
	Sender     string    `gorm:"size:10" json:"sender"`
	Analysis   string    `gorm:"type:json" json:"analysis"`
	CreatedAt  time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// AICardInfo AI 消息卡片
type AICardInfo struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	ContentID uint      `gorm:"index" json:"content_id"`
	Title     string    `gorm:"size:255" json:"title"`
	VideoURL  string    `gorm:"size:500" json:"video_url"`
	Analysis  string    `gorm:"type:json" json:"analysis"`
	CreatedAt time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// AIAnalysisResult AI 分析结果
type AIAnalysisResult struct {
	ID            uint      `gorm:"primaryKey" json:"id"`
	TaskID        string    `gorm:"uniqueIndex;size:64;not null" json:"task_id"`
	StudentID     uint      `gorm:"index" json:"student_id"`
	AnalysisData  string    `gorm:"type:json" json:"analysis_data"`
	StabilityScore    float64 `json:"stability_score"`
	ProficiencyScore  float64 `json:"proficiency_score"`
	FluencyScore     float64 `json:"fluency_score"`
	OverallScore     float64 `json:"overall_score"`
	Feedback      string    `gorm:"type:text" json:"feedback"`
	CreatedAt     time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// AINoteInfo AI 笔记（视频笔记/摘要）
type AINoteInfo struct {
	ID          uint      `gorm:"primaryKey" json:"id"`
	TaskID      string    `gorm:"size:64" json:"task_id"`
	StudentID   uint      `gorm:"index" json:"student_id"`
	VideoURL    string    `gorm:"size:500" json:"video_url"`
	Title       string    `gorm:"size:255" json:"title"`
	AutoChapters string   `gorm:"type:text" json:"auto_chapters"`
	Summarization string  `gorm:"type:text" json:"summarization"`
	PersonalNote string   `gorm:"type:text" json:"personal_note"`
	TaskStatus  string    `gorm:"size:50" json:"task_status"`
	CreatedAt   time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// VideoHistory 视频历史记录
type VideoHistory struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	HistoryID uint      `gorm:"index" json:"history_id"`
	StudentID uint      `gorm:"index" json:"student_id"`
	Content   string    `gorm:"type:text" json:"content"`
	CreatedAt time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// MotionAnalysisData 动作分析数据（原始 JSON）
type MotionAnalysisData struct {
	ID           uint      `gorm:"primaryKey" json:"id"`
	TaskID       string    `gorm:"uniqueIndex;size:64" json:"task_id"`
	StudentID    uint      `gorm:"index" json:"student_id"`
	AnalysisData string    `gorm:"type:json" json:"analysis_data"`
	CreatedAt    time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// ScheduleEvent 日程事件（SpringBoot 侧）
type ScheduleEvent struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	StudentID uint      `gorm:"index" json:"student_id"`
	Title     string    `gorm:"size:255" json:"title"`
	StartTime time.Time `gorm:"not null" json:"start_time"`
	EndTime   time.Time `json:"end_time"`
	Type      string    `gorm:"size:50" json:"type"`
	Content   string    `gorm:"type:text" json:"content"`
	CreatedAt time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// StudentCheckin 学生打卡记录
type StudentCheckin struct {
	ID          uint      `gorm:"primaryKey" json:"id"`
	StudentID   uint      `gorm:"not null;index" json:"student_id"`
	CheckinDate time.Time `gorm:"type:date;not null" json:"checkin_date"`
	Duration    int       `json:"duration"`
	CheckinType string   `gorm:"size:50" json:"checkin_type"`
	Score       float64   `gorm:"type:decimal(4,1)" json:"score"`
}

// ShuttlecockSkill 毽球技能
type ShuttlecockSkill struct {
	ID         uint      `gorm:"primaryKey" json:"id"`
	StudentID  uint      `gorm:"not null;index" json:"student_id"`
	SkillName  string    `gorm:"size:100;not null" json:"skill_name"`
	Level      int       `json:"level"`
	LearnedDate time.Time `gorm:"type:date" json:"learned_date"`
}