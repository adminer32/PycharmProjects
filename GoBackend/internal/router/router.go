package router

import (
	"saaes-backend/internal/config"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"github.com/redis/go-redis/v9"
)

type Deps struct {
	DB     *gorm.DB
	RDB    *redis.Client
	Config *config.Config
}

type Engine struct {
	*gin.Engine
}

func New(deps Deps) *Engine {
	r := gin.New()
	return &Engine{Engine: r}
}

func (e *Engine) Group(base string, middlewares ...gin.HandlerFunc) *gin.RouterGroup {
	return e.Engine.Group(base, middlewares...)
}