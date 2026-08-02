-- 创建数据库
CREATE DATABASE IF NOT EXISTS `school_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `school_system`;

-- 1. 系统管理相关表

-- 1.1 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `gender` TINYINT(1) DEFAULT NULL COMMENT '性别 0:女 1:男',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.2 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.3 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `permission_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(50) NOT NULL COMMENT '权限编码',
  `url` VARCHAR(200) DEFAULT NULL COMMENT '权限路径',
  `method` VARCHAR(20) DEFAULT NULL COMMENT '请求方法',
  `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父权限ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 1.4 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 1.5 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT(20) NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE CASCADE,
  FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`permission_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 2. 业务相关表

-- 2.1 院系表
CREATE TABLE IF NOT EXISTS `department` (
  `dept_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '院系ID',
  `dept_name` VARCHAR(100) NOT NULL COMMENT '院系名称',
  `dept_code` VARCHAR(50) NOT NULL COMMENT '院系编码',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dept_id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院系表';

-- 2.2 课程表（教师表在其后创建，教师外键在教师表创建后再添加）
CREATE TABLE IF NOT EXISTS `course` (
  `course_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `course_code` VARCHAR(50) NOT NULL COMMENT '课程编码',
  `credits` DECIMAL(3,1) NOT NULL COMMENT '学分',
  `hours` INT(11) NOT NULL COMMENT '课时',
  `dept_id` BIGINT(20) NOT NULL COMMENT '所属院系ID',
  `teacher_id` BIGINT(20) DEFAULT NULL COMMENT '授课教师ID',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '课程描述',
  `audit_status` TINYINT(1) DEFAULT 0 COMMENT '审核状态 0:待审核 1:已通过 2:已拒绝',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态 0:停用 1:启用',
  `capacity` INT NOT NULL DEFAULT 50 COMMENT '课程容量',
  `selected_count` INT NOT NULL DEFAULT 0 COMMENT '当前有效选课人数',
  `version` INT NOT NULL DEFAULT 0 COMMENT '并发更新版本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`course_id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  CONSTRAINT `ck_course_capacity` CHECK (`capacity` > 0),
  CONSTRAINT `ck_course_selected_count` CHECK (`selected_count` >= 0 AND `selected_count` <= `capacity`),
  FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 2.3 教师表
CREATE TABLE IF NOT EXISTS `teacher` (
  `teacher_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '关联用户ID',
  `dept_id` BIGINT(20) NOT NULL COMMENT '所属院系ID',
  `title` VARCHAR(50) DEFAULT NULL COMMENT '职称',
  `specialty` VARCHAR(100) DEFAULT NULL COMMENT '专业',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`teacher_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

ALTER TABLE `course`
  ADD CONSTRAINT `fk_course_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`);

-- 2.4 课程排课表：冲突校验以周次、星期和节次为准，不能以总课时替代。
CREATE TABLE IF NOT EXISTS `course_schedule` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT(20) NOT NULL,
  `day_of_week` TINYINT NOT NULL COMMENT '1=周一，7=周日',
  `start_section` TINYINT NOT NULL,
  `end_section` TINYINT NOT NULL,
  `start_week` TINYINT NOT NULL,
  `end_week` TINYINT NOT NULL,
  `week_type` VARCHAR(8) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/ODD/EVEN',
  `classroom` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_course_schedule_course` (`course_id`),
  UNIQUE KEY `uk_course_schedule_slot` (`course_id`, `day_of_week`, `start_section`, `end_section`, `start_week`, `end_week`, `week_type`),
  CONSTRAINT `ck_schedule_day` CHECK (`day_of_week` BETWEEN 1 AND 7),
  CONSTRAINT `ck_schedule_section` CHECK (`start_section` <= `end_section`),
  CONSTRAINT `ck_schedule_week` CHECK (`start_week` <= `end_week`),
  CONSTRAINT `ck_schedule_type` CHECK (`week_type` IN ('ALL', 'ODD', 'EVEN')),
  FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程排课表';

-- 2.5 学生表
CREATE TABLE IF NOT EXISTS `student` (
  `student_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '关联用户ID',
  `dept_id` BIGINT(20) NOT NULL COMMENT '所属院系ID',
  `student_no` VARCHAR(20) NOT NULL COMMENT '学号',
  `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
  `class` VARCHAR(50) DEFAULT NULL COMMENT '班级',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`student_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- 2.6 选课表
CREATE TABLE IF NOT EXISTS `course_selection` (
  `selection_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '选课ID',
  `student_id` BIGINT(20) NOT NULL COMMENT '学生ID',
  `course_id` BIGINT(20) NOT NULL COMMENT '课程ID',
  `semester` VARCHAR(20) NOT NULL COMMENT '学期',
  `year` VARCHAR(10) NOT NULL COMMENT '学年',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态 0:取消 1:正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`selection_id`),
  UNIQUE KEY `uk_student_course` (`student_id`, `course_id`, `semester`, `year`),
  FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课表';

-- 2.7 成绩表
CREATE TABLE IF NOT EXISTS `score` (
  `score_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
  `selection_id` BIGINT(20) NOT NULL COMMENT '选课ID',
  `teacher_id` BIGINT(20) NOT NULL COMMENT '评分教师ID',
  `usual_score` DECIMAL(5,2) DEFAULT 0 COMMENT '平时成绩',
  `final_score` DECIMAL(5,2) DEFAULT 0 COMMENT '期末成绩',
  `total_score` DECIMAL(5,2) DEFAULT 0 COMMENT '总评成绩',
  `status` TINYINT(1) DEFAULT 0 COMMENT '状态 0:未评定 1:已评定',
  `comment` VARCHAR(200) DEFAULT NULL COMMENT '评语',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`score_id`),
  UNIQUE KEY `uk_selection` (`selection_id`),
  CONSTRAINT `ck_usual_score` CHECK (`usual_score` BETWEEN 0 AND 100),
  CONSTRAINT `ck_final_score` CHECK (`final_score` BETWEEN 0 AND 100),
  FOREIGN KEY (`selection_id`) REFERENCES `course_selection` (`selection_id`) ON DELETE CASCADE,
  FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

-- 初始化角色数据
INSERT IGNORE INTO sys_role (role_name, role_code, description) VALUES 
('学生', 'STUDENT', '学生角色'),
('教师', 'TEACHER', '教师角色'),
('教务管理员', 'ADMIN', '教务管理员角色');

-- 初始化权限数据
INSERT IGNORE INTO sys_permission (permission_name, permission_code, url, method, parent_id) VALUES 
('用户管理', 'user:manage', '/api/user/**', 'GET,POST,PUT,DELETE', 0),
('课程管理', 'course:manage', '/api/course/**', 'GET,POST,PUT,DELETE', 0),
('选课管理', 'selection:manage', '/api/selection/**', 'GET,POST,PUT,DELETE', 0),
('成绩管理', 'score:manage', '/api/score/**', 'GET,POST,PUT,DELETE', 0),
('课程发布', 'course:add', '/api/course/add', 'POST', 2),
('课程审核', 'course:audit', '/api/course/approve/**', 'POST', 2),
('成绩录入', 'score:add', '/api/score/add', 'POST', 4),
('成绩导出', 'score:export', '/api/score/export/**', 'GET', 4);

-- 初始化角色权限关联数据
INSERT INTO sys_role_permission (role_id, permission_id) VALUES 
-- 学生权限
(1, 3),
(1, 4),
-- 教师权限
(2, 2),
(2, 3),
(2, 4),
(2, 5),
(2, 7),
(2, 8),
-- 教务管理员权限
(3, 1),
(3, 2),
(3, 3),
(3, 4),
(3, 5),
(3, 6),
(3, 7),
(3, 8);

-- 初始化院系数据
INSERT INTO department (dept_name, dept_code, description) VALUES 
('计算机科学与技术学院', 'CS', '计算机科学与技术学院'),
('软件学院', 'SE', '软件学院'),
('信息工程学院', 'IE', '信息工程学院'),
('数学与统计学院', 'MATH', '数学与统计学院'),
('外国语学院', 'FL', '外国语学院');

-- 初始化管理员用户
INSERT IGNORE INTO sys_user (username, password, real_name, gender, phone, email, status) VALUES 
('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', '系统管理员', 1, '13800138000', 'admin@school.edu', 1);

-- 初始化学生用户
INSERT IGNORE INTO sys_user (username, password, real_name, gender, phone, email, status) VALUES 
('student', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '学生用户', 1, '13800138001', 'student@school.edu', 1),
('teacher', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '教师用户', 1, '13800138002', 'teacher@school.edu', 1);

-- 关联管理员角色
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 3);

