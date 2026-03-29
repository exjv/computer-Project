package com.jou.networkrepair.module.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.dto.RepairRecordDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.enums.RepairOrderStatusEnum;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import com.jou.networkrepair.module.repair.service.RepairRecordService;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
            List<RepairOrder> myOrders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getReporterId, userId)
                    .select(RepairOrder::getId));
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
        if (maintainerId == null) throw new BusinessException("维修人员不能为空");
        SysUser maintainer = userMapper.selectById(maintainerId);
        if (maintainer == null) throw new BusinessException("维修人员不存在");

        RepairRecord record = new RepairRecord();
        fillRecordByOrder(record, order, device, maintainer);
        fillRecordByDto(record, dto);

        record.setRepairSequence(calcReportSequence(dto.getDeviceId()));
        record.setMaintenanceSequence(calcMaintenanceSequence(dto.getDeviceId()));
        if (record.getLaborHours() == null) {
            record.setLaborHours(calcLaborHours(record.getStartRepairTime(), record.getFinishTime()));
        }

        record.setRepairTime(record.getFinishTime() != null ? record.getFinishTime() : LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        repairRecordMapper.insert(record);

        syncOrderAndDeviceStatus(order, record);
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
        record.setFaultReason(dto.getFaultReason());
        record.setProcessDetail(dto.getProcessDetail());
        record.setFixMeasure(dto.getFixMeasure());
        record.setResultDetail(dto.getResultDetail());
        record.setIsResolved(dto.getIsResolved());
        record.setUsedParts(dto.getUsedParts());
        record.setUsedPartsDesc(dto.getUsedPartsDesc());
        record.setDelayApplied(dto.getDelayApplied());
        record.setDelayReason(dto.getDelayReason());
        record.setLaborHours(dto.getLaborHours() != null ? dto.getLaborHours() : calcLaborHours(dto.getStartRepairTime(), dto.getFinishTime()));
        record.setRepairConclusion(dto.getRepairConclusion());
        record.setUserConfirmResult(dto.getUserConfirmResult());
        record.setUserSatisfaction(dto.getUserSatisfaction());
        record.setPhotoUrls(dto.getPhotoUrls());
        record.setRemark(dto.getRemark());
        record.setReportTime(dto.getReportTime());
        record.setAcceptTime(dto.getAcceptTime());
        record.setStartRepairTime(dto.getStartRepairTime());
        record.setFinishTime(dto.getFinishTime());
        record.setRepairTime(dto.getFinishTime() != null ? dto.getFinishTime() : old.getRepairTime());
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

        Map<Long, NetworkDevice> deviceMap = deviceMapper.selectList(null).stream()
                .collect(Collectors.toMap(NetworkDevice::getId, d -> d, (a, b) -> a));

        Map<String, Object> data = new HashMap<>();

        Map<Long, Long> frequent = all.stream().collect(Collectors.groupingBy(RepairRecord::getDeviceId, Collectors.counting()));
        data.put("frequentRepairDevices", frequent.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> toDeviceStatRow(deviceMap.get(e.getKey()), e.getValue(), null, null))
                .collect(Collectors.toList()));

        Map<Long, Double> avgHours = all.stream()
                .filter(r -> r.getStartRepairTime() != null && r.getFinishTime() != null && !r.getFinishTime().isBefore(r.getStartRepairTime()))
                .collect(Collectors.groupingBy(RepairRecord::getDeviceId,
                        Collectors.averagingDouble(r -> Duration.between(r.getStartRepairTime(), r.getFinishTime()).toMinutes() / 60.0)));
        data.put("longestAvgRepairDevices", avgHours.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> toDeviceStatRow(deviceMap.get(e.getKey()), null, round(e.getValue()), null))
                .collect(Collectors.toList()));

        LocalDate border = LocalDate.now().minusYears(6);
        data.put("nearRetireDevices", frequent.entrySet().stream()
                .filter(e -> e.getValue() >= 8 || maybeNearRetire(deviceMap.get(e.getKey()), border))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> toDeviceStatRow(deviceMap.get(e.getKey()), e.getValue(), avgHours.get(e.getKey()) == null ? null : round(avgHours.get(e.getKey())), "高频报修或超龄设备"))
                .collect(Collectors.toList()));

        return data;
    }

    private Map<String, Object> toDeviceStatRow(NetworkDevice d, Long repairCount, Double avgHours, String advice) {
        Map<String, Object> m = new HashMap<>();
        m.put("deviceId", d == null ? null : d.getId());
        m.put("deviceCode", d == null ? null : d.getDeviceCode());
        m.put("deviceName", d == null ? null : d.getDeviceName());
        m.put("deviceType", d == null ? null : d.getDeviceType());
        m.put("campus", d == null ? null : d.getCampus());
        m.put("repairCount", repairCount);
        m.put("avgRepairHours", avgHours);
        m.put("advice", advice);
        return m;
    }

    private boolean maybeNearRetire(NetworkDevice d, LocalDate border) {
        if (d == null) return false;
        return d.getPurchaseDate() != null && d.getPurchaseDate().isBefore(border);
    }

    private void fillRecordByOrder(RepairRecord record, RepairOrder order, NetworkDevice device, SysUser maintainer) {
        record.setRepairOrderId(order.getId());
        record.setRepairOrderNo(order.getOrderNo());
        record.setDeviceId(device.getId());
        record.setDeviceCode(device.getDeviceCode());
        record.setReportTime(order.getReportTime());
        record.setAcceptTime(order.getAcceptTime());
        record.setStartRepairTime(order.getStartRepairTime());
        record.setFinishTime(order.getFinishTime());
        record.setMaintainerId(maintainer.getId());
        record.setMaintainerEmployeeNo(maintainer.getEmployeeNo());
        record.setMaintainerName(maintainer.getRealName());
        record.setFaultReason(order.getFaultType());
        record.setProcessDetail(order.getDescription());
        record.setFixMeasure(order.getHandleDescription());
        record.setResultDetail(order.getHandleDescription());
        record.setUsedParts(order.getNeedPurchaseParts());
        record.setUsedPartsDesc(order.getPartsDescription());
        record.setDelayApplied(order.getApplyDelay());
        record.setDelayReason(order.getDelayReason());
        record.setUserConfirmResult(order.getUserConfirmResult());
        record.setUserSatisfaction(order.getSatisfactionScore());
        record.setRemark(order.getRemark());
        record.setIsResolved(1);
        record.setRepairConclusion("维修完工，待用户确认");
    }

    private void fillRecordByDto(RepairRecord record, RepairRecordDTO dto) {
        if (dto.getReportTime() != null) record.setReportTime(dto.getReportTime());
        if (dto.getAcceptTime() != null) record.setAcceptTime(dto.getAcceptTime());
        if (dto.getStartRepairTime() != null) record.setStartRepairTime(dto.getStartRepairTime());
        if (dto.getFinishTime() != null) record.setFinishTime(dto.getFinishTime());
        if (dto.getFaultReason() != null) record.setFaultReason(dto.getFaultReason());
        if (dto.getProcessDetail() != null) record.setProcessDetail(dto.getProcessDetail());
        if (dto.getFixMeasure() != null) record.setFixMeasure(dto.getFixMeasure());
        if (dto.getResultDetail() != null) record.setResultDetail(dto.getResultDetail());
        if (dto.getIsResolved() != null) record.setIsResolved(dto.getIsResolved());
        if (dto.getUsedParts() != null) record.setUsedParts(dto.getUsedParts());
        if (dto.getUsedPartsDesc() != null) record.setUsedPartsDesc(dto.getUsedPartsDesc());
        if (dto.getDelayApplied() != null) record.setDelayApplied(dto.getDelayApplied());
        if (dto.getDelayReason() != null) record.setDelayReason(dto.getDelayReason());
        if (dto.getLaborHours() != null) record.setLaborHours(dto.getLaborHours());
        if (dto.getRepairConclusion() != null) record.setRepairConclusion(dto.getRepairConclusion());
        if (dto.getUserConfirmResult() != null) record.setUserConfirmResult(dto.getUserConfirmResult());
        if (dto.getUserSatisfaction() != null) record.setUserSatisfaction(dto.getUserSatisfaction());
        if (dto.getPhotoUrls() != null) record.setPhotoUrls(dto.getPhotoUrls());
        if (dto.getRemark() != null) record.setRemark(dto.getRemark());
    }

    private void syncOrderAndDeviceStatus(RepairOrder order, RepairRecord record) {
        if (record.getIsResolved() != null && record.getIsResolved() == 1) {
            order.setStatus(RepairOrderStatusEnum.COMPLETED.getLabel());
            order.setFinishTime(record.getFinishTime() != null ? record.getFinishTime() : LocalDateTime.now());
            NetworkDevice dev = new NetworkDevice();
            dev.setId(record.getDeviceId());
            dev.setStatus("正常");
            deviceMapper.updateById(dev);
        } else {
            order.setStatus(RepairOrderStatusEnum.IN_PROGRESS.getLabel());
        }
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
    }

    private Integer calcReportSequence(Long deviceId) {
        Long count = repairRecordMapper.selectCount(new LambdaQueryWrapper<RepairRecord>().eq(RepairRecord::getDeviceId, deviceId));
        return (count == null ? 0 : count.intValue()) + 1;
    }

    private Integer calcMaintenanceSequence(Long deviceId) {
        Long count = repairRecordMapper.selectCount(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getDeviceId, deviceId)
                .eq(RepairRecord::getIsResolved, 1));
        return (count == null ? 0 : count.intValue()) + 1;
    }

    private Integer calcLaborHours(LocalDateTime start, LocalDateTime finish) {
        if (start == null || finish == null || finish.isBefore(start)) return null;
        long hours = ChronoUnit.HOURS.between(start, finish);
        return (int) Math.max(1, hours);
    }

    private Double round(Double value) {
        if (value == null) return null;
        return Math.round(value * 10.0) / 10.0;
    }
}
