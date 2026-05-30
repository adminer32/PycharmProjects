package middleware

import (
	"net/http"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
)

func JWTAuth(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		token := extractToken(c)
		if token == "" {
			response.Unauthorized(c, "未提供认证令牌")
			c.Abort()
			return
		}

		claims, err := parseToken(token, deps)
		if err != nil {
			response.Unauthorized(c, "令牌无效")
			c.Abort()
			return
		}

		exists, _ := deps.RDB.Exists(c.Request.Context(), "jwt:blacklist:"+claims.TokenID).Result()
		if exists > 0 {
			response.Unauthorized(c, "令牌已失效")
			c.Abort()
			return
		}

		c.Set("user_id", claims.UserID)
		c.Set("username", claims.Username)
		c.Set("role", claims.Role)
		c.Set("token_id", claims.TokenID)
		c.Next()
	}
}

type tokenClaims struct {
	UserID   uint   `json:"user_id"`
	Username string `json:"username"`
	Role     string `json:"role"`
	TokenID  string `json:"jti"`
	jwt.RegisteredClaims
}

func parseToken(tokenStr string, deps *router.Deps) (*tokenClaims, error) {
	token, err := jwt.ParseWithClaims(tokenStr, &tokenClaims{}, func(t *jwt.Token) (interface{}, error) {
		return []byte(deps.Config.JWT.Secret), nil
	})
	if err != nil {
		return nil, err
	}
	claims, ok := token.Claims.(*tokenClaims)
	if !ok || !token.Valid {
		return nil, jwt.ErrTokenInvalidClaims
	}
	return claims, nil
}

func extractToken(c *gin.Context) string {
	auth := c.GetHeader("Authorization")
	if auth == "" {
		auth = c.Query("token")
	}
	if auth == "" {
		auth = c.Query("access_token")
	}
	parts := strings.SplitN(auth, " ", 2)
	if len(parts) == 2 {
		return parts[1]
	}
	return auth
}

func RequireRole(roles ...string) gin.HandlerFunc {
	return func(c *gin.Context) {
		role, exists := c.Get("role")
		if !exists {
			response.Forbidden(c, "无权访问此资源")
			c.Abort()
			return
		}
		for _, r := range roles {
			if role.(string) == r {
				c.Next()
				return
			}
		}
		response.Forbidden(c, "无权访问此资源")
		c.Abort()
	}
}

func RateLimit(deps *router.Deps, maxReq int, windowSec int) gin.HandlerFunc {
	return func(c *gin.Context) {
		ip := c.ClientIP()
		key := "ratelimit:" + ip
		ctx := c.Request.Context()
		n, _ := deps.RDB.Incr(ctx, key).Result()
		if n == 1 {
			deps.RDB.Expire(ctx, key, 0)
		}
		if n > int64(maxReq) {
			c.JSON(http.StatusTooManyRequests, response.Result{Status: "error", Message: "请求过于频繁"})
			c.Abort()
			return
		}
		c.Next()
	}
}