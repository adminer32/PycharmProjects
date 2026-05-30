SET NAMES utf8mb4;

-- 清空旧数据 (可选，根据需要)
-- DELETE FROM `teacher_profile`;
-- DELETE FROM `users` WHERE `username` = 'teacher01';

-- 插入演示用户 (密码均为 123456)
INSERT INTO `users` (`username`, `password_hash`, `password_salt`, `role`, `active`) VALUES 
('teacher01', '462ddb9fa125fdac01fe132e057295c3b8fd1946f394b12c382ec4ab43b25cf5', '1234', 'TEACHER', 1);

-- 获取刚插入的 ID
SET @teacher_user_id = LAST_INSERT_ID();

-- 插入教师档案
INSERT INTO `teacher_profile` (`user_id`, `name`, `avatar`, `email`) VALUES 
(@teacher_user_id, '何小荷', 'https://api.dicebear.com/7.x/avataaars/svg?seed=teacher', 'he@example.com');

-- 插入班级
INSERT INTO `tch_class` (`name`, `teacher_id`, `grade`) VALUES 
('初一(1)班', @teacher_user_id, 7);
