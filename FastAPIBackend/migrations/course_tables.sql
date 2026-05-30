-- 课程表迁移到 FastAPI 数据库 (aiidecn)

-- ----------------------------
-- Table structure for course_category
-- ----------------------------
DROP TABLE IF EXISTS `course_category`;
CREATE TABLE `course_category` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '课程中心分类id',
  `parent_id` int NULL DEFAULT NULL COMMENT '父分类id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '课程中心分类名称',
  `enabled` bit(1) NULL DEFAULT NULL COMMENT '状态（1：启用，2：禁用）',
  `deleted` tinyint NULL DEFAULT NULL COMMENT '是否已删除1：是，0：否',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of course_category
-- ----------------------------
INSERT INTO `course_category` VALUES (1, NULL, '基本课程', b'1', 0);
INSERT INTO `course_category` VALUES (2, NULL, '综合课程', b'1', 0);
INSERT INTO `course_category` VALUES (3, 1, '基本动作', b'1', 0);
INSERT INTO `course_category` VALUES (4, 1, '基本技术', b'1', 0);
INSERT INTO `course_category` VALUES (5, 1, '比赛规则', b'1', 0);
INSERT INTO `course_category` VALUES (6, 1, '战术运用', b'1', 0);
INSERT INTO `course_category` VALUES (7, NULL, '个人课程', b'1', 0);

-- ----------------------------
-- Table structure for course_info
-- ----------------------------
DROP TABLE IF EXISTS `course_info`;
CREATE TABLE `course_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '课程id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '课程名称',
  `category_id` int NULL DEFAULT NULL COMMENT '课程所属分类ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '章节内容',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '课程描述',
  `video_url` varchar(500) NULL DEFAULT NULL COMMENT '视频URL',
  `video_duration` varchar(50) NULL DEFAULT NULL COMMENT '视频时长',
  `teacher_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上课老师名称',
  `cover_image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '课程封面图片url',
  `level_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '等级名称',
  `create_by` int NULL DEFAULT NULL COMMENT '课程创建者id',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `enabled` bit(1) NULL DEFAULT NULL COMMENT '状态（1：启用，2：禁用）',
  `deleted` tinyint NULL DEFAULT NULL COMMENT '是否已删除1：是，0：否',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of course_info (示例数据)
-- ----------------------------
INSERT INTO `course_info` VALUES (1, '盘踢技巧详解', 3, '课程内容介绍', '盘踢是毽球运动中最基础的动作之一', '/videos/单人专项动作/盘踢.mp4', '12:30', '张教练', '/files/course/panti.png', '初级', 1, NOW(), NULL, b'1', 0);
INSERT INTO `course_info` VALUES (2, '磕踢技巧详解', 3, '课程内容介绍', '磕踢动作要领及训练方法', '/videos/单人专项动作/磕踢.mp4', '15:45', '张教练', '/files/course/kenti.png', '初级', 1, NOW(), NULL, b'1', 0);
INSERT INTO `course_info` VALUES (3, '绷踢技巧教学', 3, '课程内容介绍', '绷踢技术的详细讲解', '/videos/单人专项动作/绷踢.mp4', '16:40', '王教练', '/files/course/bengti.png', '初级', 1, NOW(), NULL, b'1', 0);
INSERT INTO `course_info` VALUES (4, '拐踢动作纠正', 3, '课程内容介绍', '拐踢常见错误及纠正方法', '/videos/单人专项动作/拐踢.mp4', '13:25', '王教练', '/files/course/guaiti.png', '中级', 1, NOW(), NULL, b'1', 0);
INSERT INTO `course_info` VALUES (5, '基本技术-控制球', 4, '课程内容介绍', '控制球的基础训练', '/videos/基本技术/控制球.mp4', '18:00', '李教练', '/files/course/kongzhiqiu.png', '初级', 1, NOW(), NULL, b'1', 0);
INSERT INTO `course_info` VALUES (6, '毽球基本技术', 4, '课程内容介绍', '毽球基本技术综合教学', '/videos/基本技术/毽球基本技术.mp4', '20:00', '李教练', '/files/course/jibenchuji.png', '初级', 1, NOW(), NULL, b'1', 0);