-- 关联学生角色
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES 
((SELECT user_id FROM sys_user WHERE username = 'student'), 1),
((SELECT user_id FROM sys_user WHERE username = 'teacher'), 2);

-- 初始化教师信息
INSERT IGNORE INTO teacher (user_id, dept_id, title, specialty) VALUES 
((SELECT user_id FROM sys_user WHERE username = 'teacher'), 1, '讲师', '计算机科学与技术');

-- 初始化学生信息
INSERT IGNORE INTO student (user_id, dept_id, student_no, grade, class) VALUES 
((SELECT user_id FROM sys_user WHERE username = 'student'), 1, '20220001', '2022级', '计算机1班');

-- 初始化课程数据
INSERT IGNORE INTO course (course_name, course_code, credits, hours, dept_id, audit_status, status) VALUES 
('高等数学', 'MATH101', 4, 64, 4, 1, 1),
('线性代数', 'MATH102', 3, 48, 4, 1, 1),
('大学英语', 'ENG101', 3, 48, 5, 1, 1),
('计算机科学导论', 'CS101', 3, 48, 1, 1, 1),
('数据结构', 'CS201', 4, 64, 1, 1, 1),
('操作系统', 'CS301', 4, 64, 1, 1, 1),
('数据库系统', 'CS302', 3, 48, 1, 1, 1),
('软件工程', 'SE101', 3, 48, 2, 1, 1);

