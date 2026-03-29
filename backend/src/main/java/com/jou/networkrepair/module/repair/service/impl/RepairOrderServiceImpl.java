package com.jou.networkrepair.module.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderAuditDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCloseDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderDelayApproveDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderFeedbackDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderReassignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.enums.RepairOrderStatusEnum;
import com.jou.networkrepair.module.repair.mapper.RepairOrderFlowMapper;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import com.jou.networkrepair.module.repair.service.RepairOrderService;
import com.jou.networkrepair.module.repair.vo.AssignmentRecommendationVO;
import com.jou.networkrepair.module.repair.vo.RepairEstimateVO;
import com.jou.networkrepair.module.system.entity.BusinessLog;
import com.jou.networkrepair.module.system.entity.FileAttachment;
import com.jou.networkrepair.module.system.entity.RepairFeedback;
import com.jou.networkrepair.module.system.mapper.BusinessLogMapper;
import com.jou.networkrepair.module.system.mapper.FileAttachmentMapper;
import com.jou.networkrepair.module.system.mapper.RepairFeedbackMapper;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairOrderServiceImpl implements RepairOrderService {
    private static final Set<String> PRIORITY_SET = new HashSet<>(Arrays.asList("低", "中", "高"));

    private final RepairOrderMapper repairOrderMapper;
    private final RepairOrderFlowMapper repairOrderFlowMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final BusinessLogMapper businessLogMapper;
    private final RepairFeedbackMapper repairFeedbackMapper;
    private final FileAttachmentMapper fileAttachmentMapper;
    private final RepairRecordMapper repairRecordMapper;

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

        boolean approvalRequired = device.getRepairApprovalRequired() != null && device.getRepairApprovalRequired() == 1;
        order.setStatus(approvalRequired ? RepairOrderStatusEnum.SUBMITTED.getLabel() : RepairOrderStatusEnum.PENDING_ASSIGN.getLabel());
        order.setProgress(approvalRequired ? 5 : 20);
        order.setReportTime(now);
        order.setRemark(dto.getRemark());
        order.setCreateBy(userId);
        order.setUpdateBy(userId);
        order.setCreateTime(now);
        order.setUpdateTime(now);

        repairOrderMapper.insert(order);
        String createRemark = approvalRequired ? "提交报修工单，等待管理员审核" : "提交报修工单，设备无需审核直接进入待分配";
        addFlow(order.getId(), null, order.getStatus(), "CREATE", userId, "user", createRemark);
        addBusinessLog(order, "CREATE", userId, "user", null, order.getStatus(), createRemark);
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
        if (!RepairOrderStatusEnum.PENDING_ASSIGN.getLabel().equals(order.getStatus())) {
            throw new BusinessException("未审核通过进入待分配前，不能执行分配");
        }
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
    public void auditByAdmin(Long id, RepairOrderAuditDTO dto, Long userId, String role) {
        if (!"admin".equals(role)) throw new BusinessException("仅管理员可审核");
        RepairOrder order = requireOrder(id);
        if (!RepairOrderStatusEnum.SUBMITTED.getLabel().equals(order.getStatus())) {
            throw new BusinessException("当前工单不在待审核状态");
        }
        String from = order.getStatus();
        LocalDateTime now = LocalDateTime.now();
        SysUser admin = userMapper.selectById(userId);
        order.setAuditBy(userId);
        order.setAuditTime(now);
        if (admin != null) {
            order.setAuditByEmployeeNo(admin.getEmployeeNo());
            order.setAuditByName(admin.getRealName());
        }
        if (Boolean.TRUE.equals(dto.getApproved())) {
            order.setStatus(RepairOrderStatusEnum.PENDING_ASSIGN.getLabel());
            order.setProgress(Math.max(defaultZero(order.getProgress()), 20));
            String remark = dto.getRemark() == null ? "管理员审核通过，进入待分配" : dto.getRemark();
            order.setRemark(remark);
            order.setUpdateBy(userId);
            order.setUpdateTime(now);
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), from, order.getStatus(), "ADMIN_APPROVE", userId, role, remark);
            addBusinessLog(order, "ADMIN_APPROVE", userId, role, from, order.getStatus(), remark);
        } else {
            order.setStatus(RepairOrderStatusEnum.AUDIT_REJECTED.getLabel());
            order.setProgress(0);
            String remark = dto.getRemark() == null ? "管理员审核驳回" : dto.getRemark();
            order.setRemark(remark);
            order.setUpdateBy(userId);
            order.setUpdateTime(now);
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), from, order.getStatus(), "ADMIN_REJECT", userId, role, remark);
            addBusinessLog(order, "ADMIN_REJECT", userId, role, from, order.getStatus(), remark);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignByAdmin(Long id, RepairOrderReassignDTO dto, Long userId, String role) {
        if (!"admin".equals(role)) throw new BusinessException("仅管理员可改派");
        RepairOrder order = requireOrder(id);
        if (order.getAssignMaintainerId() == null) throw new BusinessException("工单尚未分配，不能改派");
        if (RepairOrderStatusEnum.terminalStatuses().contains(order.getStatus())) {
            throw new BusinessException("终态工单不允许改派");
        }
        SysUser maintainer = userMapper.selectById(dto.getAssignMaintainerId());
        if (maintainer == null) throw new BusinessException("维修人员不存在");
        String from = order.getStatus();
        order.setAssignMaintainerId(maintainer.getId());
        order.setAssignMaintainerEmployeeNo(maintainer.getEmployeeNo());
        order.setAssignMaintainerName(maintainer.getRealName());
        order.setAssignBy(userId);
        order.setAssignTime(LocalDateTime.now());
        order.setStatus(RepairOrderStatusEnum.PENDING_ACCEPT.getLabel());
        order.setAcceptTime(null);
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        String remark = dto.getRemark() == null ? "管理员改派维修人员：" + maintainer.getRealName() : dto.getRemark();
        addFlow(order.getId(), from, order.getStatus(), "ADMIN_REASSIGN", userId, role, remark);
        addBusinessLog(order, "ADMIN_REASSIGN", userId, role, from, order.getStatus(), remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveDelayByAdmin(Long id, RepairOrderDelayApproveDTO dto, Long userId, String role) {
        if (!"admin".equals(role)) throw new BusinessException("仅管理员可审批延期");
        RepairOrder order = requireOrder(id);
        if (!RepairOrderStatusEnum.DELAY_APPLYING.getLabel().equals(order.getStatus())) {
            throw new BusinessException("仅申请延期中的工单可审批");
        }
        String from = order.getStatus();
        if (Boolean.TRUE.equals(dto.getApproved())) {
            order.setStatus(RepairOrderStatusEnum.DELAY_APPROVED.getLabel());
            if (dto.getDelayedExpectedFinishTime() != null) {
                order.setDelayedExpectedFinishTime(dto.getDelayedExpectedFinishTime());
                order.setExpectedFinishTime(dto.getDelayedExpectedFinishTime());
            }
            String remark = dto.getRemark() == null ? "管理员审批延期通过" : dto.getRemark();
            order.setRemark(remark);
            order.setUpdateBy(userId);
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), from, order.getStatus(), "ADMIN_DELAY_APPROVE", userId, role, remark);
            addBusinessLog(order, "ADMIN_DELAY_APPROVE", userId, role, from, order.getStatus(), remark);
        } else {
            order.setStatus(RepairOrderStatusEnum.IN_PROGRESS.getLabel());
            String remark = dto.getRemark() == null ? "管理员审批延期驳回，退回维修中" : dto.getRemark();
            order.setRemark(remark);
            order.setUpdateBy(userId);
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), from, order.getStatus(), "ADMIN_DELAY_REJECT", userId, role, remark);
            addBusinessLog(order, "ADMIN_DELAY_REJECT", userId, role, from, order.getStatus(), remark);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeByAdmin(Long id, RepairOrderCloseDTO dto, Long userId, String role) {
        if (!"admin".equals(role)) throw new BusinessException("仅管理员可关闭");
        RepairOrder order = requireOrder(id);
        if (RepairOrderStatusEnum.terminalStatuses().contains(order.getStatus())) {
            throw new BusinessException("终态工单不能重复关闭");
        }
        if (Boolean.TRUE.equals(dto.getForceClose())) {
            if (dto.getCloseReason() == null || dto.getCloseReason().trim().isEmpty()) {
                throw new BusinessException("强制关闭必须填写原因");
            }
        } else {
            if (!RepairOrderStatusEnum.COMPLETED.getLabel().equals(order.getStatus())
                    || !"已解决".equals(order.getUserConfirmResult())) {
                throw new BusinessException("用户未确认前不能直接关闭");
            }
        }
        String from = order.getStatus();
        order.setStatus(RepairOrderStatusEnum.CLOSED.getLabel());
        order.setCloseReason(dto.getCloseReason());
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        String op = Boolean.TRUE.equals(dto.getForceClose()) ? "ADMIN_FORCE_CLOSE" : "ADMIN_CLOSE";
        addFlow(order.getId(), from, order.getStatus(), op, userId, role, dto.getCloseReason());
        addBusinessLog(order, op, userId, role, from, order.getStatus(), dto.getCloseReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainerAccept(Long id, String remark, Long userId, String role) {
        requireMaintainer(role);
        RepairOrder order = requireOrder(id);
        ensureMaintainerScope(order, userId);
        if (!RepairOrderStatusEnum.PENDING_ACCEPT.getLabel().equals(order.getStatus())) {
            throw new BusinessException("未分配不能接单");
        }
        String from = order.getStatus();
        order.setStatus(RepairOrderStatusEnum.ACCEPTED.getLabel());
        order.setAcceptTime(LocalDateTime.now());
        order.setProgress(Math.max(defaultZero(order.getProgress()), 40));
        order.setRemark(remark);
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, order.getStatus(), "MAINTAINER_ACCEPT", userId, role, remark);
        addBusinessLog(order, "MAINTAINER_ACCEPT", userId, role, from, order.getStatus(), remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainerReject(Long id, String reason, Long userId, String role) {
        requireMaintainer(role);
        if (reason == null || reason.trim().isEmpty()) throw new BusinessException("拒单必须填写原因");
        RepairOrder order = requireOrder(id);
        ensureMaintainerScope(order, userId);
        if (!RepairOrderStatusEnum.PENDING_ACCEPT.getLabel().equals(order.getStatus())) {
            throw new BusinessException("当前状态不能拒单");
        }
        String from = order.getStatus();
        order.setStatus(RepairOrderStatusEnum.PENDING_ASSIGN.getLabel());
        order.setAssignMaintainerId(null);
        order.setAssignMaintainerEmployeeNo(null);
        order.setAssignMaintainerName(null);
        order.setAcceptTime(null);
        order.setRemark(reason);
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, order.getStatus(), "MAINTAINER_REJECT", userId, role, reason);
        addBusinessLog(order, "MAINTAINER_REJECT", userId, role, from, order.getStatus(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainerStart(Long id, String remark, Long userId, String role) {
        requireMaintainer(role);
        RepairOrder order = requireOrder(id);
        ensureMaintainerScope(order, userId);
        if (!RepairOrderStatusEnum.ACCEPTED.getLabel().equals(order.getStatus())) {
            throw new BusinessException("未接单不能开始维修");
        }
        String from = order.getStatus();
        order.setStatus(RepairOrderStatusEnum.IN_PROGRESS.getLabel());
        order.setStartRepairTime(LocalDateTime.now());
        order.setProgress(Math.max(defaultZero(order.getProgress()), 50));
        order.setRemark(remark);
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, order.getStatus(), "MAINTAINER_START", userId, role, remark);
        addBusinessLog(order, "MAINTAINER_START", userId, role, from, order.getStatus(), remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainerUpdateProgress(Long id, RepairOrderStatusDTO dto, Long userId, String role) {
        requireMaintainer(role);
        RepairOrder order = requireOrder(id);
        ensureMaintainerScope(order, userId);
        if (!Arrays.asList(RepairOrderStatusEnum.IN_PROGRESS.getLabel(), RepairOrderStatusEnum.DELAY_APPROVED.getLabel(),
                RepairOrderStatusEnum.PENDING_PARTS.getLabel()).contains(order.getStatus())) {
            throw new BusinessException("当前状态不允许更新维修进度");
        }
        if (dto.getProgress() == null) throw new BusinessException("请填写进度百分比");
        order.setProgress(dto.getProgress());
        if (dto.getHandleDescription() != null) order.setHandleDescription(dto.getHandleDescription());
        if (dto.getExpectedFinishTime() != null) order.setExpectedFinishTime(dto.getExpectedFinishTime());
        order.setRemark(dto.getRemark());
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        if (dto.getScenePhotoUrls() != null) {
            for (String url : dto.getScenePhotoUrls()) {
                if (url == null || url.trim().isEmpty()) continue;
                FileAttachment fa = new FileAttachment();
                fa.setBusinessType("REPAIR_ORDER");
                fa.setBusinessId(order.getId());
                fa.setBizType("REPAIR_ORDER");
                fa.setBizId(order.getId());
                fa.setFileName("现场照片");
                fa.setFileUrl(url);
                fa.setFileType("image");
                fa.setUploaderId(userId);
                fa.setUploadTime(LocalDateTime.now());
                fa.setRemark("维修现场照片");
                fa.setCreateTime(LocalDateTime.now());
                fa.setUpdateTime(LocalDateTime.now());
                fileAttachmentMapper.insert(fa);
            }
        }
        addFlow(order.getId(), order.getStatus(), order.getStatus(), "MAINTAINER_PROGRESS", userId, role, dto.getRemark());
        addBusinessLog(order, "MAINTAINER_PROGRESS", userId, role, order.getStatus(), order.getStatus(),
                "进度更新至" + dto.getProgress() + "%，处理说明：" + (dto.getHandleDescription() == null ? "-" : dto.getHandleDescription()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainerApplyDelay(Long id, RepairOrderStatusDTO dto, Long userId, String role) {
        requireMaintainer(role);
        RepairOrder order = requireOrder(id);
        ensureMaintainerScope(order, userId);
        if (!Arrays.asList(RepairOrderStatusEnum.IN_PROGRESS.getLabel(), RepairOrderStatusEnum.DELAY_APPROVED.getLabel()).contains(order.getStatus())) {
            throw new BusinessException("当前状态不允许申请延期");
        }
        String from = order.getStatus();
        order.setApplyDelay(1);
        order.setStatus(RepairOrderStatusEnum.DELAY_APPLYING.getLabel());
        order.setDelayedExpectedFinishTime(dto.getDelayedExpectedFinishTime());
        order.setRemark(dto.getRemark());
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, order.getStatus(), "MAINTAINER_DELAY_APPLY", userId, role, dto.getRemark());
        addBusinessLog(order, "MAINTAINER_DELAY_APPLY", userId, role, from, order.getStatus(), dto.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainerApplyParts(Long id, RepairOrderStatusDTO dto, Long userId, String role) {
        requireMaintainer(role);
        RepairOrder order = requireOrder(id);
        ensureMaintainerScope(order, userId);
        if (!Arrays.asList(RepairOrderStatusEnum.IN_PROGRESS.getLabel(), RepairOrderStatusEnum.DELAY_APPROVED.getLabel()).contains(order.getStatus())) {
            throw new BusinessException("当前状态不允许申请配件");
        }
        String from = order.getStatus();
        order.setNeedPurchaseParts(1);
        order.setStatus(RepairOrderStatusEnum.PENDING_PARTS.getLabel());
        order.setPartsDescription(dto.getPartsDescription());
        order.setRemark(dto.getRemark());
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, order.getStatus(), "MAINTAINER_PARTS_APPLY", userId, role, dto.getRemark());
        addBusinessLog(order, "MAINTAINER_PARTS_APPLY", userId, role, from, order.getStatus(), dto.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainerFinish(Long id, RepairOrderStatusDTO dto, Long userId, String role) {
        requireMaintainer(role);
        RepairOrder order = requireOrder(id);
        ensureMaintainerScope(order, userId);
        if (!Arrays.asList(RepairOrderStatusEnum.IN_PROGRESS.getLabel(), RepairOrderStatusEnum.DELAY_APPROVED.getLabel(),
                RepairOrderStatusEnum.PENDING_PARTS.getLabel()).contains(order.getStatus())) {
            throw new BusinessException("当前状态不允许提交完工");
        }
        String from = order.getStatus();
        order.setStatus(RepairOrderStatusEnum.PENDING_CONFIRM.getLabel());
        order.setProgress(100);
        order.setFinishTime(LocalDateTime.now());
        if (dto.getHandleDescription() != null) order.setHandleDescription(dto.getHandleDescription());
        order.setRemark(dto.getRemark());
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, order.getStatus(), "MAINTAINER_FINISH", userId, role, dto.getRemark());
        addBusinessLog(order, "MAINTAINER_FINISH", userId, role, from, order.getStatus(), dto.getRemark());
        autoDepositRepairRecord(order, userId, dto);
        recordPredictionError(order, userId, role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, RepairOrderStatusDTO dto, Long userId, String role) {
        if (!RepairOrderStatusEnum.containsLabel(dto.getStatus())) {
            throw new BusinessException("不支持的工单状态");
        }
        RepairOrder order = requireOrder(id);
        ensureScope(order, userId, role);
        assertTransitionAllowed(order, dto.getStatus(), role);

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
        if (RepairOrderStatusEnum.PENDING_CONFIRM.getLabel().equals(dto.getStatus())) {
            if (order.getProgress() == null || order.getProgress() < 100) {
                throw new BusinessException("维修未完成不能进入待验收");
            }
        }
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
    @Transactional(rollbackFor = Exception.class)
    public void cancelByUser(Long id, String remark, Long userId) {
        RepairOrder order = requireOrder(id);
        if (!userId.equals(order.getReporterId())) throw new BusinessException("只能撤销自己的工单");
        if (!Arrays.asList("已提交/待审核", "审核驳回").contains(order.getStatus())) {
            throw new BusinessException("当前状态不允许撤销");
        }
        String from = order.getStatus();
        order.setStatus(RepairOrderStatusEnum.CANCELED.getLabel());
        order.setProgress(0);
        order.setUpdateBy(userId);
        order.setUpdateTime(LocalDateTime.now());
        order.setRemark(remark);
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), from, order.getStatus(), "USER_CANCEL", userId, "user", remark);
        addBusinessLog(order, "USER_CANCEL", userId, "user", from, order.getStatus(), remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void feedbackByUser(Long id, RepairOrderFeedbackDTO dto, Long userId) {
        RepairOrder order = requireOrder(id);
        if (!userId.equals(order.getReporterId())) throw new BusinessException("只能反馈自己的工单");
        if (!RepairOrderStatusEnum.PENDING_CONFIRM.getLabel().equals(order.getStatus())) {
            throw new BusinessException("仅待验收状态可提交验收反馈");
        }
        LocalDateTime now = LocalDateTime.now();
        String from = order.getStatus();
        boolean unresolved = "未解决".equals(dto.getConfirmResult());

        order.setUserConfirmResult(dto.getConfirmResult());
        order.setSatisfactionScore(dto.getSatisfactionScore());
        order.setFeedback(dto.getFeedbackContent());
        order.setConfirmTime(now);
        order.setRemark(dto.getRemark());
        if (unresolved) {
            order.setStatus(RepairOrderStatusEnum.IN_PROGRESS.getLabel());
            order.setProgress(60);
            if (dto.getRemark() == null || dto.getRemark().trim().isEmpty()) {
                order.setRemark("用户反馈未解决，退回维修中");
            }
        } else {
            order.setStatus(RepairOrderStatusEnum.COMPLETED.getLabel());
            order.setProgress(100);
            order.setFinishTime(now);
        }
        order.setUpdateBy(userId);
        order.setUpdateTime(now);
        repairOrderMapper.updateById(order);

        RepairFeedback feedback = new RepairFeedback();
        feedback.setRepairOrderId(order.getId());
        feedback.setUserId(userId);
        feedback.setUserEmployeeNo(order.getReporterEmployeeNo());
        feedback.setConfirmResult(dto.getConfirmResult());
        feedback.setSatisfactionScore(dto.getSatisfactionScore());
        feedback.setFeedbackContent(dto.getFeedbackContent());
        feedback.setConfirmTime(now);
        feedback.setCreateTime(now);
        feedback.setUpdateTime(now);
        repairFeedbackMapper.insert(feedback);

        String opType = unresolved ? "USER_CONFIRM_UNRESOLVED" : "USER_CONFIRM_RESOLVED";
        String defaultRemark = unresolved ? "用户确认未解决，退回维修中" : "用户确认已解决，工单完成";
        String remark = (dto.getRemark() == null || dto.getRemark().trim().isEmpty()) ? defaultRemark : dto.getRemark();
        addFlow(order.getId(), from, order.getStatus(), opType, userId, "user", remark);
        addBusinessLog(order, opType, userId, "user", from, order.getStatus(), remark);
    }

    @Override
    public List<AssignmentRecommendationVO> recommendAssignments(Long id, Long userId, String role) {
        if (!"admin".equals(role)) throw new BusinessException("仅管理员可查看推荐分配人");
        RepairOrder order = requireOrder(id);
        NetworkDevice device = deviceMapper.selectById(order.getDeviceId());
        double priorityScore = calcPriorityScore(order, device);

        List<SysUser> maintainers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "maintainer")
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
        if (maintainers.isEmpty()) return java.util.Collections.emptyList();

        return maintainers.stream().map(m -> {
            Long unfinished = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .notIn(RepairOrder::getStatus, RepairOrderStatusEnum.terminalStatuses()));
            Long processing = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .in(RepairOrder::getStatus, Arrays.asList(
                            RepairOrderStatusEnum.ACCEPTED.getLabel(),
                            RepairOrderStatusEnum.IN_PROGRESS.getLabel(),
                            RepairOrderStatusEnum.PENDING_PARTS.getLabel(),
                            RepairOrderStatusEnum.DELAY_APPLYING.getLabel(),
                            RepairOrderStatusEnum.DELAY_APPROVED.getLabel())));

            List<RepairOrder> finishedOrders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .isNotNull(RepairOrder::getReportTime)
                    .isNotNull(RepairOrder::getFinishTime)
                    .orderByDesc(RepairOrder::getId).last("limit 30"));
            double avgHandleHours = finishedOrders.isEmpty() ? 8D : finishedOrders.stream()
                    .mapToDouble(o -> java.time.Duration.between(o.getReportTime(), o.getFinishTime()).toMinutes() / 60.0)
                    .average().orElse(8D);

            List<RepairOrder> skillOrders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .eq(order.getDeviceType() != null, RepairOrder::getDeviceType, order.getDeviceType())
                    .in(RepairOrder::getStatus, Arrays.asList(RepairOrderStatusEnum.COMPLETED.getLabel(), RepairOrderStatusEnum.CLOSED.getLabel()))
                    .orderByDesc(RepairOrder::getId).last("limit 20"));
            double skillMatch = skillOrders.isEmpty() ? 40D : Math.min(100D, 40D + skillOrders.size() * 8D);

            double loadScore = Math.max(0D, 100D - unfinished * 12D - processing * 10D - Math.max(0D, avgHandleHours - 8D) * 2D);
            double recommendation = priorityScore * 0.35 + loadScore * 0.40 + skillMatch * 0.25;
            String reason = String.format("优先级%.1f；未完成%d、处理中%d；历史平均处理%.1fh；设备类型匹配度%.1f",
                    priorityScore, unfinished, processing, avgHandleHours, skillMatch);
            return new AssignmentRecommendationVO(m.getId(), m.getRealName(), round(recommendation), round(priorityScore),
                    round(loadScore), unfinished, processing, round(avgHandleHours), round(skillMatch), reason);
        }).sorted((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()))
                .collect(Collectors.toList());
    }

    @Override
    public RepairEstimateVO estimateFinishTime(Long id, Long userId, String role) {
        RepairOrder order = requireOrder(id);
        ensureScope(order, userId, role);
        NetworkDevice device = deviceMapper.selectById(order.getDeviceId());
        double base = estimateBaseHours(device == null ? null : device.getDeviceType(), order.getFaultType());
        double priorityFactor = "高".equals(order.getPriority()) ? 0.8 : ("中".equals(order.getPriority()) ? 1.0 : 1.2);
        double partsExtra = order.getNeedPurchaseParts() != null && order.getNeedPurchaseParts() == 1 ? 4.0 : 0.0;
        Double maintainerAvg = null;
        if (order.getAssignMaintainerId() != null) {
            List<RepairOrder> his = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, order.getAssignMaintainerId())
                    .isNotNull(RepairOrder::getReportTime).isNotNull(RepairOrder::getFinishTime)
                    .orderByDesc(RepairOrder::getId).last("limit 30"));
            if (!his.isEmpty()) {
                maintainerAvg = his.stream().mapToDouble(o -> java.time.Duration.between(o.getReportTime(), o.getFinishTime()).toMinutes() / 60.0)
                        .average().orElse(base);
            }
        }
        double estimate = base * priorityFactor + partsExtra;
        if (maintainerAvg != null) estimate = estimate * 0.4 + maintainerAvg * 0.6;
        LocalDateTime anchor = order.getStartRepairTime() != null ? order.getStartRepairTime() : LocalDateTime.now();
        LocalDateTime estimatedFinish = anchor.plusMinutes((long) (estimate * 60));
        order.setExpectedFinishTime(estimatedFinish);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        String basis = String.format("设备类型基准%.1fh；故障类型修正；紧急程度系数%.2f；配件附加%.1fh；维修人员历史均时%s",
                base, priorityFactor, partsExtra, maintainerAvg == null ? "无历史，按基准" : String.format("%.1fh", maintainerAvg));
        return new RepairEstimateVO(estimatedFinish.toString(), round(estimate), basis);
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
        List<RepairOrder> comparable = repairOrderMapper.selectList(scope(role, userId)
                .isNotNull(RepairOrder::getExpectedFinishTime)
                .isNotNull(RepairOrder::getFinishTime));
        map.put("predictionComparableCount", comparable.size());
        if (!comparable.isEmpty()) {
            List<Double> absErrors = comparable.stream()
                    .map(o -> Math.abs(java.time.Duration.between(o.getExpectedFinishTime(), o.getFinishTime()).toMinutes() / 60.0))
                    .collect(Collectors.toList());
            double avg = absErrors.stream().mapToDouble(v -> v).average().orElse(0D);
            long within4 = absErrors.stream().filter(v -> v <= 4).count();
            long within24 = absErrors.stream().filter(v -> v <= 24).count();
            map.put("predictionAvgAbsErrorHours", round(avg));
            map.put("predictionWithin4hCount", within4);
            map.put("predictionWithin24hCount", within24);
        } else {
            map.put("predictionAvgAbsErrorHours", 0D);
            map.put("predictionWithin4hCount", 0);
            map.put("predictionWithin24hCount", 0);
        }
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

    private void assertTransitionAllowed(RepairOrder order, String toStatus, String role) {
        String from = order.getStatus();
        if (from == null || from.equals(toStatus)) return;
        if (RepairOrderStatusEnum.terminalStatuses().contains(from)) {
            throw new BusinessException("终态工单不允许再流转");
        }
        if (RepairOrderStatusEnum.PENDING_ACCEPT.getLabel().equals(toStatus)
                && !RepairOrderStatusEnum.PENDING_ASSIGN.getLabel().equals(from)) {
            throw new BusinessException("未审核并进入待分配，不能分配到待接单");
        }
        if (RepairOrderStatusEnum.ACCEPTED.getLabel().equals(toStatus)
                && !RepairOrderStatusEnum.PENDING_ACCEPT.getLabel().equals(from)) {
            throw new BusinessException("未分配不能接单");
        }
        if (RepairOrderStatusEnum.IN_PROGRESS.getLabel().equals(toStatus)
                && !(RepairOrderStatusEnum.ACCEPTED.getLabel().equals(from)
                || RepairOrderStatusEnum.DELAY_APPLYING.getLabel().equals(from)
                || RepairOrderStatusEnum.DELAY_APPROVED.getLabel().equals(from)
                || "待验收/待确认".equals(from))) {
            throw new BusinessException("未接单不能开始维修");
        }
        if (RepairOrderStatusEnum.PENDING_CONFIRM.getLabel().equals(toStatus)
                && !(RepairOrderStatusEnum.IN_PROGRESS.getLabel().equals(from)
                || RepairOrderStatusEnum.DELAY_APPROVED.getLabel().equals(from))) {
            throw new BusinessException("维修未完成不能进入待验收");
        }
        if (RepairOrderStatusEnum.CLOSED.getLabel().equals(toStatus)
                && !"admin".equals(role)
                && !RepairOrderStatusEnum.COMPLETED.getLabel().equals(from)) {
            throw new BusinessException("仅管理员可关闭，且需满足业务条件");
        }
    }

    private void requireMaintainer(String role) {
        if (!"maintainer".equals(role)) throw new BusinessException("仅维修人员可执行该操作");
    }

    private void ensureMaintainerScope(RepairOrder order, Long userId) {
        if (userId == null || !userId.equals(order.getAssignMaintainerId())) {
            throw new BusinessException("维修人员只能操作分配给自己的工单");
        }
    }

    private double calcPriorityScore(RepairOrder order, NetworkDevice device) {
        double urgent = "高".equals(order.getPriority()) ? 100D : ("中".equals(order.getPriority()) ? 70D : 40D);
        double deviceImportance = 50D;
        if (device != null) {
            int repairCount = device.getTotalRepairRequests() == null ? 0 : device.getTotalRepairRequests();
            if (repairCount >= 10) deviceImportance = 90D;
            else if (repairCount >= 5) deviceImportance = 75D;
            else deviceImportance = 55D;
        }
        double waitHours = order.getReportTime() == null ? 0D :
                Math.max(0D, java.time.Duration.between(order.getReportTime(), LocalDateTime.now()).toMinutes() / 60.0);
        double waitScore = Math.min(100D, waitHours * 2D);
        double impactScore = order.getAffectWideAreaNetwork() != null && order.getAffectWideAreaNetwork() == 1 ? 100D : 50D;
        return round(urgent * 0.35 + deviceImportance * 0.20 + waitScore * 0.20 + impactScore * 0.25);
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private double estimateBaseHours(String deviceType, String faultType) {
        double base = 8D;
        if (deviceType != null) {
            String t = deviceType.toLowerCase();
            if (t.contains("核心") || t.contains("交换机")) base += 3;
            if (t.contains("路由")) base += 2;
            if (t.contains("无线")) base += 1;
        }
        if (faultType != null) {
            String f = faultType.toLowerCase();
            if (f.contains("硬件")) base += 4;
            else if (f.contains("网络")) base += 2;
            else if (f.contains("配置")) base += 1;
        }
        return base;
    }


    private void autoDepositRepairRecord(RepairOrder order, Long maintainerId, RepairOrderStatusDTO dto) {
        if (order.getDeviceId() == null) return;
        Long exists = repairRecordMapper.selectCount(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getRepairOrderId, order.getId()));
        if (exists != null && exists > 0L) return;

        SysUser maintainer = userMapper.selectById(maintainerId);
        RepairRecord record = new RepairRecord();
        record.setRepairOrderId(order.getId());
        record.setRepairOrderNo(order.getOrderNo());
        record.setDeviceId(order.getDeviceId());
        record.setDeviceCode(order.getDeviceCode());
        record.setRepairSequence(calcRepairSequence(order.getDeviceId()));
        record.setMaintenanceSequence(calcMaintenanceSequence(order.getDeviceId()));
        record.setReportTime(order.getReportTime());
        record.setAcceptTime(order.getAcceptTime());
        record.setStartRepairTime(order.getStartRepairTime());
        record.setFinishTime(order.getFinishTime());
        record.setMaintainerId(maintainerId);
        record.setMaintainerEmployeeNo(maintainer == null ? null : maintainer.getEmployeeNo());
        record.setMaintainerName(maintainer == null ? null : maintainer.getRealName());
        record.setFaultReason(order.getFaultType());
        record.setProcessDetail(order.getDescription());
        record.setFixMeasure(dto.getHandleDescription() != null ? dto.getHandleDescription() : order.getHandleDescription());
        record.setResultDetail(dto.getHandleDescription() != null ? dto.getHandleDescription() : order.getHandleDescription());
        record.setUsedParts(order.getNeedPurchaseParts());
        record.setUsedPartsDesc(order.getPartsDescription());
        record.setDelayApplied(order.getApplyDelay());
        record.setDelayReason(order.getDelayReason());
        record.setLaborHours(calcLaborHours(order.getStartRepairTime(), order.getFinishTime()));
        record.setRepairConclusion("维修完成，待用户确认");
        record.setUserConfirmResult(order.getUserConfirmResult());
        record.setUserSatisfaction(order.getSatisfactionScore());
        record.setPhotoUrls(buildRepairPhotos(order.getId(), order.getScenePhotoUrls()));
        record.setRemark(order.getRemark());
        record.setIsResolved(1);
        record.setRepairTime(order.getFinishTime() != null ? order.getFinishTime() : LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        repairRecordMapper.insert(record);
    }

    private String buildRepairPhotos(Long orderId, String scenePhotoUrls) {
        List<String> urls = new java.util.ArrayList<>();
        if (notBlank(scenePhotoUrls)) urls.addAll(parseCsv(scenePhotoUrls));
        List<FileAttachment> atts = fileAttachmentMapper.selectList(new LambdaQueryWrapper<FileAttachment>()
                .and(w -> w.eq(FileAttachment::getBusinessType, "REPAIR_ORDER").or().eq(FileAttachment::getBizType, "REPAIR_ORDER"))
                .and(w -> w.eq(FileAttachment::getBusinessId, orderId).or().eq(FileAttachment::getBizId, orderId))
                .orderByDesc(FileAttachment::getId));
        urls.addAll(atts.stream().map(FileAttachment::getFileUrl).filter(this::notBlank).collect(Collectors.toList()));
        return urls.stream().distinct().collect(Collectors.joining(","));
    }

    private List<String> parseCsv(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(s -> s.replace("\"", ""))
                .filter(this::notBlank)
                .collect(Collectors.toList());
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

    private Integer calcLaborHours(LocalDateTime start, LocalDateTime finish) {
        if (start == null || finish == null || finish.isBefore(start)) return null;
        long hours = java.time.Duration.between(start, finish).toHours();
        return (int) Math.max(1, hours);
    }

    private void recordPredictionError(RepairOrder order, Long userId, String role) {
        if (order.getExpectedFinishTime() == null || order.getFinishTime() == null) return;
        double err = Math.abs(java.time.Duration.between(order.getExpectedFinishTime(), order.getFinishTime()).toMinutes() / 60.0);
        addBusinessLog(order, "PREDICTION_EVAL", userId, role, order.getStatus(), order.getStatus(),
                "预测误差：" + round(err) + "小时");
    }
}
