package response

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type Result struct {
	Status  string      `json:"status"`
	Message string      `json:"message,omitempty"`
	Data    interface{} `json:"data,omitempty"`
}

func Success(c *gin.Context, data interface{}) {
	c.JSON(http.StatusOK, Result{Status: "success", Data: data})
}

func SuccessWithMsg(c *gin.Context, msg string, data interface{}) {
	c.JSON(http.StatusOK, Result{Status: "success", Message: msg, Data: data})
}

func Error(c *gin.Context, msg string) {
	c.JSON(http.StatusOK, Result{Status: "error", Message: msg})
}

func Unauthorized(c *gin.Context, msg string) {
	c.JSON(http.StatusUnauthorized, Result{Status: "error", Message: msg})
}

func Forbidden(c *gin.Context, msg string) {
	c.JSON(http.StatusForbidden, Result{Status: "error", Message: msg})
}

func BadRequest(c *gin.Context, msg string) {
	c.JSON(http.StatusBadRequest, Result{Status: "error", Message: msg})
}

func ServerError(c *gin.Context, msg string) {
	c.JSON(http.StatusInternalServerError, Result{Status: "error", Message: msg})
}

// PageResult 分页响应
type PageResult struct {
	List       interface{} `json:"list"`
	Total      int64       `json:"total"`
	Page       int         `json:"page"`
	Limit      int         `json:"limit"`
	TotalPages int         `json:"total_pages"`
}

func Page(c *gin.Context, list interface{}, total int64, page, limit int) {
	pages := int(total) / limit
	if int(total)%limit > 0 {
		pages++
	}
	c.JSON(http.StatusOK, Result{
		Status: "success",
		Data: PageResult{
			List:       list,
			Total:      total,
			Page:       page,
			Limit:      limit,
			TotalPages: pages,
		},
	})
}