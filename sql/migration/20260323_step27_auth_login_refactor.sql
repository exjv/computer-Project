-- Step27: 登录认证与验证码机制重构补丁
SET NAMES utf8mb4;

-- 登录日志补齐失败原因与检索索引
ALTER TABLE login_log
  ADD COLUMN IF NOT EXISTS fail_reason VARCHAR(255) NULL AFTER login_status,
  ADD KEY IF NOT EXISTS idx_login_log_username_time (username, login_time),
  ADD KEY IF NOT EXISTS idx_login_log_status_time (login_status, login_time);

-- 公告状态默认值与发布索引（门户公告联动）
ALTER TABLE announcement
  ADD KEY IF NOT EXISTS idx_announcement_status_publish_time (status, publish_time);
