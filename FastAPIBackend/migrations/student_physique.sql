-- 学生体质表 (aiidecn库)
USE aiidecn;

DROP TABLE IF EXISTS `student_physique`;
CREATE TABLE `student_physique` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` int NOT NULL COMMENT '学生ID',
  `weight` decimal(5,2) NOT NULL COMMENT '体重(公斤)',
  `bmi` decimal(4,1) NOT NULL COMMENT 'BMI指数',
  `fat_percentage` decimal(4,1) NOT NULL COMMENT '脂肪率(%)',
  `skeletal_muscle_mass` decimal(5,1) NOT NULL COMMENT '骨骼肌量(公斤)',
  `visceral_fat_level` decimal(3,1) NOT NULL COMMENT '内脏脂肪等级',
  `limb_skeletal_muscle_index` decimal(4,1) NOT NULL COMMENT '四肢骨骼肌指数(kg/㎡)',
  `estimated_waist_hip_ratio` decimal(4,2) NOT NULL COMMENT '推测腰臀比',
  `body_type` varchar(50) NOT NULL COMMENT '身体类型',
  `body_shape` varchar(50) NOT NULL COMMENT '身体形态',
  `basal_metabolism_rate` int NOT NULL COMMENT '基础代谢率(千卡/日)',
  `moisture_rate` decimal(4,1) NOT NULL COMMENT '水分率(%)',
  `bone_salt_amount` decimal(5,2) NOT NULL COMMENT '骨盐量(公斤)',
  `protein_percentage` decimal(4,1) NOT NULL COMMENT '蛋白质(%)',
  `lean_body_mass` decimal(5,1) NOT NULL COMMENT '去脂体重(公斤)',
  `body_age` int NOT NULL COMMENT '身体年龄(岁)',
  `heart_rate` int NOT NULL COMMENT '心率(bpm)',
  `segment_moisture` decimal(5,2) NOT NULL COMMENT '水分',
  `segment_protein` decimal(5,2) NOT NULL COMMENT '蛋白质',
  `segment_fat_mass` decimal(5,2) NOT NULL COMMENT '脂肪量',
  `segment_bone_salt` decimal(5,2) NOT NULL COMMENT '骨盐量',
  `segment_fat_total` decimal(5,1) NOT NULL COMMENT '脂肪合计(公斤)',
  `segment_fat_right_upper` decimal(4,1) NOT NULL COMMENT '右上肢脂肪(公斤)',
  `segment_fat_left_upper` decimal(4,1) NOT NULL COMMENT '左上肢脂肪(公斤)',
  `segment_fat_trunk` decimal(4,1) NOT NULL COMMENT '躯干脂肪(公斤)',
  `segment_fat_right_lower` decimal(4,1) NOT NULL COMMENT '右下肢脂肪(公斤)',
  `segment_fat_left_lower` decimal(4,1) NOT NULL COMMENT '左下肢脂肪(公斤)',
  `segment_skeletal_muscle_total` decimal(5,1) NOT NULL COMMENT '骨骼肌合计(公斤)',
  `segment_skeletal_right_upper` decimal(4,1) NOT NULL COMMENT '右上肢骨骼肌(公斤)',
  `segment_skeletal_left_upper` decimal(4,1) NOT NULL COMMENT '左上肢骨骼肌(公斤)',
  `segment_skeletal_trunk` decimal(4,1) NOT NULL COMMENT '躯干骨骼肌(公斤)',
  `segment_skeletal_right_lower` decimal(4,1) NOT NULL COMMENT '右下肢骨骼肌(公斤)',
  `segment_skeletal_left_lower` decimal(4,1) NOT NULL COMMENT '左下肢骨骼肌(公斤)',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_id` (`student_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生体质表';

-- 插入示例数据 (学生ID为1)
INSERT INTO `student_physique` (
  `student_id`, `weight`, `bmi`, `fat_percentage`, `skeletal_muscle_mass`,
  `visceral_fat_level`, `limb_skeletal_muscle_index`, `estimated_waist_hip_ratio`,
  `body_type`, `body_shape`, `basal_metabolism_rate`, `moisture_rate`,
  `bone_salt_amount`, `protein_percentage`, `lean_body_mass`, `body_age`,
  `heart_rate`, `segment_moisture`, `segment_protein`, `segment_fat_mass`,
  `segment_bone_salt`, `segment_fat_total`, `segment_fat_right_upper`,
  `segment_fat_left_upper`, `segment_fat_trunk`, `segment_fat_right_lower`,
  `segment_fat_left_lower`, `segment_skeletal_muscle_total`,
  `segment_skeletal_right_upper`, `segment_skeletal_left_upper`,
  `segment_skeletal_trunk`, `segment_skeletal_right_lower`,
  `segment_skeletal_left_lower`
) VALUES (
  1, 66.80, 22.8, 8.6, 33.0,
  3.0, 9.0, 0.83,
  '偏瘦肌肉型', '匀称型', 1701, 67.8,
  3.14, 19.0, 61.1, 19,
  75, 45.29, 12.63, 5.74,
  3.14, 5.7, 0.1,
  0.1, 3.6, 1.0,
  0.9, 33.0,
  3.8, 3.6,
  6.7, 9.6,
  9.3
);
