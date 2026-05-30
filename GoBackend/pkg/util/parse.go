package util

import (
	"strconv"
)

// ParseUint 安全解析 uint
func ParseUint(s string) uint {
	v, _ := strconv.ParseUint(s, 10, 64)
	return uint(v)
}

// ParseInt 安全解析 int
func ParseInt(s string) int {
	v, _ := strconv.Atoi(s)
	return v
}

// ParseFloat 安全解析 float64
func ParseFloat(s string) float64 {
	v, _ := strconv.ParseFloat(s, 64)
	return v
}