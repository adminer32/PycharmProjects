package user

import (
	"crypto/rand"
	"encoding/hex"
	"mime/multipart"
	"net/http"
	"saaes-backend/internal/router"
	"saaes-backend/pkg/response"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

// Login POST /token
func Login(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Username string `json:"username" binding:"required"`
			Password string `json:"password" binding:"required"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "参数不完整")
			return
		}

		var user struct {
			ID           uint   `gorm:"primaryKey" json:"id"`
			Username     string `gorm:"uniqueIndex;size:50" json:"username"`
			PasswordHash string `gorm:"column:password_hash;size:128" json:"-"`
			PasswordSalt string `gorm:"column:password_salt;size:16" json:"-"`
			Role         string `gorm:"type:enum('ADMIN','TEACHER','STUDENT')" json:"role"`
			Active       bool   `gorm:"default:false" json:"active"`
		}
		if err := deps.DB.Where("username = ?", req.Username).First(&user).Error; err != nil {
			c.JSON(http.StatusOK, gin.H{"success": false, "message": "用户名或密码错误"})
			return
		}
		if !user.Active {
			c.JSON(http.StatusOK, gin.H{"success": false, "message": "账号未激活"})
			return
		}
		if !bcryptCompare([]byte(user.PasswordHash), []byte(req.Password+user.PasswordSalt)) {
			c.JSON(http.StatusOK, gin.H{"success": false, "message": "用户名或密码错误"})
			return
		}

		pair, _ := generateTokenPair(c, deps, user.ID, user.Username, user.Role)
		c.JSON(http.StatusOK, pair)
	}
}

// CheckToken GET /token
func CheckToken(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		token := extractToken(c)
		claims, err := parseToken(token, deps)
		if err != nil {
			c.JSON(http.StatusOK, gin.H{"success": false, "valid": false})
			return
		}
		c.JSON(http.StatusOK, gin.H{"success": true, "valid": true, "user_id": claims.UserID, "role": claims.Role})
	}
}

// RefreshToken POST /token/refresh
func RefreshToken(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			RefreshToken string `json:"refresh_token" binding:"required"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 refresh_token")
			return
		}
		claims, err := parseToken(req.RefreshToken, deps)
		if err != nil {
			response.Unauthorized(c, "refresh token 无效")
			return
		}
		var user struct {
			ID       uint   `gorm:"primaryKey" json:"id"`
			Username string `json:"username"`
			Role     string `json:"role"`
		}
		if err := deps.DB.First(&user, claims.UserID).Error; err != nil {
			response.Unauthorized(c, "用户不存在")
			return
		}
		pair, _ := generateTokenPair(c, deps, user.ID, user.Username, user.Role)
		c.JSON(http.StatusOK, pair)
	}
}

// CheckVToken GET /vtoken
func CheckVToken(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	}
}

// CreateVToken POST /vtoken
func CreateVToken(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req struct {
			Email string `json:"email" binding:"required"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			response.BadRequest(c, "缺少 email")
			return
		}
		code := randomCode(6)
		deps.RDB.Set(c.Request.Context(), "vtoken:"+req.Email, code, 15*time.Minute)
		c.JSON(http.StatusOK, gin.H{"success": true, "message": "验证码已发送"})
	}
}

// GetCaptcha GET /captcha
func GetCaptcha(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		code := randomCode(4)
		deps.RDB.Set(c.Request.Context(), "captcha:"+c.ClientIP(), code, 5*time.Minute)
		c.Header("Content-Type", "image/svg+xml")
		c.String(http.StatusOK, "<svg xmlns='http://www.w3.org/2000/svg'><text x='10' y='25' font-size='20'>"+code+"</text></svg>")
	}
}

// UploadFile POST /file/operation/upload
func UploadFile(deps *router.Deps) gin.HandlerFunc {
	return func(c *gin.Context) {
		file, err := c.FormFile("file")
		if err != nil {
			response.BadRequest(c, "未上传文件")
			return
		}
		if file.Size > 100*1024*1024 {
			response.BadRequest(c, "文件过大")
			return
		}
		filename := saveUpload(file, deps.Config.UploadsPath)
		c.JSON(http.StatusOK, response.Result{Status: "success", Data: gin.H{"object": filename}})
	}
}

// ─── helpers ──────────────────────────────────────────────────────────────

type TokenClaims struct {
	UserID   uint   `json:"user_id"`
	Username string `json:"username"`
	Role     string `json:"role"`
	TokenID  string `json:"jti"`
	jwt.RegisteredClaims
}

func generateTokenPair(c *gin.Context, deps *router.Deps, userID uint, username, role string) (gin.H, error) {
	tokenID := uuidMust()
	now := time.Now()
	cfg := deps.Config

	accessExp := now.Add(time.Duration(cfg.JWT.AccessExpiry) * time.Second)
	accessToken, _ := signToken(tokenID, userID, username, role, accessExp.Unix(), deps)
	refreshExp := now.Add(time.Duration(cfg.JWT.RefreshExpiry) * time.Second)
	refreshToken, _ := signToken(tokenID+"_refresh", userID, username, role, refreshExp.Unix(), deps)

	return gin.H{
		"success": true, "access_token": accessToken, "refresh_token": refreshToken,
		"expires_in": cfg.JWT.AccessExpiry, "token_id": tokenID,
	}, nil
}

func signToken(tokenID string, userID uint, username, role string, exp int64, deps *router.Deps) (string, error) {
	claims := TokenClaims{
		UserID:   userID,
		Username: username,
		Role:     role,
		TokenID:  tokenID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Unix(exp, 0)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			ID:        tokenID,
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS512, claims)
	return token.SignedString([]byte(deps.Config.JWT.Secret))
}

func parseToken(tokenStr string, deps *router.Deps) (*TokenClaims, error) {
	token, err := jwt.ParseWithClaims(tokenStr, &TokenClaims{}, func(t *jwt.Token) (interface{}, error) {
		return []byte(deps.Config.JWT.Secret), nil
	})
	if err != nil {
		return nil, err
	}
	claims, ok := token.Claims.(*TokenClaims)
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

func bcryptCompare(hashed []byte, data []byte) bool {
	return bcrypt.CompareHashAndPassword(hashed, data) == nil
}

func saveUpload(file *multipart.FileHeader, basePath string) string {
	src, _ := file.Open()
	defer src.Close()
	buf := make([]byte, 8)
	rand.Read(buf)
	suffix := hex.EncodeToString(buf)
	filename := time.Now().Format("20060102150405") + "_" + suffix + "_" + file.Filename
	return basePath + "/" + filename
}

func randomCode(n int) string {
	const chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz"
	b := make([]byte, n)
	rand.Read(b)
	for i := range b {
		b[i] = chars[int(b[i])%len(chars)]
	}
	return string(b)
}

func uuidMust() string {
	b := make([]byte, 16)
	rand.Read(b)
	return uuid.UUID(b).String()
}