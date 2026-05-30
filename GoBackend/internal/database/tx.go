package database

import (
	"context"
	"fmt"
	"time"

	"gorm.io/gorm"
)

func WithTransaction[T any](db *gorm.DB, fn func(tx *gorm.DB) (T, error)) (T, error) {
	tx := db.Begin()
	if tx.Error != nil {
		return *new(T), tx.Error
	}

	result, err := fn(tx)
	if err != nil {
		tx.Rollback()
		return result, err
	}

	return result, tx.Commit().Error
}

func WithRetry(ctx context.Context, maxRetries int, fn func() error) error {
	var err error
	for i := 0; i < maxRetries; i++ {
		if err = fn(); err == nil {
			return nil
		}
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(time.Duration(i+1) * 200 * time.Millisecond):
		}
	}
	return fmt.Errorf("重试 %d 次后失败: %w", maxRetries, err)
}