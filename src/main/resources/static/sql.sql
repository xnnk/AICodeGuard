/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80031 (8.0.31)
 Source Host           : localhost:3306
 Source Schema         : ai-code-guard

 Target Server Type    : MySQL
 Target Server Version : 80031 (8.0.31)
 File Encoding         : 65001

 Date: 29/04/2025 09:56:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for detection_task
-- ----------------------------
DROP TABLE IF EXISTS `detection_task`;
CREATE TABLE `detection_task`  (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '任务ID（UUID）',
  `code_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '关联的代码ID',
  `status` enum('PENDING','SUCCESS','FAILED') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'PENDING' COMMENT '任务状态',
  `start_time` datetime NOT NULL COMMENT '任务开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '任务结束时间',
  `error_message` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '失败时的错误信息',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `code_id`(`code_id` ASC) USING BTREE,
  CONSTRAINT `detection_task_ibfk_1` FOREIGN KEY (`code_id`) REFERENCES `generated_code` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '漏洞检测任务记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of detection_task
-- ----------------------------
INSERT INTO `detection_task` VALUES ('0a0b78fc-c3f8-4d9b-b927-f38c02f45e79', '1969a899-a4cc-4833-b071-867f174e2b5c', 'SUCCESS', '2025-04-28 17:13:28', '2025-04-28 17:13:36', NULL);
INSERT INTO `detection_task` VALUES ('13c45cb9-413c-45ed-9452-48f0ece8be2e', '716579cc-62cc-4682-96c0-ff72b3e1e3c7', 'PENDING', '2025-04-28 17:40:42', NULL, NULL);
INSERT INTO `detection_task` VALUES ('5aa0a9f9-3956-4f03-82f6-c7e0aebb0180', '716579cc-62cc-4682-96c0-ff72b3e1e3c7', 'SUCCESS', '2025-04-28 17:40:42', '2025-04-28 17:40:52', NULL);
INSERT INTO `detection_task` VALUES ('68bd7aa8-74f8-49eb-8eb6-d28d569ff0d0', '9651f89b-7fcc-4d49-a6e6-527890f3fd90', 'SUCCESS', '2025-04-28 17:47:03', '2025-04-28 17:47:11', NULL);
INSERT INTO `detection_task` VALUES ('76b31fbd-137b-4312-8eb6-6186706bae98', '76e562e8-e249-4187-aa3a-66dc41523e1f', 'FAILED', '2025-04-28 17:36:43', '2025-04-28 17:36:48', '调用Claude模型生成代码失败: 调用Claude API失败: Connection reset');
INSERT INTO `detection_task` VALUES ('8e71c406-aca5-406f-bba9-75d1c003cb3c', '4410bc1c-985a-4bc2-8303-d12d741ccf68', 'PENDING', '2025-04-28 17:00:00', NULL, NULL);
INSERT INTO `detection_task` VALUES ('c32d70cf-73e1-4a9e-b160-d3b11f82f8c9', '9651f89b-7fcc-4d49-a6e6-527890f3fd90', 'PENDING', '2025-04-28 17:47:03', NULL, NULL);
INSERT INTO `detection_task` VALUES ('d3aa1367-4c1d-4e28-887a-bc0997786691', '07a31dc6-a656-41f0-b04b-0bd7d1f800bf', 'PENDING', '2025-04-28 17:02:22', NULL, NULL);
INSERT INTO `detection_task` VALUES ('e2499ccf-c79f-4cbe-be43-683093c851f0', '8a64a370-08bc-4152-90fd-2e74308dfe61', 'PENDING', '2025-04-28 16:44:35', NULL, NULL);
INSERT INTO `detection_task` VALUES ('f6ff160e-f26c-41f3-87f6-8380100fe4d2', '76e562e8-e249-4187-aa3a-66dc41523e1f', 'PENDING', '2025-04-28 17:36:43', NULL, NULL);

-- ----------------------------
-- Table structure for generated_code
-- ----------------------------
DROP TABLE IF EXISTS `generated_code`;
CREATE TABLE `generated_code`  (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '代码ID（UUID）',
  `user_id` int NOT NULL COMMENT '生成代码的用户ID',
  `prompt` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户输入的自然语言需求',
  `language` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '编程语言（如Java、Python）',
  `created_at` datetime NOT NULL COMMENT '生成时间',
  `ai_model` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '调用的AI模型（如DeepSeek-Coder）',
  `scan_status` enum('PENDING','SUCCESS','FAILED') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'PENDING' COMMENT '检测状态',
  `scan_time` datetime NULL DEFAULT NULL COMMENT '检测完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `generated_code_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '代码生成记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of generated_code
-- ----------------------------
INSERT INTO `generated_code` VALUES ('07a31dc6-a656-41f0-b04b-0bd7d1f800bf', 1, '我想写一个能使用加减乘除的计算机程序。', 'python', '2025-04-28 17:02:22', 'deepseek', 'PENDING', NULL);
INSERT INTO `generated_code` VALUES ('1969a899-a4cc-4833-b071-867f174e2b5c', 1, '我想写一个能使用加减乘除的计算机程序。', 'python', '2025-04-28 17:13:28', 'deepseek', 'PENDING', NULL);
INSERT INTO `generated_code` VALUES ('4410bc1c-985a-4bc2-8303-d12d741ccf68', 1, '我想写一个能使用加减乘除的计算机程序。', 'python', '2025-04-28 16:59:59', 'deepseek', 'PENDING', NULL);
INSERT INTO `generated_code` VALUES ('716579cc-62cc-4682-96c0-ff72b3e1e3c7', 1, '5我想写一个能使用加减乘除的计算机程序。', 'python', '2025-04-28 17:40:42', 'deepseek', 'SUCCESS', '2025-04-28 17:40:42');
INSERT INTO `generated_code` VALUES ('76e562e8-e249-4187-aa3a-66dc41523e1f', 1, '2我想写一个能使用加减乘除的计算机程序。', 'python', '2025-04-28 17:36:43', 'deepseek', 'SUCCESS', '2025-04-28 17:36:43');
INSERT INTO `generated_code` VALUES ('8a64a370-08bc-4152-90fd-2e74308dfe61', 1, '你好，这是一个接口测试，我想写一个能使用加减乘除的计算机程序。', 'python', '2025-04-28 16:44:35', 'deepseek', 'PENDING', NULL);
INSERT INTO `generated_code` VALUES ('9651f89b-7fcc-4d49-a6e6-527890f3fd90', 1, '6我想写一个能使用加减乘除的计算机程序。', 'python', '2025-04-28 17:47:03', 'deepseek', 'PENDING', NULL);
INSERT INTO `generated_code` VALUES ('ac6757cf-722e-45c5-a017-88f35b013916', 1, '你好，这是一个测试，我想写一个使用Python写的HelloWorld程序，顺便说一下这个prompt好用吗', 'python', '2025-04-24 11:39:08', 'deepseek', 'PENDING', NULL);

-- ----------------------------
-- Table structure for sys_resource
-- ----------------------------
DROP TABLE IF EXISTS `sys_resource`;
CREATE TABLE `sys_resource`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `parentId` int NULL DEFAULT NULL COMMENT '资源父id',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '资源名称',
  `perms` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '权限标识符',
  `type` tinyint NULL DEFAULT NULL COMMENT '类型 0:目录 1:菜单 2:按钮',
  `parent_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '资源' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_resource
-- ----------------------------
INSERT INTO `sys_resource` VALUES (1, NULL, '用户管理', 'sys:role', 0, NULL);
INSERT INTO `sys_resource` VALUES (2, NULL, '角色管理', 'sys:user', 0, NULL);
INSERT INTO `sys_resource` VALUES (3, 1, '查看用户', 'sys:user:view', 2, NULL);
INSERT INTO `sys_resource` VALUES (4, 1, '编辑用户', 'sys:user:edit', 2, NULL);
INSERT INTO `sys_resource` VALUES (5, 1, '删除用户', 'sys:user:delete', 2, NULL);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` int NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '角色名',
  `grade` int NOT NULL COMMENT '角色层级',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'admin', 1, '系统超级管理员，权限全开');
