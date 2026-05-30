package jwt

import (
	"crypto/sha512"
	"encoding/hex"
	"errors"
	"fmt"

	"saaes-backend/internal/config"

	"github.com/golang-jwt/jwt/v5"
)

var (
	ErrInvalidToken = errors.New("令牌无效")
	ErrExpiredToken = errors.New("令牌已过期")
)

type Manager struct {
	secret []byte
}

type Claims struct {
	UserID   uint   `json:"user_id"`
	Username string `json:"username"`
	Role     string `json:"role"`
	TokenID  string `json:"jti"`
	jwt.RegisteredClaims
}

func NewManager(cfg *config.Config) *Manager {
	return &Manager{secret: []byte(cfg.JWT.Secret)}
}

func (m *Manager) GenerateToken(c Claims) (string, error) {
	token := jwt.NewWithClaims(jwt.SigningMethodHS512, c)
	return token.SignedString(m.secret)
}

func (m *Manager) ValidateToken(tokenStr string) (*Claims, error) {
	token, err := jwt.ParseWithClaims(tokenStr, &Claims{}, func(t *jwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", t.Header["alg"])
		}
		return m.secret, nil
	})
	if err != nil {
		if errors.Is(err, jwt.ErrTokenExpired) {
			return nil, ErrExpiredToken
		}
		return nil, ErrInvalidToken
	}
	if claims, ok := token.Claims.(*Claims); ok && token.Valid {
		return claims, nil
	}
	return nil, ErrInvalidToken
}

func HashToken(token string) string {
	h := sha512.Sum512([]byte(token))
	return hex.EncodeToString(h[:])
}