-- 演示排课数据；不同的总课时不代表不冲突，真正依据是下列排课时段。
INSERT IGNORE INTO course_schedule (course_id, day_of_week, start_section, end_section, start_week, end_week, week_type, classroom) VALUES
((SELECT course_id FROM course WHERE course_code = 'CS101'), 1, 1, 2, 1, 16, 'ALL', 'A101'),
((SELECT course_id FROM course WHERE course_code = 'CS201'), 3, 3, 4, 1, 16, 'ALL', 'A102'),
((SELECT course_id FROM course WHERE course_code = 'MATH101'), 2, 1, 2, 1, 16, 'ALL', 'B201'),
((SELECT course_id FROM course WHERE course_code = 'MATH102'), 2, 1, 2, 1, 16, 'ODD', 'B202'),
((SELECT course_id FROM course WHERE course_code = 'ENG101'), 2, 1, 2, 1, 16, 'EVEN', 'C301'),
((SELECT course_id FROM course WHERE course_code = 'CS302'), 5, 5, 6, 1, 16, 'ALL', 'A103'),
((SELECT course_id FROM course WHERE course_code = 'SE101'), 4, 3, 4, 1, 16, 'ALL', 'D401'),
((SELECT course_id FROM course WHERE course_code = 'CS301'), 3, 3, 4, 1, 16, 'ALL', 'A104');

-- 初始化选课记录（学生已选择的课程，用于测试）
INSERT IGNORE INTO course_selection (student_id, course_id, semester, year, status) VALUES 
((SELECT student_id FROM student WHERE student_no = '20220001'), 
 (SELECT course_id FROM course WHERE course_code = 'CS101'), '春季学期', '2025-2026', 1),
((SELECT student_id FROM student WHERE student_no = '20220001'), 
 (SELECT course_id FROM course WHERE course_code = 'CS201'), '春季学期', '2025-2026', 1),
((SELECT student_id FROM student WHERE student_no = '20220001'), 
 (SELECT course_id FROM course WHERE course_code = 'MATH101'), '春季学期', '2025-2026', 1);

-- 初始化记录完成后同步课程已选人数，保证 selected_count 与有效选课记录一致。
UPDATE course c
SET selected_count = (SELECT COUNT(*) FROM course_selection cs WHERE cs.course_id = c.course_id AND cs.status = 1);

-- 初始化成绩数据（教师已录入的成绩）
INSERT IGNORE INTO score (selection_id, teacher_id, usual_score, final_score, total_score, status, comment) VALUES 
((SELECT cs.selection_id FROM course_selection cs JOIN student s ON cs.student_id = s.student_id 
  JOIN course c ON cs.course_id = c.course_id WHERE s.student_no = '20220001' AND c.course_code = 'CS101'),
 (SELECT teacher_id FROM teacher WHERE user_id = (SELECT user_id FROM sys_user WHERE username = 'teacher')),
 85.00, 90.00, 87.50, 1, '学习认真，成绩优秀'),
((SELECT cs.selection_id FROM course_selection cs JOIN student s ON cs.student_id = s.student_id 
  JOIN course c ON cs.course_id = c.course_id WHERE s.student_no = '20220001' AND c.course_code = 'CS201'),
 (SELECT teacher_id FROM teacher WHERE user_id = (SELECT user_id FROM sys_user WHERE username = 'teacher')),
 78.00, 82.00, 80.80, 1, '基础扎实，需加强实践'),
((SELECT cs.selection_id FROM course_selection cs JOIN student s ON cs.student_id = s.student_id 
  JOIN course c ON cs.course_id = c.course_id WHERE s.student_no = '20220001' AND c.course_code = 'MATH101'),
 (SELECT teacher_id FROM teacher WHERE user_id = (SELECT user_id FROM sys_user WHERE username = 'teacher')),
 92.00, 88.00, 89.20, 1, '逻辑思维强，表现优异');

-- 数据库升级：为已有course表添加audit_status字段
-- ALTER TABLE course ADD COLUMN audit_status TINYINT(1) DEFAULT 0 COMMENT '审核状态 0:待审核 1:已通过 2:已拒绝' AFTER description;
-- UPDATE course SET audit_status = 1 WHERE status = 1;
-- UPDATE course SET audit_status = 0 WHERE status = 0;
-- UPDATE course SET audit_status = 2 WHERE status = 2;
-- UPDATE course SET audit_status = 1 WHERE status = -1;
