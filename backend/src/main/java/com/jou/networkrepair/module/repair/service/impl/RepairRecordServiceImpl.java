package com.jou.networkrepair.module.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.dto.RepairRecordDTO;
import com.jou.networkrepair.module.repair.enums.RepairOrderStatusEnum;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import com.jou.networkrepair.module.repair.service.RepairRecordService;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairRecordServiceImpl implements RepairRecordService {
    private final RepairRecordMapper repairRecordMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;

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
        RepairOrder order = repairOrderMapper.selectById(dto.getRepairOrderId());
        if (order == null) throw new BusinessException("关联工单不存在");
        NetworkDevice device = deviceMapper.selectById(dto.getDeviceId());
        if (device == null) throw new BusinessException("关联设备不存在");

        Long maintainerId = "maintainer".equals(role) ? userId : dto.getMaintainerId();
        SysUser maintainer = userMapper.selectById(maintainerId);
        if (maintainer == null) throw new BusinessException("维修人员不存在");

        RepairRecord record = new RepairRecord();
        record.setRepairOrderId(dto.getRepairOrderId());
        record.setRepairOrderNo(order.getOrderNo());
        record.setDeviceId(dto.getDeviceId());
        record.setDeviceCode(device.getDeviceCode());
        record.setRepairSequence(calcRepairSequence(dto.getDeviceId()));
        record.setMaintenanceSequence(calcMaintenanceSequence(dto.getDeviceId()));
        record.setReportTime(dto.getReportTime() != null ? dto.getReportTime() : order.getReportTime());
        record.setAcceptTime(dto.getAcceptTime() != null ? dto.getAcceptTime() : order.getAcceptTime());
        record.setStartRepairTime(dto.getStartRepairTime() != null ? dto.getStartRepairTime() : order.getStartRepairTime());
        record.setFinishTime(dto.getFinishTime() != null ? dto.getFinishTime() : order.getFinishTime());
        record.setMaintainerId(maintainerId);
        record.setMaintainerEmployeeNo(maintainer.getEmployeeNo());
        record.setMaintainerName(maintainer.getRealName());
        record.setFaultReason(dto.getFaultReason());
        record.setProcessDetail(dto.getProcessDetail());
        record.setFixMeasure(dto.getFixMeasure());
        record.setResultDetail(dto.getResultDetail());
        record.setIsResolved(dto.getIsResolved());
        record.setUsedParts(dto.getUsedParts());
        record.setUsedPartsDesc(dto.getUsedPartsDesc());
        record.setDelayApplied(dto.getDelayApplied());
        record.setDelayReason(dto.getDelayReason());
        record.setLaborHours(dto.getLaborHours());
        record.setRepairConclusion(dto.getRepairConclusion());
        record.setUserConfirmResult(dto.getUserConfirmResult());
        record.setUserSatisfaction(dto.getUserSatisfaction());
        record.setPhotoUrls(dto.getPhotoUrls());
        record.setRemark(dto.getRemark());
        record.setRepairTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        repairRecordMapper.insert(record);

        if (record.getIsResolved() != null && record.getIsResolved() == 1) {
            order.setStatus(RepairOrderStatusEnum.FINISHED.getLabel());
            order.setFinishTime(LocalDateTime.now());
            NetworkDevice dev = new NetworkDevice();
            dev.setId(record.getDeviceId());
            dev.setStatus("正常");
            deviceMapper.updateById(dev);
        } else {
            order.setStatus(RepairOrderStatusEnum.IN_REPAIR.getLabel());
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
        record.setFixMeasure(dto.getFixMeasure());
        record.setResultDetail(dto.getResultDetail());
        record.setIsResolved(dto.getIsResolved());
        record.setUsedParts(dto.getUsedParts());
        record.setUsedPartsDesc(dto.getUsedPartsDesc());
        record.setDelayApplied(dto.getDelayApplied());
        record.setDelayReason(dto.getDelayReason());
        record.setLaborHours(dto.getLaborHours());
        record.setRepairConclusion(dto.getRepairConclusion());
        record.setUserConfirmResult(dto.getUserConfirmResult());
        record.setUserSatisfaction(dto.getUserSatisfaction());
        record.setPhotoUrls(dto.getPhotoUrls());
        record.setRemark(dto.getRemark());
        record.setReportTime(dto.getReportTime());
        record.setAcceptTime(dto.getAcceptTime());
        record.setStartRepairTime(dto.getStartRepairTime());
        record.setFinishTime(dto.getFinishTime());
        record.setUpdateTime(LocalDateTime.now());
        repairRecordMapper.updateById(record);
    }

    @Override
    public void delete(Long id) {
        repairRecordMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> deviceStatistics() {
        List<RepairRecord> all = repairRecordMapper.selectList(new LambdaQueryWrapper<RepairRecord>().orderByDesc(RepairRecord::getId));
        Map<String, Object> data = new HashMap<>();
        Map<Long, Long> frequent = all.stream().collect(Collectors.groupingBy(RepairRecord::getDeviceId, Collectors.counting()));
        data.put("frequentRepairDevices", frequent.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10).collect(Collectors.toList()));

        Map<Long, Double> avgHours = all.stream()
                .filter(r -> r.getStartRepairTime() != null && r.getFinishTime() != null && !r.getFinishTime().isBefore(r.getStartRepairTime()))
                .collect(Collectors.groupingBy(RepairRecord::getDeviceId,
                        Collectors.averagingDouble(r -> java.time.Duration.between(r.getStartRepairTime(), r.getFinishTime()).toHours())));
        data.put("longestAvgRepairDevices", avgHours.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10).collect(Collectors.toList()));

        data.put("nearRetireDevices", frequent.entrySet().stream()
                .filter(e -> e.getValue() >= 8)
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList()));
        return data;
    }

    private Integer calcRepairSequence(Long deviceId) {
        Long count = repairRecordMapper.selectCount(new LambdaQueryWrapper<RepairRecord>().eq(RepairRecord::getDeviceId, deviceId));
        return (count == null ? 0 : count.intValue()) + 1;
    }

    private Integer calcMaintenanceSequence(Long deviceId) {
        Long count = repairRecordMapper.selectCount(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getDeviceId, deviceId)
                .eq(RepairRecord::getIsResolved, 1));
        return (count == null ? 0 : count.intValue()) + 1;
    }
}
