package com.jou.networkrepair.module.repair.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.module.repair.dto.RepairRecordDTO;
import com.jou.networkrepair.module.repair.entity.RepairRecord;

public interface RepairRecordService {
    Page<RepairRecord> page(Long current, Long size, Long repairOrderId, Long deviceId, Long maintainerId, Integer isResolved, Long userId, String role);
    RepairRecord detail(Long id, Long userId, String role);
    void create(RepairRecordDTO dto, Long userId, String role);
    void update(Long id, RepairRecordDTO dto, Long userId, String role);
    void delete(Long id);
}
