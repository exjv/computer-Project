USE network_repair;

ALTER TABLE network_device
  ADD COLUMN IF NOT EXISTS brand VARCHAR(100) NULL COMMENT '品牌' AFTER device_type,
  ADD COLUMN IF NOT EXISTS model VARCHAR(100) NULL COMMENT '型号' AFTER brand,
  ADD COLUMN IF NOT EXISTS serial_number VARCHAR(100) NULL COMMENT '序列号' AFTER model,
  ADD COLUMN IF NOT EXISTS campus VARCHAR(100) NULL COMMENT '所属校区' AFTER serial_number,
  ADD COLUMN IF NOT EXISTS building_location VARCHAR(150) NULL COMMENT '楼宇/机房/办公室' AFTER campus,
  ADD COLUMN IF NOT EXISTS enable_date DATE NULL COMMENT '启用时间' AFTER purchase_date,
  ADD COLUMN IF NOT EXISTS warranty_expiry_date DATE NULL COMMENT '保修截止时间' AFTER enable_date,
  ADD COLUMN IF NOT EXISTS owner_name VARCHAR(50) NULL COMMENT '责任人' AFTER warranty_expiry_date,
  ADD COLUMN IF NOT EXISTS manage_department VARCHAR(100) NULL COMMENT '管理部门' AFTER owner_name,
  ADD COLUMN IF NOT EXISTS last_fault_time DATETIME NULL COMMENT '最近故障时间' AFTER status,
  ADD COLUMN IF NOT EXISTS total_repair_requests INT DEFAULT 0 COMMENT '累计报修次数' AFTER last_fault_time,
  ADD COLUMN IF NOT EXISTS total_repair_count INT DEFAULT 0 COMMENT '累计维修次数' AFTER total_repair_requests,
  ADD COLUMN IF NOT EXISTS fault_reason_stats VARCHAR(500) NULL COMMENT '历史故障原因统计' AFTER total_repair_count,
  ADD COLUMN IF NOT EXISTS repair_approval_required TINYINT DEFAULT 0 COMMENT '是否需要管理员审核(1是0否)' AFTER fault_reason_stats;

UPDATE network_device
SET brand = IFNULL(brand, SUBSTRING_INDEX(brand_model, '/', 1)),
    model = IFNULL(model, SUBSTRING_INDEX(brand_model, '/', -1)),
    building_location = IFNULL(building_location, location),
    total_repair_requests = IFNULL(total_repair_requests, 0),
    total_repair_count = IFNULL(total_repair_count, 0),
    repair_approval_required = IFNULL(repair_approval_required, 0);
