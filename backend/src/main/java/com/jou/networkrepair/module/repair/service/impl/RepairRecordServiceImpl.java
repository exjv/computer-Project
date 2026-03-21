package com.jou.networkrepair.module.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.dto.RepairRecordDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import com.jou.networkrepair.module.repair.service.RepairRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepairRecordServiceImpl implements RepairRecordService {
    private final RepairRecordMapper repairRecordMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;

    @Override
    public Page<RepairRecord> page(Long current, Long size, Long repairOrderId, Long deviceId, Long maintainerId, Integer isResolved, Long userId, String role) {
        LambdaQueryWrapper<RepairRecord> qw = new LambdaQueryWrapper<RepairRecord>()
                .eq(repairOrderId != null, RepairRecord::getRepairOrderId, repairOrderId)
                .eq(deviceId != null, RepairRecord::getDeviceId, deviceId)
                .eq(maintainerId != null, RepairRecord::getMaintainerId, maintainerId)
                .eq(isResolved != null, RepairRecord::getIsResolved, isResolved)
                .orderByDesc(RepairRecord::getId);
        if ("maintainer".equals(role)) qw.eq(RepairRecord::getMaintainerId, userId);
        if ("user".equals(role)) {
            List<RepairOrder> myOrders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getReporterId, userId).select(RepairOrder::getId));
            List<Long> orderIds = new ArrayList<>();
            for (RepairOrder order : myOrders) orderIds.add(order.getId());
            if (orderIds.isEmpty()) return new Page<>(current, size);
            qw.in(RepairRecord::getRepairOrderId, orderIds);
        }
        return repairRecordMapper.selectPage(new Page<>(current, size), qw);
    }

    @Override
    public RepairRecord detail(Long id, Long userId, String role) {
        RepairRecord record = repairRecordMapper.selectById(id);
        if (record == null) throw new BusinessException("维修记录不存在");
        if ("maintainer".equals(role) && !userId.equals(record.getMaintainerId())) throw new BusinessException("无权查看");
        if ("user".equals(role)) {
            Long cnt = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getId, record.getRepairOrderId())
                    .eq(RepairOrder::getReporterId, userId));
            if (cnt == null || cnt == 0L) throw new BusinessException("无权查看");
        }
        return record;
    }

    @Override
    public void create(RepairRecordDTO dto, Long userId, String role) {
        RepairRecord record = new RepairRecord();
        record.setRepairOrderId(dto.getRepairOrderId());
        record.setDeviceId(dto.getDeviceId());
        record.setMaintainerId("maintainer".equals(role) ? userId : dto.getMaintainerId());
        record.setFaultReason(dto.getFaultReason());
        record.setProcessDetail(dto.getProcessDetail());
        record.setResultDetail(dto.getResultDetail());
        record.setIsResolved(dto.getIsResolved());
        record.setRepairTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        repairRecordMapper.insert(record);

        RepairOrder order = repairOrderMapper.selectById(record.getRepairOrderId());
        if (order == null) throw new BusinessException("关联工单不存在");
        if (record.getIsResolved() != null && record.getIsResolved() == 1) {
            order.setStatus("已完成");
            order.setFinishTime(LocalDateTime.now());
            NetworkDevice dev = new NetworkDevice();
            dev.setId(record.getDeviceId());
            dev.setStatus("正常");
            deviceMapper.updateById(dev);
        } else {
            order.setStatus("处理中");
        }
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
    }

    @Override
    public void update(Long id, RepairRecordDTO dto, Long userId, String role) {
        RepairRecord old = repairRecordMapper.selectById(id);
        if (old == null) throw new BusinessException("维修记录不存在");
        if ("maintainer".equals(role) && !userId.equals(old.getMaintainerId())) throw new BusinessException("仅可修改本人维修记录");
        RepairRecord record = new RepairRecord();
        record.setId(id);
        record.setRepairOrderId(dto.getRepairOrderId());
        record.setDeviceId(dto.getDeviceId());
        record.setMaintainerId(dto.getMaintainerId() == null ? old.getMaintainerId() : dto.getMaintainerId());
        record.setFaultReason(dto.getFaultReason());
        record.setProcessDetail(dto.getProcessDetail());
        record.setResultDetail(dto.getResultDetail());
        record.setIsResolved(dto.getIsResolved());
        record.setUpdateTime(LocalDateTime.now());
        repairRecordMapper.updateById(record);
    }

    @Override
    public void delete(Long id) {
        repairRecordMapper.deleteById(id);
    }
}
