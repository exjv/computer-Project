package com.jou.networkrepair.module.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.enums.RepairOrderStatusEnum;
import com.jou.networkrepair.module.repair.mapper.RepairOrderFlowMapper;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.service.RepairOrderService;
import com.jou.networkrepair.module.system.entity.BusinessLog;
import com.jou.networkrepair.module.system.mapper.BusinessLogMapper;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RepairOrderServiceImpl implements RepairOrderService {
    private static final Set<String> PRIORITY_SET = new HashSet<>(Arrays.asList("低", "中", "高"));

    private final RepairOrderMapper repairOrderMapper;
    private final RepairOrderFlowMapper repairOrderFlowMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final BusinessLogMapper businessLogMapper;

    @Override
    public Page<RepairOrder> page(Long current, Long size, String status, String title, String orderNo, String priority,
                                  String deviceType, String faultType,
                                  LocalDateTime reportTimeStart, LocalDateTime reportTimeEnd,
                                  String sortField, String sortOrder,
                                  Long userId, String role) {
        LambdaQueryWrapper<RepairOrder> qw = new LambdaQueryWrapper<RepairOrder>()
                .eq(notBlank(status), RepairOrder::getStatus, status)
                .like(notBlank(title), RepairOrder::getTitle, title)
                .like(notBlank(orderNo), RepairOrder::getOrderNo, orderNo)
                .eq(notBlank(priority), RepairOrder::getPriority, priority)
                .like(notBlank(deviceType), RepairOrder::getDeviceType, deviceType)
                .like(notBlank(faultType), RepairOrder::getFaultType, faultType)
                .ge(reportTimeStart != null, RepairOrder::getReportTime, reportTimeStart)
                .le(reportTimeEnd != null, RepairOrder::getReportTime, reportTimeEnd);

        if ("user".equals(role)) {
            qw.eq(RepairOrder::getReporterId, userId);
        } else if ("maintainer".equals(role)) {
            qw.eq(RepairOrder::getAssignMaintainerId, userId);
        }

        if ("asc".equalsIgnoreCase(sortOrder)) {
            if ("reportTime".equals(sortField)) qw.orderByAsc(RepairOrder::getReportTime);
            else if ("priority".equals(sortField)) qw.orderByAsc(RepairOrder::getPriority);
            else if ("status".equals(sortField)) qw.orderByAsc(RepairOrder::getStatus);
            else qw.orderByAsc(RepairOrder::getId);
        } else {
            if ("reportTime".equals(sortField)) qw.orderByDesc(RepairOrder::getReportTime);
            else if ("priority".equals(sortField)) qw.orderByDesc(RepairOrder::getPriority);
            else if ("status".equals(sortField)) qw.orderByDesc(RepairOrder::getStatus);
            else qw.orderByDesc(RepairOrder::getId);
        }
        return repairOrderMapper.selectPage(new Page<>(current, size), qw);
    }

    @Override
    public RepairOrder detail(Long id, Long userId, String role) {
        RepairOrder order = requireOrder(id);
        ensureScope(order, userId, role);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(RepairOrderCreateDTO dto, Long userId) {
        if (!PRIORITY_SET.contains(dto.getPriority())) {
            throw new BusinessException("紧急程度仅支持：低/中/高");
        }
        NetworkDevice device = deviceMapper.selectById(dto.getDeviceId());
        if (device == null) throw new BusinessException("设备不存在");
        SysUser reporter = userMapper.selectById(userId);
        if (reporter == null) throw new BusinessException("报修用户不存在");

        LocalDateTime now = LocalDateTime.now();
        RepairOrder order = new RepairOrder();
        order.setOrderNo(generateOrderNo());
        order.setReporterId(userId);
        order.setReporterName(reporter.getRealName());
        order.setReporterEmployeeNo(notBlank(dto.getReporterEmployeeNo()) ? dto.getReporterEmployeeNo() : reporter.getEmployeeNo());
        order.setContactPhone(notBlank(dto.getContactPhone()) ? dto.getContactPhone() : reporter.getPhone());
        order.setReporterDepartment(notBlank(dto.getReporterDepartment()) ? dto.getReporterDepartment() : reporter.getDepartment());
        order.setReportLocation(notBlank(dto.getReportLocation()) ? dto.getReportLocation() : device.getLocation());

        order.setDeviceId(device.getId());
        order.setDeviceCode(device.getDeviceCode());
        order.setDeviceName(device.getDeviceName());
        order.setDeviceType(notBlank(device.getDeviceTypeName()) ? device.getDeviceTypeName() : device.getDeviceType());

        order.setTitle(notBlank(dto.getTitle()) ? dto.getTitle() : (device.getDeviceName() + "故障报修"));
        order.setFaultType(dto.getFaultType());
        order.setDescription(dto.getDescription());
        order.setPriority(dto.getPriority());
        order.setAffectWideAreaNetwork(defaultZero(dto.getAffectWideAreaNetwork()));

        order.setNeedPurchaseParts(defaultZero(dto.getNeedPurchaseParts()));
        order.setPartsDescription(dto.getPartsDescription());
        order.setApplyDelay(defaultZero(dto.getApplyDelay()));
        order.setOriginalExpectedFinishTime(dto.getOriginalExpectedFinishTime());
        order.setDelayedExpectedFinishTime(dto.getDelayedExpectedFinishTime());
        order.setExpectedFinishTime(dto.getDelayedExpectedFinishTime() != null ? dto.getDelayedExpectedFinishTime() : dto.getOriginalExpectedFinishTime());

        order.setStatus(RepairOrderStatusEnum.SUBMITTED.getLabel());
        order.setProgress(5);
        order.setReportTime(now);
        order.setRemark(dto.getRemark());
        order.setCreateBy(userId);
        order.setUpdateBy(userId);
        order.setCreateTime(now);
        order.setUpdateTime(now);

        repairOrderMapper.insert(order);
        addFlow(order.getId(), null, order.getStatus(), "CREATE", userId, "user", "提交报修工单");
        addBusinessLog(order, "CREATE", userId, "user", null, order.getStatus(), "提交报修工单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RepairOrder req, Long userId, String role) {
        RepairOrder old = requireOrder(id);
        ensureScope(old, userId, role);
        if (RepairOrderStatusEnum.terminalStatuses().contains(old.getStatus())) {
            throw new BusinessException("终态工单不允许编辑");
        }

        req.setId(id);
        req.setOrderNo(null);
        req.setReporterId(null);
        req.setReporterName(null);
        req.setReporterEmployeeNo(null);
        req.setStatus(null);
        req.setProgress(null);
        req.setCreateTime(null);
        req.setCreateBy(null);
        req.setUpdateBy(userId);
        req.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(req);
        addBusinessLog(old, "UPDATE", userId, role, old.getStatus(), old.getStatus(), "更新工单信息");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId, String role) {
        RepairOrder order = requireOrder(id);
        ensureScope(order, userId, role);
        if (!"admin".equals(role) && !"user".equals(role)) {
            throw new BusinessException("仅管理员或报修人可删除");
        }
        if (RepairOrderStatusEnum.terminalStatuses().contains(order.getStatus())) {
            throw new BusinessException("终态工单不允许删除");
        }
        repairOrderMapper.deleteById(id);
        addBusinessLog(order, "DELETE", userId, role, order.getStatus(), null, "删除工单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, RepairOrderAssignDTO dto, Long userId, String role) {
        if (!"admin".equals(role)) throw new BusinessException("仅管理员可分配");
        RepairOrder order = requireOrder(id);
        SysUser maintainer = userMapper.selectById(dto.getAssignMaintainerId());
        if (maintainer == null) throw new BusinessException("维修人员不存在");

        LocalDateTime now = LocalDateTime.now();
        String from = order.getStatus();
        order.setAssignMaintainerId(maintainer.getId());
        order.setAssignMaintainerEmployeeNo(maintainer.getEmployeeNo());
        order.setAssignMaintainerName(maintainer.getRealName());
        order.setAssignBy(userId);
        order.setAssignTime(now);
        order.setStatus(RepairOrderStatusEnum.PENDING_ACCEPT.getLabel());
        order.setProgress(Math.max(defaultZero(order.getProgress()), 30));
        order.setUpdateBy(userId);
        order.setUpdateTime(now);
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, order.getStatus(), "ASSIGN", userId, role, "分配维修人员：" + maintainer.getRealName());
        addBusinessLog(order, "ASSIGN", userId, role, from, order.getStatus(), "分配维修人员：" + maintainer.getRealName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, RepairOrderStatusDTO dto, Long userId, String role) {
        if (!RepairOrderStatusEnum.containsLabel(dto.getStatus())) {
            throw new BusinessException("不支持的工单状态");
        }
        RepairOrder order = requireOrder(id);
        ensureScope(order, userId, role);

        LocalDateTime now = LocalDateTime.now();
        String from = order.getStatus();
        order.setStatus(dto.getStatus());
        if (dto.getProgress() != null) order.setProgress(dto.getProgress());
        if (dto.getNeedPurchaseParts() != null) order.setNeedPurchaseParts(dto.getNeedPurchaseParts());
        if (dto.getPartsDescription() != null) order.setPartsDescription(dto.getPartsDescription());
        if (dto.getApplyDelay() != null) order.setApplyDelay(dto.getApplyDelay());
        if (dto.getOriginalExpectedFinishTime() != null) order.setOriginalExpectedFinishTime(dto.getOriginalExpectedFinishTime());
        if (dto.getDelayedExpectedFinishTime() != null) {
            order.setDelayedExpectedFinishTime(dto.getDelayedExpectedFinishTime());
            order.setExpectedFinishTime(dto.getDelayedExpectedFinishTime());
        }
        if (dto.getUserConfirmResult() != null) order.setUserConfirmResult(dto.getUserConfirmResult());
        if (dto.getSatisfactionScore() != null) order.setSatisfactionScore(dto.getSatisfactionScore());
        if (dto.getFeedback() != null) order.setFeedback(dto.getFeedback());
        if (dto.getCloseReason() != null) order.setCloseReason(dto.getCloseReason());
        if (dto.getRemark() != null) order.setRemark(dto.getRemark());

        if (RepairOrderStatusEnum.AUDIT_APPROVED.getLabel().equals(dto.getStatus()) || RepairOrderStatusEnum.AUDIT_REJECTED.getLabel().equals(dto.getStatus())) {
            order.setAuditBy(userId);
            SysUser u = userMapper.selectById(userId);
            if (u != null) {
                order.setAuditByName(u.getRealName());
                order.setAuditByEmployeeNo(u.getEmployeeNo());
            }
            order.setAuditTime(now);
        }
        if (RepairOrderStatusEnum.ACCEPTED.getLabel().equals(dto.getStatus())) order.setAcceptTime(now);
        if (RepairOrderStatusEnum.IN_PROGRESS.getLabel().equals(dto.getStatus())) order.setStartRepairTime(now);
        if (RepairOrderStatusEnum.COMPLETED.getLabel().equals(dto.getStatus()) || RepairOrderStatusEnum.CLOSED.getLabel().equals(dto.getStatus())) {
            order.setFinishTime(now);
            if (order.getProgress() == null || order.getProgress() < 100) order.setProgress(100);
        }
        if (RepairOrderStatusEnum.COMPLETED.getLabel().equals(dto.getStatus())) order.setConfirmTime(now);

        order.setUpdateBy(userId);
        order.setUpdateTime(now);
        repairOrderMapper.updateById(order);
        addFlow(id, from, dto.getStatus(), "STATUS_UPDATE", userId, role, dto.getRemark());
        addBusinessLog(order, "STATUS_UPDATE", userId, role, from, dto.getStatus(), dto.getRemark());
    }

    @Override
    public List<RepairOrderFlow> flows(Long id, Long userId, String role) {
        RepairOrder order = requireOrder(id);
        ensureScope(order, userId, role);
        return repairOrderFlowMapper.selectList(new LambdaQueryWrapper<RepairOrderFlow>()
                .eq(RepairOrderFlow::getRepairOrderId, id)
                .orderByAsc(RepairOrderFlow::getId));
    }

    @Override
    public Map<String, Object> stats(Long userId, String role) {
        Map<String, Object> map = new HashMap<>();
        Long total = repairOrderMapper.selectCount(scope(role, userId));
        Long processing = repairOrderMapper.selectCount(scope(role, userId)
                .notIn(RepairOrder::getStatus, RepairOrderStatusEnum.terminalStatuses()));
        Long completed = repairOrderMapper.selectCount(scope(role, userId)
                .eq(RepairOrder::getStatus, RepairOrderStatusEnum.COMPLETED.getLabel()));
        Long closed = repairOrderMapper.selectCount(scope(role, userId)
                .eq(RepairOrder::getStatus, RepairOrderStatusEnum.CLOSED.getLabel()));

        map.put("total", total);
        map.put("processing", processing);
        map.put("completed", completed);
        map.put("closed", closed);
        return map;
    }

    private RepairOrder requireOrder(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        return order;
    }

    private void ensureScope(RepairOrder order, Long userId, String role) {
        if ("admin".equals(role)) return;
        if ("maintainer".equals(role) && userId != null && userId.equals(order.getAssignMaintainerId())) return;
        if ("user".equals(role) && userId != null && userId.equals(order.getReporterId())) return;
        throw new BusinessException("无权操作该工单");
    }

    private void addFlow(Long orderId, String from, String to, String opType, Long operatorId, String role, String remark) {
        RepairOrderFlow flow = new RepairOrderFlow();
        flow.setRepairOrderId(orderId);
        flow.setFromStatus(from);
        flow.setToStatus(to);
        flow.setAction(opType);
        flow.setOperationType(opType);
        flow.setOperatorId(operatorId);
        if (operatorId != null) {
            SysUser operator = userMapper.selectById(operatorId);
            if (operator != null) {
                flow.setOperatorEmployeeNo(operator.getEmployeeNo());
                flow.setOperatorName(operator.getRealName());
            }
        }
        flow.setOperatorRole(role);
        flow.setRemark(remark);
        flow.setOperationTime(LocalDateTime.now());
        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        repairOrderFlowMapper.insert(flow);
    }

    private void addBusinessLog(RepairOrder order, String action, Long operatorId, String role, String fromStatus, String toStatus, String remark) {
        BusinessLog log = new BusinessLog();
        log.setBusinessType("REPAIR_ORDER");
        log.setBusinessNo(order.getOrderNo());
        log.setBizType("REPAIR_ORDER");
        log.setBizId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setAction(action);
        log.setOperatorId(operatorId);
        if (operatorId != null) {
            SysUser u = userMapper.selectById(operatorId);
            if (u != null) {
                log.setOperatorName(u.getRealName());
                log.setOperatorEmployeeNo(u.getEmployeeNo());
                log.setOperatorJobNo(u.getEmployeeNo());
            }
        }
        log.setOperatorRole(role);
        log.setStatus(toStatus);
        String content = String.format("状态：%s -> %s；意见：%s",
                fromStatus == null ? "-" : fromStatus,
                toStatus == null ? "-" : toStatus,
                remark == null ? "-" : remark);
        log.setContent(content);
        log.setOperationTime(LocalDateTime.now());
        log.setCreateTime(LocalDateTime.now());
        businessLogMapper.insert(log);
    }

    private String generateOrderNo() {
        String prefix = "RO" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long cnt = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .likeRight(RepairOrder::getOrderNo, prefix));
        return prefix + String.format("%04d", cnt + 1);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private Integer defaultZero(Integer i) {
        return i == null ? 0 : i;
    }

    private LambdaQueryWrapper<RepairOrder> scope(String role, Long userId) {
        LambdaQueryWrapper<RepairOrder> qw = new LambdaQueryWrapper<>();
        if ("user".equals(role)) qw.eq(RepairOrder::getReporterId, userId);
        if ("maintainer".equals(role)) qw.eq(RepairOrder::getAssignMaintainerId, userId);
        return qw;
    }
}
