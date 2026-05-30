package config

import (
	"encoding/json"
	"os"
)

type Config struct {
	ServerAddr     string
	VideoBasePath  string
	UploadsPath    string
	Database       DatabaseConfig
	Redis          RedisConfig
	JWT            JWTConfig
	DeepSeek       DeepSeekConfig
	DashScope      DashScopeConfig
	SendGrid       SendGridConfig
	CloudflareTurnstile CloudflareConfig
}

type DatabaseConfig struct {
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
	Charset  string
}

type RedisConfig struct {
	Host     string
	Port     string
	Password string
	DB       int
}

type JWTConfig struct {
	Secret        string
	AccessExpiry  int64 // seconds
	RefreshExpiry int64
}

type DeepSeekConfig struct {
	APIKey     string
	BaseURL    string
	Model      string
}

type DashScopeConfig struct {
	APIKey string
}

type SendGridConfig struct {
	APIKey string
}

type CloudflareConfig struct {
	SiteKey   string
	SecretKey string
}

func Load(path ...string) *Config {
	cfgPath := "configs/config.json"
	if len(path) > 0 {
		cfgPath = path[0]
	}

	data, err := os.ReadFile(cfgPath)
	if err != nil {
		// fallback to env or defaults
		return defaultConfig()
	}

	var cfg Config
	if err := json.Unmarshal(data, &cfg); err != nil {
		return defaultConfig()
	}
	return &cfg
}

func defaultConfig() *Config {
	return &Config{
		ServerAddr:    ":8001",
		VideoBasePath:  "/home/mrliao/下载/GVHMR/InputVideo/视频",
		UploadsPath:   "./data/uploads",
		Database: DatabaseConfig{
			Host:     "127.0.0.1",
			Port:     "3306",
			User:     "root",
			Password: "",
			DBName:   "aiidecn",
			Charset:  "utf8mb4",
		},
		Redis: RedisConfig{
			Host: "127.0.0.1",
			Port: "6379",
			DB:   0,
		},
		JWT: JWTConfig{
			Secret:        "change-me-in-production",
			AccessExpiry:  14400,
			RefreshExpiry: 604800,
		},
		DeepSeek: DeepSeekConfig{
			BaseURL: "https://api.deepseek.com/v1",
			Model:   "deepseek-chat",
		},
		DashScope: DashScopeConfig{
			APIKey: "",
		},
	}
}