INSERT INTO `sys_role` VALUES (2, 'user', 3, '普通用户，权限全闭');

-- ----------------------------
-- Table structure for sys_role_resource
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_resource`;
CREATE TABLE `sys_role_resource`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL COMMENT '角色id',
  `resource_id` int NOT NULL COMMENT '资源id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色资源关系' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_resource
-- ----------------------------
INSERT INTO `sys_role_resource` VALUES (1, 1, 1);
INSERT INTO `sys_role_resource` VALUES (2, 1, 2);
INSERT INTO `sys_role_resource` VALUES (3, 1, 3);
INSERT INTO `sys_role_resource` VALUES (4, 1, 4);
INSERT INTO `sys_role_resource` VALUES (5, 1, 5);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `account` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `salt` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '盐',
  `forbidden` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '是否禁用 1:是 0:否',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', 'admin', '35f8a398dc8ac10a567e76a11af2ba3ffc9078eeeab1a0dd9199d641c3b3c1ba', 'admin123', '0');
INSERT INTO `sys_user` VALUES (2, 'xnnk', 'xnnk', '0f4ead87728c223ef79122ea4cc300cc2c03d2cacd7b1612c92aebc0b6e07f99', 'cae7bf9e7767400fa08d0b7adb87e9ad', '0');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户id',
  `role_id` int NOT NULL COMMENT '角色id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户角色关系' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2, 2);

SET FOREIGN_KEY_CHECKS = 1;
