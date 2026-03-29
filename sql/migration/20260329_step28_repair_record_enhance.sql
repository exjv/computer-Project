-- 第17步：强化维修记录模块
ALTER TABLE repair_record
  ADD COLUMN IF NOT EXISTS maintenance_count_no INT NOT NULL DEFAULT 1 AFTER repair_count_no,
  ADD COLUMN IF NOT EXISTS report_time DATETIME NULL AFTER maintenance_count_no,
  ADD COLUMN IF NOT EXISTS accept_time DATETIME NULL AFTER report_time,
  ADD COLUMN IF NOT EXISTS start_repair_time DATETIME NULL AFTER accept_time,
  ADD COLUMN IF NOT EXISTS finish_time DATETIME NULL AFTER start_repair_time,
  ADD COLUMN IF NOT EXISTS fix_measure TEXT NULL AFTER process_detail,
  ADD COLUMN IF NOT EXISTS delay_applied TINYINT NOT NULL DEFAULT 0 AFTER used_parts_desc,
  ADD COLUMN IF NOT EXISTS delay_reason VARCHAR(500) NULL AFTER delay_applied,
  ADD COLUMN IF NOT EXISTS user_confirm_result VARCHAR(50) NULL AFTER repair_conclusion,
  ADD COLUMN IF NOT EXISTS user_satisfaction INT NULL AFTER user_confirm_result,
  ADD COLUMN IF NOT EXISTS photo_urls TEXT NULL AFTER user_satisfaction,
  ADD COLUMN IF NOT EXISTS remark VARCHAR(500) NULL AFTER photo_urls,
  ADD COLUMN IF NOT EXISTS create_by BIGINT NULL AFTER remark,
  ADD COLUMN IF NOT EXISTS update_by BIGINT NULL AFTER create_by,
  ADD KEY IF NOT EXISTS idx_repair_record_device_finish (device_id, finish_time),
  ADD KEY IF NOT EXISTS idx_repair_record_report_time (report_time);
