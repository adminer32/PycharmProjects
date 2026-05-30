-- 视频观看进度表 (aiidecn库)
USE aiidecn;

DROP TABLE IF EXISTS `video_progress`;
CREATE TABLE `video_progress` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `video_path` varchar(500) NOT NULL COMMENT '视频路径',
  `progress` int NOT NULL DEFAULT 0 COMMENT '观看进度百分比',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_video` (`user_id`, `video_path`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '视频观看进度表';
