package com.jou.networkrepair.module.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.enums.RepairOrderStatusEnum;
import com.jou.networkrepair.module.repair.algorithm.RepairDispatchAlgorithm;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderActionDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderAuditDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCloseDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderDelayApproveDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderReassignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderFlowMapper;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import com.jou.networkrepair.module.repair.service.RepairOrderService;
import com.jou.networkrepair.module.repair.vo.AssignmentRecommendationVO;
import com.jou.networkrepair.module.repair.vo.DispatchResultVO;
import com.jou.networkrepair.module.repair.vo.MaintainerRecommendVO;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RepairOrderServiceImpl implements RepairOrderService {
    private static final Set<String> PRIORITY_SET = new HashSet<>(Arrays.asList("低", "中", "高"));
    private static final Set<String> STATUS_SET = new HashSet<>(Arrays.asList(
            "待提交", "已提交/待审核", "审核通过", "审核驳回", "待分配", "已分配", "待接单", "维修人员已接单", "维修中",
            "待采购/待配件", "申请延期中", "延期已批准", "待验收/待确认", "已完成", "已关闭", "已取消"));

    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final RepairDispatchAlgorithm repairDispatchAlgorithm;
    private final RepairOrderFlowMapper repairOrderFlowMapper;
    private final RepairRecordMapper repairRecordMapper;

    @Override
    public Page<RepairOrder> page(Long current, Long size, String status, String title, String orderNo, String priority,
                                  String deviceType, String faultType, Long assignMaintainerId, Integer applyDelay, Integer needPurchaseParts,
                                  LocalDateTime reportTimeStart, LocalDateTime reportTimeEnd, String sortField, String sortOrder) {
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(status != null && !status.isEmpty(), RepairOrder::getStatus, status)
                .like(title != null && !title.isEmpty(), RepairOrder::getTitle, title)
                .like(orderNo != null && !orderNo.isEmpty(), RepairOrder::getOrderNo, orderNo)
                .eq(priority != null && !priority.isEmpty(), RepairOrder::getPriority, priority)
                .like(deviceType != null && !deviceType.isEmpty(), RepairOrder::getDeviceType, deviceType)
                .like(faultType != null && !faultType.isEmpty(), RepairOrder::getFaultType, faultType)
                .eq(assignMaintainerId != null, RepairOrder::getAssignMaintainerId, assignMaintainerId)
                .eq(applyDelay != null, RepairOrder::getApplyDelay, applyDelay)
                .eq(needPurchaseParts != null, RepairOrder::getNeedPurchaseParts, needPurchaseParts)
                .ge(reportTimeStart != null, RepairOrder::getReportTime, reportTimeStart)
                .le(reportTimeEnd != null, RepairOrder::getReportTime, reportTimeEnd);
        applySort(wrapper, sortField, sortOrder);
        return repairOrderMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public Page<RepairOrder> myPage(Long current, Long size, String status, String orderNo, String priority,
                                    String deviceType, String faultType, Integer applyDelay, Integer needPurchaseParts,
                                    LocalDateTime reportTimeStart, LocalDateTime reportTimeEnd,
                                    Long userId, String role, String sortField, String sortOrder) {
        LambdaQueryWrapper<RepairOrder> qw = new LambdaQueryWrapper<>();
        if ("user".equals(role)) qw.eq(RepairOrder::getReporterId, userId);
        if ("maintainer".equals(role)) qw.eq(RepairOrder::getAssignMaintainerId, userId);
        qw.eq(status != null && !status.isEmpty(), RepairOrder::getStatus, status)
                .like(orderNo != null && !orderNo.isEmpty(), RepairOrder::getOrderNo, orderNo)
                .eq(priority != null && !priority.isEmpty(), RepairOrder::getPriority, priority)
                .like(deviceType != null && !deviceType.isEmpty(), RepairOrder::getDeviceType, deviceType)
                .like(faultType != null && !faultType.isEmpty(), RepairOrder::getFaultType, faultType)
                .eq(applyDelay != null, RepairOrder::getApplyDelay, applyDelay)
                .eq(needPurchaseParts != null, RepairOrder::getNeedPurchaseParts, needPurchaseParts)
                .ge(reportTimeStart != null, RepairOrder::getReportTime, reportTimeStart)
                .le(reportTimeEnd != null, RepairOrder::getReportTime, reportTimeEnd);
        applySort(qw, sortField, sortOrder);
        return repairOrderMapper.selectPage(new Page<>(current, size), qw);
    }

    @Override
    public RepairOrder detail(Long id, Long userId, String role) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if ("user".equals(role) && !userId.equals(order.getReporterId())) throw new BusinessException("无权查看");
        if ("maintainer".equals(role) && !userId.equals(order.getAssignMaintainerId())) throw new BusinessException("无权查看");
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(RepairOrderCreateDTO dto, Long userId) {
        if (!PRIORITY_SET.contains(dto.getPriority())) throw new BusinessException("无效优先级");
        NetworkDevice existsDevice = deviceMapper.selectById(dto.getDeviceId());
        if (existsDevice == null) throw new BusinessException("设备不存在");
        SysUser reporter = userMapper.selectById(userId);
        if (reporter == null) throw new BusinessException("报修用户不存在");

        RepairOrder order = new RepairOrder();
        order.setDeviceId(dto.getDeviceId());
        order.setDeviceCode(existsDevice.getDeviceCode());
        order.setDeviceName(existsDevice.getDeviceName());
        order.setDeviceType(existsDevice.getDeviceTypeName() != null ? existsDevice.getDeviceTypeName() : existsDevice.getDeviceType());
        order.setTitle(dto.getTitle());
        order.setDescription(dto.getDescription());
        order.setFaultType(dto.getFaultType());
        order.setPriority(dto.getPriority());
        order.setReporterId(userId);
        order.setReporterEmployeeNo(reporter.getEmployeeNo());
        order.setReporterName(reporter.getRealName());
        order.setContactPhone(dto.getContactPhone() != null && !dto.getContactPhone().trim().isEmpty() ? dto.getContactPhone() : reporter.getPhone());
        order.setReporterDepartment(reporter.getDepartment());
        order.setReportLocation(dto.getReportLocation() != null && !dto.getReportLocation().trim().isEmpty() ? dto.getReportLocation() : existsDevice.getLocation());
        order.setAffectWideAreaNetwork(dto.getAffectWideAreaNetwork());
        order.setRemark(dto.getRemark());
        order.setOriginalExpectedFinishTime(dto.getOriginalExpectedFinishTime());
        order.setExpectedFinishTime(dto.getOriginalExpectedFinishTime());
        order.setOrderNo(generateOrderNo());
        boolean needApprove = existsDevice.getRepairApprovalRequired() != null && existsDevice.getRepairApprovalRequired() == 1;
        order.setStatus(needApprove ? "已提交/待审核" : "待分配");
        order.setProgress(needApprove ? 10 : 30);
        order.setReportTime(LocalDateTime.now());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.insert(order);
        addFlow(order.getId(), "待提交", order.getStatus(), "SUBMIT", userId, "user",
                needApprove ? "设备需管理员审批，进入待审核" : "设备无需审批，直接进入待分配");
        syncDeviceStatus(order.getDeviceId());
    }

    @Override
    public void update(Long id, RepairOrder req) {
        RepairOrder old = repairOrderMapper.selectById(id);
        if (old == null) throw new BusinessException("工单不存在");
        if (Arrays.asList("已完成", "已关闭", "已取消").contains(old.getStatus())) {
            throw new BusinessException("终态工单不允许编辑核心字段");
        }
        req.setId(id);
        req.setReporterId(null);
        req.setOrderNo(null);
        req.setStatus(null);
        req.setAssignMaintainerId(null);
        req.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(req);
    }

    @Override
    public void delete(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if (Arrays.asList("维修人员已接单", "维修中", "待采购/待配件", "申请延期中", "延期已批准", "待验收/待确认", "已完成", "已关闭").contains(order.getStatus())) throw new BusinessException("当前状态不允许删除");
        repairOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, RepairOrderAssignDTO dto, Long assignBy) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if (!"待分配".equals(order.getStatus())) throw new BusinessException("未审核通过或未进入待分配，不能分配");
        SysUser maintainer = userMapper.selectById(dto.getAssignMaintainerId());
        if (maintainer == null || !"maintainer".equals(maintainer.getRole()) || maintainer.getStatus() == null || maintainer.getStatus() != 1) {
            throw new BusinessException("维修人员无效或不可用");
        }
        String fromStatus = order.getStatus();
        SysUser assignUser = assignBy == null ? null : userMapper.selectById(assignBy);
        if (assignUser != null) {
            order.setAssignBy(assignUser.getId());
            order.setAssignByEmployeeNo(assignUser.getEmployeeNo());
            order.setAssignByName(assignUser.getRealName());
        }
        order.setAssignMaintainerId(dto.getAssignMaintainerId());
        order.setAssignMaintainerEmployeeNo(maintainer.getEmployeeNo());
        order.setAssignMaintainerName(maintainer.getRealName());
        order.setAssignTime(LocalDateTime.now());
        order.setStatus("已分配");
        order.setProgress(35);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), fromStatus, "已分配", "ADMIN_ASSIGN", null, "admin", "管理员分配维修人员");
        moveStatus(order, "待接单", 40, null, "admin", "等待维修人员接单", "SYSTEM_WAIT_ACCEPT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void action(Long id, RepairOrderActionDTO dto, Long userId, String role) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        String action = dto.getAction();
        if ("ADMIN_APPROVE".equals(action)) {
            requireRole(role, "admin");
            checkStatus(order.getStatus(), "已提交/待审核");
            moveStatus(order, "审核通过", 20, userId, role, dto.getRemark(), action);
            order.setAuditBy(userId);
            order.setAuditTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            moveStatus(order, RepairOrderStatusEnum.PENDING_ASSIGN.getLabel(), 30, userId, role, "审核通过进入待分配", "ADMIN_TO_ASSIGN");
        } else if ("ADMIN_REJECT".equals(action)) {
            requireRole(role, "admin");
            checkStatus(order.getStatus(), "已提交/待审核");
            moveStatus(order, "审核驳回", 0, userId, role, dto.getRemark(), action);
        } else if ("USER_CANCEL".equals(action)) {
            requireRole(role, "user");
            if (!userId.equals(order.getReporterId())) throw new BusinessException("只能撤销自己的工单");
            if (!Arrays.asList("已提交/待审核", "审核驳回", "待分配").contains(order.getStatus())) throw new BusinessException("当前状态不允许撤销");
            moveStatus(order, "已取消", 0, userId, role, dto.getRemark(), action);
        } else if ("MAINTAINER_ACCEPT".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), RepairOrderStatusEnum.PENDING_ACCEPT.getLabel());
            order.setAcceptTime(LocalDateTime.now());
            moveStatus(order, "维修人员已接单", 45, userId, role, dto.getRemark(), action);
        } else if ("MAINTAINER_REJECT".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), "待接单");
            moveStatus(order, "待分配", 30, userId, role, dto.getRemark() == null ? "维修人员拒单，退回待分配" : dto.getRemark(), action);
        } else if ("MAINTAINER_START".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), RepairOrderStatusEnum.MAINTAINER_ACCEPTED.getLabel());
            order.setStartRepairTime(LocalDateTime.now());
            moveStatus(order, "维修中", 60, userId, role, dto.getRemark(), action);
        } else if ("MAINTAINER_DELAY_APPLY".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            if (!Arrays.asList("维修中", "延期已批准").contains(order.getStatus())) throw new BusinessException("当前状态不允许申请延期");
            order.setDelayReason(dto.getRemark());
            moveStatus(order, "申请延期中", 65, userId, role, dto.getRemark() == null ? "申请延期" : dto.getRemark(), action);
        } else if ("MAINTAINER_PARTS_APPLY".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), "维修中");
            order.setPartsRequirement(dto.getPartsRequirement());
            moveStatus(order, "待采购/待配件", 68, userId, role, dto.getRemark() == null ? "申请配件" : dto.getRemark(), action);
        } else if ("MAINTAINER_PROGRESS".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), "维修中");
            order.setPartsRequirement(dto.getPartsRequirement());
            moveStatus(order, "待采购/待配件", 68, userId, role, dto.getRemark() == null ? "申请配件" : dto.getRemark(), action);
        } else if ("MAINTAINER_PROGRESS".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), RepairOrderStatusEnum.IN_REPAIR.getLabel());
            if (dto.getProgress() == null) throw new BusinessException("请传入进度");
            order.setProgress(dto.getProgress());
            if (dto.getScenePhotoUrls() != null) order.setScenePhotoUrls(dto.getScenePhotoUrls());
            if (dto.getHandleDescription() != null) order.setHandleDescription(dto.getHandleDescription());
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), RepairOrderStatusEnum.IN_REPAIR.getLabel(), RepairOrderStatusEnum.IN_REPAIR.getLabel(), action, userId, role, "进度更新至" + dto.getProgress() + "%");
            addBusinessLog(order, action, userId, role, order.getStatus(), order.getStatus(), "进度更新至" + dto.getProgress() + "%");
        } else if ("MAINTAINER_DELAY_APPLY".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), RepairOrderStatusEnum.IN_REPAIR.getLabel());
            order.setApplyDelay(1);
            order.setDelayedExpectedFinishTime(dto.getDelayedExpectedFinishTime());
            moveStatus(order, RepairOrderStatusEnum.DELAY_APPLYING.getLabel(), order.getProgress(), userId, role, dto.getRemark(), action);
        } else if ("MAINTAINER_PARTS_APPLY".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), RepairOrderStatusEnum.IN_REPAIR.getLabel());
            order.setNeedPurchaseParts(1);
            order.setPartsDescription(dto.getPartsDescription());
            moveStatus(order, RepairOrderStatusEnum.PENDING_PARTS.getLabel(), order.getProgress(), userId, role, dto.getRemark(), action);
        } else if ("MAINTAINER_FINISH".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            if (!Arrays.asList("维修中", "延期已批准", "待采购/待配件").contains(order.getStatus())) throw new BusinessException("当前状态不允许完工");
            moveStatus(order, "待验收/待确认", 90, userId, role, dto.getRemark(), action);
            order.setFinishTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            sinkRepairRecord(order, userId, dto);
            recordPredictionError(order, userId);
        } else if ("USER_CONFIRM_RESOLVED".equals(action)) {
            requireRole(role, "user");
            if (!userId.equals(order.getReporterId())) throw new BusinessException("只能确认自己的工单");
            checkStatus(order.getStatus(), "待验收/待确认");
            order.setConfirmTime(LocalDateTime.now());
            if (dto.getSatisfactionScore() != null) order.setSatisfactionScore(dto.getSatisfactionScore());
            if (dto.getFeedback() != null) order.setFeedback(dto.getFeedback());
            moveStatus(order, "已完成", 100, userId, role, dto.getRemark(), action);
        } else if ("USER_CONFIRM_UNRESOLVED".equals(action)) {
            requireRole(role, "user");
            if (!userId.equals(order.getReporterId())) throw new BusinessException("只能确认自己的工单");
            checkStatus(order.getStatus(), "待验收/待确认");
            moveStatus(order, "维修中", 60, userId, role, dto.getRemark(), action);
        } else if ("ADMIN_REASSIGN".equals(action)) {
            requireRole(role, "admin");
            if (dto.getAssignMaintainerId() == null) throw new BusinessException("改派时必须指定新的维修人员");
            SysUser maintainer = userMapper.selectById(dto.getAssignMaintainerId());
            if (maintainer == null || !"maintainer".equals(maintainer.getRole()) || maintainer.getStatus() == null || maintainer.getStatus() != 1) {
                throw new BusinessException("维修人员无效或不可用");
            }
            String oldStatus = order.getStatus();
            order.setAssignMaintainerId(dto.getAssignMaintainerId());
            order.setAssignTime(LocalDateTime.now());
            order.setStatus("待接单");
            order.setProgress(35);
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), oldStatus, "待接单", action, userId, role, dto.getRemark() == null ? "管理员改派" : dto.getRemark());
        } else if ("ADMIN_DELAY_APPROVE".equals(action)) {
            requireRole(role, "admin");
            checkStatus(order.getStatus(), "申请延期中");
            moveStatus(order, "延期已批准", 70, userId, role, dto.getRemark() == null ? "管理员审批延期" : dto.getRemark(), action);
        } else if ("ADMIN_CLOSE".equals(action)) {
            requireRole(role, "admin");
            if (Arrays.asList("已完成", "已关闭", "已取消").contains(order.getStatus())) throw new BusinessException("终态工单不允许重复关闭");
            if (!"已完成".equals(order.getStatus()) && (dto.getRemark() == null || dto.getRemark().trim().isEmpty())) throw new BusinessException("用户未确认前强制关闭必须填写原因");
            order.setCloseReason(dto.getRemark());
            moveStatus(order, "已关闭", 100, userId, role, dto.getRemark(), action);
        } else {
            throw new BusinessException("不支持的操作");
        }
        syncDeviceStatus(order.getDeviceId());
    }

    @Override
    public List<RepairOrderFlow> flows(Long id, Long userId, String role) {
        RepairOrder order = detail(id, userId, role);
        if (order == null) throw new BusinessException("工单不存在");
        return repairOrderFlowMapper.selectList(new LambdaQueryWrapper<RepairOrderFlow>()
                .eq(RepairOrderFlow::getRepairOrderId, id)
                .orderByAsc(RepairOrderFlow::getId));
    }

    @Override
    public List<BusinessLog> businessLogs(Long id, Long userId, String role) {
        RepairOrder order = detail(id, userId, role);
        if (order == null) throw new BusinessException("工单不存在");
        return businessLogMapper.selectList(new LambdaQueryWrapper<BusinessLog>()
                .eq(BusinessLog::getBusinessType, "REPAIR_ORDER")
                .eq(BusinessLog::getBusinessNo, order.getOrderNo())
                .orderByAsc(BusinessLog::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassign(Long id, RepairOrderReassignDTO dto, Long userId) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if (!Arrays.asList(
                RepairOrderStatusEnum.PENDING_ACCEPT.getLabel(),
                RepairOrderStatusEnum.MAINTAINER_ACCEPTED.getLabel(),
                RepairOrderStatusEnum.IN_REPAIR.getLabel()).contains(order.getStatus())) {
            throw new BusinessException("当前状态不允许改派");
        }
        SysUser maintainer = userMapper.selectById(dto.getAssignMaintainerId());
        if (maintainer == null || !"maintainer".equals(maintainer.getRole()) || maintainer.getStatus() == null || maintainer.getStatus() != 1) {
            throw new BusinessException("维修人员无效或不可用");
        }
        String oldMaintainer = order.getAssignMaintainerName();
        order.setAssignMaintainerId(maintainer.getId());
        order.setAssignMaintainerEmployeeNo(maintainer.getEmployeeNo());
        order.setAssignMaintainerName(maintainer.getRealName());
        order.setAssignBy(userId);
        SysUser admin = userMapper.selectById(userId);
        if (admin != null) {
            order.setAssignByEmployeeNo(admin.getEmployeeNo());
            order.setAssignByName(admin.getRealName());
        }
        order.setAssignTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), order.getStatus(), order.getStatus(), "ADMIN_REASSIGN", userId, "admin",
                (dto.getRemark() == null ? "" : dto.getRemark() + "；") + "由" + (oldMaintainer == null ? "-" : oldMaintainer) + "改派为" + maintainer.getRealName());
        addBusinessLog(order, "ADMIN_REASSIGN", userId, "admin", order.getStatus(), order.getStatus(),
                "改派：" + (oldMaintainer == null ? "-" : oldMaintainer) + " -> " + maintainer.getRealName() + "；" + (dto.getRemark() == null ? "无备注" : dto.getRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveDelay(Long id, RepairOrderDelayApproveDTO dto, Long userId) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        checkStatus(order.getStatus(), RepairOrderStatusEnum.DELAY_APPLYING.getLabel());
        if (Boolean.TRUE.equals(dto.getApproved())) {
            order.setDelayedExpectedFinishTime(dto.getDelayedExpectedFinishTime());
            moveStatus(order, RepairOrderStatusEnum.DELAY_APPROVED.getLabel(), order.getProgress(), userId, "admin", dto.getRemark(), "ADMIN_DELAY_APPROVE");
            moveStatus(order, RepairOrderStatusEnum.IN_REPAIR.getLabel(), 60, userId, "admin", "延期审批通过，返回维修中", "ADMIN_DELAY_TO_REPAIR");
        } else {
            moveStatus(order, RepairOrderStatusEnum.IN_REPAIR.getLabel(), 60, userId, "admin", dto.getRemark(), "ADMIN_DELAY_REJECT");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, RepairOrderCloseDTO dto, Long userId) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if (Boolean.TRUE.equals(dto.getForceClose())) {
            if (RepairOrderStatusEnum.FINISHED.getLabel().equals(order.getStatus())) throw new BusinessException("已完成工单无需强制关闭");
            order.setCloseReason(dto.getCloseReason());
            moveStatus(order, RepairOrderStatusEnum.CLOSED.getLabel(), order.getProgress(), userId, "admin", dto.getCloseReason(), "ADMIN_FORCE_CLOSE");
            return;
        }
        if (!RepairOrderStatusEnum.FINISHED.getLabel().equals(order.getStatus())) {
            throw new BusinessException("用户未确认完成前不能直接关闭");
        }
        order.setCloseReason(dto.getCloseReason());
        moveStatus(order, RepairOrderStatusEnum.CLOSED.getLabel(), 100, userId, "admin", dto.getCloseReason(), "ADMIN_CLOSE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, RepairOrderStatusDTO dto, Long userId, String role) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if ("maintainer".equals(role) && !userId.equals(order.getAssignMaintainerId())) throw new BusinessException("仅可处理分配给自己的工单");
        if (!STATUS_SET.contains(dto.getStatus())) throw new BusinessException("无效工单状态");
        validateStatusTransition(order.getStatus(), dto.getStatus());
        String fromStatus = order.getStatus();
        order.setStatus(dto.getStatus());
        if ("已完成".equals(dto.getStatus())) order.setFinishTime(LocalDateTime.now());
        addFlow(order.getId(), order.getStatus(), dto.getStatus(), "MANUAL_STATUS", userId, role, dto.getRemark());
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), fromStatus, dto.getStatus(), "MANUAL_STATUS_UPDATE", userId, role, dto.getStatus());
        addBusinessLog(order, "MANUAL_STATUS_UPDATE", userId, role, fromStatus, dto.getStatus(), "手工修改状态");

        syncDeviceStatus(order.getDeviceId());
    }

    @Override
    public Map<String, Object> stats(Long userId, String role) {
        LambdaQueryWrapper<RepairOrder> base = new LambdaQueryWrapper<>();
        if ("user".equals(role)) base.eq(RepairOrder::getReporterId, userId);
        if ("maintainer".equals(role)) base.eq(RepairOrder::getAssignMaintainerId, userId);
        Map<String, Object> map = new HashMap<>();
        map.put("total", repairOrderMapper.selectCount(base));
        map.put("pending", repairOrderMapper.selectCount(base.clone().in(RepairOrder::getStatus, Arrays.asList("已提交/待审核", "待分配", "已分配", "待接单"))));
        map.put("processing", repairOrderMapper.selectCount(base.clone().in(RepairOrder::getStatus, Arrays.asList("维修人员已接单", "维修中", "待采购/待配件", "申请延期中", "延期已批准", "待验收/待确认"))));
        map.put("finished", repairOrderMapper.selectCount(base.clone().eq(RepairOrder::getStatus, "已完成")));
        return map;
    }

    @Override
    public Map<String, Object> analytics(String rangeType, String start, String end) {
        TimeRange tr = resolveRange(rangeType, start, end);
        List<RepairOrder> orders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .ge(RepairOrder::getReportTime, tr.start)
                .le(RepairOrder::getReportTime, tr.end));
        List<RepairFeedback> feedbacks = repairFeedbackMapper.selectList(new LambdaQueryWrapper<RepairFeedback>()
                .ge(RepairFeedback::getConfirmTime, tr.start)
                .le(RepairFeedback::getConfirmTime, tr.end));

        Map<String, Object> data = new HashMap<>();
        long finishedCount = orders.stream().filter(o -> RepairOrderStatusEnum.FINISHED.getLabel().equals(o.getStatus())).count();
        data.put("rangeType", tr.rangeType);
        data.put("rangeStart", tr.start);
        data.put("rangeEnd", tr.end);
        data.put("repairCount", orders.size());
        data.put("finishedCount", finishedCount);
        data.put("unfinishedCount", Math.max(0, orders.size() - finishedCount));
        data.put("avgRepairHours", avgRepairHours(orders));
        data.put("deviceTypeRank", toRank(orders, RepairOrder::getDeviceType, null, "deviceType"));
        data.put("highFaultDeviceRank", toRank(orders, RepairOrder::getDeviceCode, null, "deviceCode"));
        data.put("faultReasonDistribution", toRank(orders, RepairOrder::getFaultType, null, "faultType"));
        data.put("maintainerOrderCount", toRank(orders, RepairOrder::getAssignMaintainerName, null, "assignMaintainerName"));
        data.put("maintainerAvgHours", maintainerAvgHours(orders));
        data.put("satisfactionStats", satisfactionStats(feedbacks));
        data.put("delayOrderRatio", ratio(orders.stream().filter(o -> o.getApplyDelay() != null && o.getApplyDelay() == 1).count(), orders.size()));
        data.put("partsPurchaseRatio", ratio(orders.stream().filter(o -> o.getNeedPurchaseParts() != null && o.getNeedPurchaseParts() == 1).count(), orders.size()));
        data.put("predictionAnalysis", predictionAnalysis(orders));
        data.put("timeTrend", buildTrend(orders, tr));
        return data;
    }

    @Override
    public Map<String, Object> feedbackStats() {
        Map<String, Object> map = new HashMap<>();
        Long totalFeedback = repairFeedbackMapper.selectCount(new LambdaQueryWrapper<>());
        map.put("feedbackTotalCount", totalFeedback == null ? 0L : totalFeedback);
        if (totalFeedback == null || totalFeedback == 0L) {
            map.put("satisfactionAvgScore", 0D);
            map.put("lowSatisfactionCount", 0L);
            map.put("unresolvedFeedbackCount", 0L);
            return map;
        }
        List<RepairFeedback> feedbackList = repairFeedbackMapper.selectList(new LambdaQueryWrapper<RepairFeedback>()
                .isNotNull(RepairFeedback::getSatisfactionScore));
        double avg = feedbackList.stream().mapToInt(v -> v.getSatisfactionScore() == null ? 0 : v.getSatisfactionScore()).average().orElse(0D);
        Long lowSatisfactionCount = repairFeedbackMapper.selectCount(new LambdaQueryWrapper<RepairFeedback>()
                .isNotNull(RepairFeedback::getSatisfactionScore)
                .le(RepairFeedback::getSatisfactionScore, 2));
        Long unresolvedCount = repairFeedbackMapper.selectCount(new LambdaQueryWrapper<RepairFeedback>()
                .eq(RepairFeedback::getConfirmResult, "未解决"));
        map.put("satisfactionAvgScore", avg);
        map.put("lowSatisfactionCount", lowSatisfactionCount == null ? 0L : lowSatisfactionCount);
        map.put("unresolvedFeedbackCount", unresolvedCount == null ? 0L : unresolvedCount);
        return map;
    }

    @Override
    public Page<Map<String, Object>> lowSatisfactionOrders(Long current, Long size, Integer threshold) {
        int scoreThreshold = threshold == null ? 2 : Math.max(1, Math.min(threshold, 5));
        return pageFeedbackOrders(current, size, new LambdaQueryWrapper<RepairFeedback>()
                .isNotNull(RepairFeedback::getSatisfactionScore)
                .le(RepairFeedback::getSatisfactionScore, scoreThreshold)
                .orderByDesc(RepairFeedback::getConfirmTime));
    }

    @Override
    public Page<Map<String, Object>> unresolvedFeedbackOrders(Long current, Long size) {
        return pageFeedbackOrders(current, size, new LambdaQueryWrapper<RepairFeedback>()
                .eq(RepairFeedback::getConfirmResult, "未解决")
                .orderByDesc(RepairFeedback::getConfirmTime));
    }

    @Override
    public List<DispatchResultVO> autoDispatch() {
        List<RepairOrder> pendingOrders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, RepairOrderStatusEnum.PENDING_ASSIGN.getLabel())
                .orderByAsc(RepairOrder::getReportTime));
        if (pendingOrders.isEmpty()) return Collections.emptyList();

        List<SysUser> maintainers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "maintainer")
                .eq(SysUser::getStatus, 1));
        if (maintainers.isEmpty()) throw new BusinessException("没有可用的维修人员");

        Map<Long, NetworkDevice> deviceMap = new HashMap<>();
        for (RepairOrder order : pendingOrders) {
            deviceMap.put(order.getDeviceId(), deviceMapper.selectById(order.getDeviceId()));
        }

        Map<Long, Long> unfinishedCountMap = new HashMap<>();
        Map<Long, Long> processingCountMap = new HashMap<>();
        Map<Long, String> maintainerNameMap = new HashMap<>();
        for (SysUser m : maintainers) {
            Long unfinished = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .in(RepairOrder::getStatus, Arrays.asList("待接单", "维修人员已接单", "维修中", "待采购/待配件", "申请延期中", "延期已批准", "待验收/待确认")));
            Long processing = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .eq(RepairOrder::getStatus, "维修中"));
            unfinishedCountMap.put(m.getId(), unfinished == null ? 0L : unfinished);
            processingCountMap.put(m.getId(), processing == null ? 0L : processing);
            maintainerNameMap.put(m.getId(), m.getRealName());
        }

        PriorityQueue<RepairOrder> maxHeap = repairDispatchAlgorithm.buildMaxHeap(pendingOrders, deviceMap);
        List<DispatchResultVO> result = new ArrayList<>();

        while (!maxHeap.isEmpty()) {
            RepairOrder order = maxHeap.poll();
            List<MaintainerRecommendVO> recommends = recommendMaintainers(order.getId());
            if (recommends.isEmpty()) throw new BusinessException("未找到可分配维修人员");
            MaintainerRecommendVO top = recommends.get(0);
            Long targetMaintainerId = top.getMaintainerId();

            order.setAssignMaintainerId(targetMaintainerId);
            order.setAssignTime(LocalDateTime.now());
            order.setStatus(RepairOrderStatusEnum.PENDING_ACCEPT.getLabel());
            order.setProgress(35);
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), "待分配", "待接单", "AUTO_ASSIGN", null, "system", top.getReason());

            unfinishedCountMap.put(targetMaintainerId, unfinishedCountMap.get(targetMaintainerId) + 1L);
            Double score = repairDispatchAlgorithm.calcPriorityScore(order, deviceMap.get(order.getDeviceId()));
            result.add(new DispatchResultVO(order.getOrderNo(), targetMaintainerId,
                    maintainerNameMap.get(targetMaintainerId), score, top.getReason()));
        }
        return result;
    }


    @Override
    public List<MaintainerRecommendVO> recommendMaintainers(Long orderId) {
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");
        if (!Arrays.asList("待分配", "已分配", "待接单").contains(order.getStatus())) {
            throw new BusinessException("当前状态不支持推荐分配");
        }
        NetworkDevice device = deviceMapper.selectById(order.getDeviceId());
        double priorityScore = repairDispatchAlgorithm.calcPriorityScore(order, device);

        List<SysUser> maintainers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "maintainer")
                .eq(SysUser::getStatus, 1));
        List<MaintainerRecommendVO> list = new ArrayList<>();
        for (SysUser m : maintainers) {
            Long unfinished = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .notIn(RepairOrder::getStatus, Arrays.asList("已完成", "已关闭", "已取消")));
            Long processing = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .in(RepairOrder::getStatus, Arrays.asList("维修人员已接单", "维修中", "待采购/待配件", "申请延期中", "延期已批准")));

            Long hisCount = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .eq(RepairOrder::getStatus, "已完成")
                    .isNotNull(RepairOrder::getAcceptTime)
                    .isNotNull(RepairOrder::getFinishTime));
            List<RepairOrder> doneOrders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .eq(RepairOrder::getStatus, "已完成")
                    .isNotNull(RepairOrder::getAcceptTime)
                    .isNotNull(RepairOrder::getFinishTime)
                    .last("limit 30"));
            double avgHours = 24D;
            if (hisCount != null && hisCount > 0 && !doneOrders.isEmpty()) {
                double sum = 0D;
                for (RepairOrder d : doneOrders) {
                    sum += Math.max(1, java.time.Duration.between(d.getAcceptTime(), d.getFinishTime()).toHours());
                }
                avgHours = sum / doneOrders.size();
            }

            boolean skillMatched = matchSkill(m, device);
            double loadScore = repairDispatchAlgorithm.calcMaintainerLoad(
                    unfinished == null ? 0L : unfinished,
                    processing == null ? 0L : processing,
                    avgHours,
                    skillMatched);
            double recommendationScore = priorityScore - loadScore;
            String reason = buildRecommendReason(priorityScore, loadScore, unfinished, processing, avgHours, skillMatched, device);
            list.add(MaintainerRecommendVO.builder()
                    .maintainerId(m.getId())
                    .maintainerName(m.getRealName())
                    .priorityScore(priorityScore)
                    .loadScore(loadScore)
                    .recommendationScore(recommendationScore)
                    .unfinishedCount(unfinished == null ? 0L : unfinished)
                    .processingCount(processing == null ? 0L : processing)
                    .avgHandleHours(avgHours)
                    .skillMatched(skillMatched)
                    .reason(reason)
                    .build());
        }
        list.sort((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()));
        return list;
    }

    private boolean matchSkill(SysUser maintainer, NetworkDevice device) {
        if (device == null) return true;
        String tag = ((maintainer.getDepartment() == null ? "" : maintainer.getDepartment()) + " "
                + (maintainer.getRole() == null ? "" : maintainer.getRole()) + " "
                + (maintainer.getRealName() == null ? "" : maintainer.getRealName())).toLowerCase();
        String type = (device.getDeviceType() == null ? "" : device.getDeviceType()).toLowerCase();
        if (type.contains("防火墙") || type.contains("核心") || type.contains("路由")) return tag.contains("网络") || tag.contains("核心");
        if (type.contains("交换") || type.contains("ap") || type.contains("无线")) return tag.contains("无线") || tag.contains("接入") || tag.contains("网络");
        return true;
    }

    private String buildRecommendReason(double priorityScore, double loadScore, Long unfinished, Long processing,
                                        double avgHours, boolean skillMatched, NetworkDevice device) {
        return "优先级分=" + String.format("%.1f", priorityScore)
                + "，负载分=" + String.format("%.1f", loadScore)
                + "（未完成" + (unfinished == null ? 0 : unfinished) + "、处理中" + (processing == null ? 0 : processing)
                + "、平均处理时长" + String.format("%.1f", avgHours) + "h、技能" + (skillMatched ? "匹配" : "不匹配") + "）"
                + "；设备类型=" + (device == null ? "未知" : device.getDeviceType());
    }

    private String generateOrderNo() {
        String datePrefix = "RO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 8; i++) {
            String candidate = datePrefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
                    + ThreadLocalRandom.current().nextInt(100000, 999999);
            Long count = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getOrderNo, candidate));
            if (count == null || count == 0) return candidate;
        }
        throw new BusinessException("工单号生成失败，请重试");
    }

    private void validateStatusTransition(String from, String to) {
        if (Objects.equals(from, to)) return;
        Map<String, Set<String>> transitionMap = new HashMap<>();
        transitionMap.put("待提交", new HashSet<>(Collections.singletonList("已提交/待审核")));
        transitionMap.put("已提交/待审核", new HashSet<>(Arrays.asList("审核通过", "审核驳回", "已取消")));
        transitionMap.put("审核通过", new HashSet<>(Collections.singletonList("待分配")));
        transitionMap.put("审核驳回", new HashSet<>(Collections.singletonList("已取消")));
        transitionMap.put("待分配", new HashSet<>(Arrays.asList("已分配", "已取消")));
        transitionMap.put("已分配", new HashSet<>(Collections.singletonList("待接单")));
        transitionMap.put("待接单", new HashSet<>(Arrays.asList("维修人员已接单", "待分配")));
        transitionMap.put("维修人员已接单", new HashSet<>(Collections.singletonList("维修中")));
        transitionMap.put("维修中", new HashSet<>(Arrays.asList("待采购/待配件", "申请延期中", "待验收/待确认", "已关闭")));
        transitionMap.put("待采购/待配件", new HashSet<>(Arrays.asList("维修中", "待验收/待确认")));
        transitionMap.put("申请延期中", new HashSet<>(Arrays.asList("延期已批准", "维修中")));
        transitionMap.put("延期已批准", new HashSet<>(Arrays.asList("维修中", "待验收/待确认")));
        transitionMap.put("待验收/待确认", new HashSet<>(Arrays.asList("已完成", "维修中", "已关闭")));
        transitionMap.put("已完成", new HashSet<>(Collections.singletonList("已关闭")));
        transitionMap.put("已关闭", Collections.emptySet());
        transitionMap.put("已取消", Collections.emptySet());
        Set<String> nextSet = transitionMap.getOrDefault(from, Collections.emptySet());
        if (!nextSet.contains(to)) throw new BusinessException("状态流转不合法：" + from + " -> " + to);
    }

    private void moveStatus(RepairOrder order, String toStatus, Integer progress, Long userId, String role, String remark, String action) {
        String from = order.getStatus();
        order.setStatus(toStatus);
        if (progress != null) order.setProgress(progress);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, toStatus, action, userId, role, remark);
        addBusinessLog(order, action, userId, role, from, toStatus, remark);
    }

    private void addFlow(Long orderId, String fromStatus, String toStatus, String action, Long userId, String role, String remark) {
        RepairOrderFlow flow = new RepairOrderFlow();
        flow.setRepairOrderId(orderId);
        flow.setFromStatus(fromStatus);
        flow.setToStatus(toStatus);
        flow.setAction(action);
        flow.setOperatorId(userId);
        if (userId != null) {
            SysUser operator = userMapper.selectById(userId);
            if (operator != null) {
                flow.setOperatorEmployeeNo(operator.getEmployeeNo());
                flow.setOperatorName(operator.getRealName());
            }
        }
        flow.setOperatorRole(role);
        flow.setOperationType(action);
        flow.setRemark(remark);
        flow.setCreateTime(LocalDateTime.now());
        repairOrderFlowMapper.insert(flow);
    }

    private void addBusinessLog(RepairOrder order, String action, Long userId, String role, String fromStatus, String toStatus, String remark) {
        BusinessLog log = new BusinessLog();
        log.setBusinessType("REPAIR_ORDER");
        log.setBusinessNo(order.getOrderNo());
        log.setAction(action);
        log.setOperatorId(userId);
        if (userId != null) {
            SysUser operator = userMapper.selectById(userId);
            if (operator != null) {
                log.setOperatorEmployeeNo(operator.getEmployeeNo());
                log.setOperatorName(operator.getRealName());
            }
        }
        log.setStatus(toStatus);
        String safeRemark = remark == null || remark.trim().isEmpty() ? "无" : remark.trim();
        log.setContent(String.format("状态：%s -> %s；角色：%s；处理意见：%s",
                fromStatus == null ? "-" : fromStatus, toStatus == null ? "-" : toStatus, role == null ? "-" : role, safeRemark));
        log.setCreateTime(LocalDateTime.now());
        businessLogMapper.insert(log);
    }

    private void saveFeedback(RepairOrder order, RepairOrderActionDTO dto, Long userId, String confirmResult) {
        SysUser user = userMapper.selectById(userId);
        RepairFeedback feedback = new RepairFeedback();
        feedback.setRepairOrderId(order.getId());
        feedback.setUserId(userId);
        feedback.setUserEmployeeNo(user == null ? null : user.getEmployeeNo());
        feedback.setConfirmResult(confirmResult);
        feedback.setSatisfactionScore(dto.getSatisfactionScore());
        feedback.setFeedbackContent(dto.getFeedbackContent());
        feedback.setConfirmTime(LocalDateTime.now());
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        repairFeedbackMapper.insert(feedback);
        addBusinessLog(order, "USER_FEEDBACK", userId, "user", order.getStatus(), order.getStatus(),
                "确认结果：" + confirmResult + "；满意度：" + (dto.getSatisfactionScore() == null ? "-" : dto.getSatisfactionScore())
                        + "；反馈：" + (dto.getFeedbackContent() == null ? "无" : dto.getFeedbackContent()));
    }

    private TimeRange resolveRange(String rangeType, String start, String end) {
        String normalized = rangeType == null ? "month" : rangeType.trim().toLowerCase();
        LocalDate now = LocalDate.now();
        if ("day".equals(normalized)) {
            return new TimeRange("day", now.atStartOfDay(), now.atTime(LocalTime.MAX));
        }
        if ("halfyear".equals(normalized)) {
            LocalDate begin = now.withDayOfMonth(1).minusMonths(5);
            return new TimeRange("halfyear", begin.atStartOfDay(), now.atTime(LocalTime.MAX));
        }
        if ("year".equals(normalized)) {
            LocalDate begin = now.withDayOfYear(1);
            return new TimeRange("year", begin.atStartOfDay(), now.atTime(LocalTime.MAX));
        }
        if ("custom".equals(normalized) && start != null && end != null) {
            LocalDateTime customStart = parseDateTime(start, true);
            LocalDateTime customEnd = parseDateTime(end, false);
            if (customStart != null && customEnd != null && !customEnd.isBefore(customStart)) {
                return new TimeRange("custom", customStart, customEnd);
            }
        }
        LocalDate begin = now.withDayOfMonth(1);
        return new TimeRange("month", begin.atStartOfDay(), now.atTime(LocalTime.MAX));
    }

    private LocalDateTime parseDateTime(String value, boolean startOfDay) {
        if (value == null || value.trim().isEmpty()) return null;
        String v = value.trim();
        try {
            if (v.length() <= 10) {
                LocalDate d = LocalDate.parse(v);
                return startOfDay ? d.atStartOfDay() : d.atTime(LocalTime.MAX);
            }
            return LocalDateTime.parse(v.replace(" ", "T"));
        } catch (Exception ignore) {
            return null;
        }
    }

    private double avgRepairHours(List<RepairOrder> orders) {
        return orders.stream()
                .filter(o -> o.getStartRepairTime() != null && o.getFinishTime() != null && !o.getFinishTime().isBefore(o.getStartRepairTime()))
                .mapToDouble(o -> Duration.between(o.getStartRepairTime(), o.getFinishTime()).toMinutes() / 60D)
                .average().orElse(0D);
    }

    private List<Map<String, Object>> maintainerAvgHours(List<RepairOrder> orders) {
        Map<String, Double> avgMap = orders.stream()
                .filter(o -> o.getAssignMaintainerName() != null && !o.getAssignMaintainerName().trim().isEmpty())
                .filter(o -> o.getStartRepairTime() != null && o.getFinishTime() != null && !o.getFinishTime().isBefore(o.getStartRepairTime()))
                .collect(Collectors.groupingBy(RepairOrder::getAssignMaintainerName,
                        Collectors.averagingDouble(o -> Duration.between(o.getStartRepairTime(), o.getFinishTime()).toMinutes() / 60D)));
        return avgMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    Map<String, Object> filters = new HashMap<>();
                    filters.put("assignMaintainerName", e.getKey());
                    item.put("filters", filters);
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> toRank(List<RepairOrder> orders, java.util.function.Function<RepairOrder, String> fn,
                                             Integer limit, String filterKey) {
        Map<String, Long> grouped = orders.stream()
                .map(fn)
                .filter(v -> v != null && !v.trim().isEmpty())
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit == null ? 10 : limit)
                .map(e -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", e.getKey());
                    row.put("value", e.getValue());
                    Map<String, Object> filters = new HashMap<>();
                    filters.put(filterKey, e.getKey());
                    row.put("filters", filters);
                    return row;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> satisfactionStats(List<RepairFeedback> feedbacks) {
        Map<String, Object> map = new HashMap<>();
        map.put("count", feedbacks.size());
        map.put("avgScore", feedbacks.stream()
                .filter(v -> v.getSatisfactionScore() != null)
                .mapToInt(RepairFeedback::getSatisfactionScore).average().orElse(0D));
        Map<Integer, Long> distribution = feedbacks.stream()
                .filter(v -> v.getSatisfactionScore() != null)
                .collect(Collectors.groupingBy(RepairFeedback::getSatisfactionScore, Collectors.counting()));
        List<Map<String, Object>> distList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", i + "分");
            row.put("score", i);
            row.put("value", distribution.getOrDefault(i, 0L));
            distList.add(row);
        }
        map.put("distribution", distList);
        map.put("unresolvedCount", feedbacks.stream().filter(v -> "未解决".equals(v.getConfirmResult())).count());
        return map;
    }

    private Map<String, Object> predictionAnalysis(List<RepairOrder> orders) {
        List<RepairOrder> comparable = orders.stream()
                .filter(o -> o.getExpectedFinishTime() != null && o.getFinishTime() != null)
                .collect(Collectors.toList());
        Map<String, Object> map = new HashMap<>();
        map.put("count", comparable.size());
        double avgError = comparable.stream()
                .mapToDouble(o -> Math.abs(Duration.between(o.getExpectedFinishTime(), o.getFinishTime()).toMinutes()) / 60D)
                .average().orElse(0D);
        map.put("avgAbsErrorHours", avgError);
        map.put("within4h", comparable.stream().filter(o -> Math.abs(Duration.between(o.getExpectedFinishTime(), o.getFinishTime()).toMinutes()) <= 240).count());
        map.put("within24h", comparable.stream().filter(o -> Math.abs(Duration.between(o.getExpectedFinishTime(), o.getFinishTime()).toMinutes()) <= 1440).count());
        return map;
    }

    private List<Map<String, Object>> buildTrend(List<RepairOrder> orders, TimeRange tr) {
        boolean monthly = "year".equals(tr.rangeType) || "halfyear".equals(tr.rangeType) ||
                ChronoUnit.DAYS.between(tr.start.toLocalDate(), tr.end.toLocalDate()) > 62;
        DateTimeFormatter fmt = monthly ? DateTimeFormatter.ofPattern("yyyy-MM") : DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> reportCount = orders.stream().filter(o -> o.getReportTime() != null)
                .collect(Collectors.groupingBy(o -> o.getReportTime().format(fmt), TreeMap::new, Collectors.counting()));
        Map<String, Long> finishedCount = orders.stream()
                .filter(o -> o.getFinishTime() != null && RepairOrderStatusEnum.FINISHED.getLabel().equals(o.getStatus()))
                .collect(Collectors.groupingBy(o -> o.getFinishTime().format(fmt), TreeMap::new, Collectors.counting()));
        Set<String> axis = new LinkedHashSet<>();
        axis.addAll(reportCount.keySet());
        axis.addAll(finishedCount.keySet());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String key : axis) {
            Map<String, Object> row = new HashMap<>();
            row.put("bucket", key);
            row.put("reportCount", reportCount.getOrDefault(key, 0L));
            row.put("finishedCount", finishedCount.getOrDefault(key, 0L));
            rows.add(row);
        }
        return rows;
    }

    private double ratio(long num, long den) {
        if (den <= 0) return 0D;
        return ((double) num / (double) den) * 100D;
    }

    private Page<Map<String, Object>> pageFeedbackOrders(Long current, Long size, LambdaQueryWrapper<RepairFeedback> wrapper) {
        Page<RepairFeedback> feedbackPage = repairFeedbackMapper.selectPage(new Page<>(current, size), wrapper);
        Page<Map<String, Object>> result = new Page<>(feedbackPage.getCurrent(), feedbackPage.getSize(), feedbackPage.getTotal());
        if (feedbackPage.getRecords() == null || feedbackPage.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }
        List<Long> orderIds = feedbackPage.getRecords().stream().map(RepairFeedback::getRepairOrderId).collect(java.util.stream.Collectors.toList());
        Map<Long, RepairOrder> orderMap = repairOrderMapper.selectBatchIds(orderIds).stream()
                .collect(java.util.stream.Collectors.toMap(RepairOrder::getId, v -> v, (a, b) -> a));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RepairFeedback feedback : feedbackPage.getRecords()) {
            RepairOrder order = orderMap.get(feedback.getRepairOrderId());
            Map<String, Object> row = new HashMap<>();
            row.put("feedbackId", feedback.getId());
            row.put("repairOrderId", feedback.getRepairOrderId());
            row.put("orderNo", order == null ? null : order.getOrderNo());
            row.put("title", order == null ? null : order.getTitle());
            row.put("orderStatus", order == null ? null : order.getStatus());
            row.put("assignMaintainerName", order == null ? null : order.getAssignMaintainerName());
            row.put("confirmResult", feedback.getConfirmResult());
            row.put("satisfactionScore", feedback.getSatisfactionScore());
            row.put("feedbackContent", feedback.getFeedbackContent());
            row.put("confirmTime", feedback.getConfirmTime());
            rows.add(row);
        }
        result.setRecords(rows);
        return result;
    }

    private void validateFeedbackInput(RepairOrderActionDTO dto) {
        if (dto.getSatisfactionScore() == null || dto.getSatisfactionScore() < 1 || dto.getSatisfactionScore() > 5) {
            throw new BusinessException("请填写1~5分满意度");
        }
        if (dto.getFeedbackContent() == null || dto.getFeedbackContent().trim().isEmpty()) {
            throw new BusinessException("请填写反馈意见");
        }
    }

    private void requireRole(String current, String expected) {
        if (!expected.equals(current)) throw new BusinessException("当前角色无权执行该操作");
    }

    private void checkStatus(String current, String expected) {
        if (!expected.equals(current)) throw new BusinessException("当前状态不允许该操作");
    }

    private void checkMaintainerScope(RepairOrder order, Long userId) {
        if (!userId.equals(order.getAssignMaintainerId())) throw new BusinessException("仅可处理分配给自己的工单");
    }

    private void syncDeviceStatus(Long deviceId) {
        NetworkDevice dbDevice = deviceMapper.selectById(deviceId);
        if (dbDevice == null) return;

        Long activeOrderCount = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getDeviceId, deviceId)
                .notIn(RepairOrder::getStatus, Arrays.asList("已完成", "已关闭", "已取消", "审核驳回")));
        Long totalOrderCount = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getDeviceId, deviceId));
        Long totalRepairCount = repairRecordMapper.selectCount(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getDeviceId, deviceId));
        RepairOrder latestOrder = repairOrderMapper.selectOne(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getDeviceId, deviceId)
                .orderByDesc(RepairOrder::getReportTime)
                .last("limit 1"));

        Map<String, Long> reasonStats = new LinkedHashMap<>();
        List<RepairRecord> records = repairRecordMapper.selectList(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getDeviceId, deviceId)
                .orderByDesc(RepairRecord::getId));
        for (RepairRecord record : records) {
            String reason = record.getFaultReason();
            if (reason == null || reason.trim().isEmpty()) continue;
            reasonStats.put(reason, reasonStats.getOrDefault(reason, 0L) + 1L);
        }
        String reasonStatsText = "";
        if (!reasonStats.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            reasonStats.forEach((k, v) -> sb.append(k).append(":").append(v).append(";"));
            reasonStatsText = sb.substring(0, sb.length() - 1);
        }

        NetworkDevice update = new NetworkDevice();
        update.setId(deviceId);
        update.setLastFaultTime(latestOrder == null ? null : latestOrder.getReportTime());
        update.setTotalRepairRequests(totalOrderCount == null ? 0 : totalOrderCount.intValue());
        update.setTotalRepairCount(totalRepairCount == null ? 0 : totalRepairCount.intValue());
        update.setFaultReasonStats(reasonStatsText);
        if (!Arrays.asList("停用", "报废").contains(dbDevice.getStatus())) {
            update.setStatus(activeOrderCount != null && activeOrderCount > 0 ? "维修中" : "正常");
        }
        update.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(update);
    }
}
