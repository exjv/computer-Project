package com.jou.networkrepair.module.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.algorithm.RepairDispatchAlgorithm;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderActionDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.mapper.RepairOrderFlowMapper;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.service.RepairOrderService;
import com.jou.networkrepair.module.repair.vo.DispatchResultVO;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RepairOrderServiceImpl implements RepairOrderService {
    private static final Set<String> PRIORITY_SET = new HashSet<>(Arrays.asList("低", "中", "高"));
    private static final Set<String> STATUS_SET = new HashSet<>(Arrays.asList(
            "已提交", "审核通过", "审核驳回", "待分配", "待接单", "维修人员已接单", "维修中", "待验收", "已完成", "已关闭", "已取消"));

    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final RepairDispatchAlgorithm repairDispatchAlgorithm;
    private final RepairOrderFlowMapper repairOrderFlowMapper;

    @Override
    public Page<RepairOrder> page(Long current, Long size, String status, String title, String orderNo, String priority) {
        return repairOrderMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<RepairOrder>()
                .eq(status != null && !status.isEmpty(), RepairOrder::getStatus, status)
                .like(title != null && !title.isEmpty(), RepairOrder::getTitle, title)
                .like(orderNo != null && !orderNo.isEmpty(), RepairOrder::getOrderNo, orderNo)
                .eq(priority != null && !priority.isEmpty(), RepairOrder::getPriority, priority)
                .orderByDesc(RepairOrder::getId));
    }

    @Override
    public Page<RepairOrder> myPage(Long current, Long size, String status, String orderNo, String priority, Long userId, String role) {
        LambdaQueryWrapper<RepairOrder> qw = new LambdaQueryWrapper<>();
        if ("user".equals(role)) qw.eq(RepairOrder::getReporterId, userId);
        if ("maintainer".equals(role)) qw.eq(RepairOrder::getAssignMaintainerId, userId);
        qw.eq(status != null && !status.isEmpty(), RepairOrder::getStatus, status)
                .like(orderNo != null && !orderNo.isEmpty(), RepairOrder::getOrderNo, orderNo)
                .eq(priority != null && !priority.isEmpty(), RepairOrder::getPriority, priority)
                .orderByDesc(RepairOrder::getId);
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

        RepairOrder order = new RepairOrder();
        order.setDeviceId(dto.getDeviceId());
        order.setTitle(dto.getTitle());
        order.setDescription(dto.getDescription());
        order.setPriority(dto.getPriority());
        order.setReporterId(userId);
        order.setOrderNo(generateOrderNo());
        order.setStatus("已提交");
        order.setProgress(5);
        order.setReportTime(LocalDateTime.now());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.insert(order);
        addFlow(order.getId(), null, "已提交", "SUBMIT", userId, "user", "用户提交报修工单");

        NetworkDevice device = new NetworkDevice();
        device.setId(order.getDeviceId());
        device.setStatus("故障");
        deviceMapper.updateById(device);
    }

    @Override
    public void update(Long id, RepairOrder req) {
        req.setId(id);
        req.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(req);
    }

    @Override
    public void delete(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if ("处理中".equals(order.getStatus()) || "待接单".equals(order.getStatus()) || "维修人员已接单".equals(order.getStatus())) throw new BusinessException("工单处理中，无法删除");
        repairOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, RepairOrderAssignDTO dto) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if (!"待分配".equals(order.getStatus())) throw new BusinessException("仅待分配工单允许分配");
        SysUser maintainer = userMapper.selectById(dto.getAssignMaintainerId());
        if (maintainer == null || !"maintainer".equals(maintainer.getRole()) || maintainer.getStatus() == null || maintainer.getStatus() != 1) {
            throw new BusinessException("维修人员无效或不可用");
        }
        String fromStatus = order.getStatus();
        order.setAssignMaintainerId(dto.getAssignMaintainerId());
        order.setAssignTime(LocalDateTime.now());
        order.setStatus("待接单");
        order.setProgress(35);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        addFlow(order.getId(), fromStatus, "待接单", "ADMIN_ASSIGN", null, "admin", "管理员分配维修人员");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void action(Long id, RepairOrderActionDTO dto, Long userId, String role) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        String action = dto.getAction();
        if ("ADMIN_APPROVE".equals(action)) {
            requireRole(role, "admin");
            checkStatus(order.getStatus(), "已提交");
            moveStatus(order, "审核通过", 20, userId, role, dto.getRemark(), action);
            order.setAuditBy(userId);
            order.setAuditTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            moveStatus(order, "待分配", 30, userId, role, "审核通过进入待分配", "ADMIN_TO_ASSIGN");
        } else if ("ADMIN_REJECT".equals(action)) {
            requireRole(role, "admin");
            checkStatus(order.getStatus(), "已提交");
            moveStatus(order, "审核驳回", 0, userId, role, dto.getRemark(), action);
        } else if ("USER_CANCEL".equals(action)) {
            requireRole(role, "user");
            if (!userId.equals(order.getReporterId())) throw new BusinessException("只能撤销自己的工单");
            if (!Arrays.asList("已提交", "审核驳回").contains(order.getStatus())) throw new BusinessException("当前状态不允许撤销");
            moveStatus(order, "已取消", 0, userId, role, dto.getRemark(), action);
        } else if ("MAINTAINER_ACCEPT".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), "待接单");
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
            checkStatus(order.getStatus(), "维修人员已接单");
            order.setStartRepairTime(LocalDateTime.now());
            moveStatus(order, "维修中", 60, userId, role, dto.getRemark(), action);
        } else if ("MAINTAINER_DELAY_APPLY".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), "维修中");
            addFlow(order.getId(), order.getStatus(), order.getStatus(), action, userId, role,
                    dto.getRemark() == null ? "申请延期" : dto.getRemark());
        } else if ("MAINTAINER_PARTS_APPLY".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), "维修中");
            addFlow(order.getId(), order.getStatus(), order.getStatus(), action, userId, role,
                    dto.getRemark() == null ? "申请配件" : dto.getRemark());
        } else if ("MAINTAINER_PROGRESS".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), "维修中");
            if (dto.getProgress() == null) throw new BusinessException("请传入进度");
            order.setProgress(dto.getProgress());
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), "维修中", "维修中", action, userId, role, "进度更新至" + dto.getProgress() + "%");
        } else if ("MAINTAINER_FINISH".equals(action)) {
            requireRole(role, "maintainer");
            checkMaintainerScope(order, userId);
            checkStatus(order.getStatus(), "维修中");
            moveStatus(order, "待验收", 90, userId, role, dto.getRemark(), action);
            order.setFinishTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
        } else if ("USER_CONFIRM_RESOLVED".equals(action)) {
            requireRole(role, "user");
            if (!userId.equals(order.getReporterId())) throw new BusinessException("只能确认自己的工单");
            checkStatus(order.getStatus(), "待验收");
            order.setConfirmTime(LocalDateTime.now());
            moveStatus(order, "已完成", 100, userId, role, dto.getRemark(), action);
        } else if ("USER_CONFIRM_UNRESOLVED".equals(action)) {
            requireRole(role, "user");
            if (!userId.equals(order.getReporterId())) throw new BusinessException("只能确认自己的工单");
            checkStatus(order.getStatus(), "待验收");
            moveStatus(order, "维修中", 60, userId, role, dto.getRemark(), action);
        } else if ("ADMIN_REASSIGN".equals(action)) {
            requireRole(role, "admin");
            if (dto.getAssignMaintainerId() == null) throw new BusinessException("改派时必须指定新的维修人员");
            SysUser maintainer = userMapper.selectById(dto.getAssignMaintainerId());
            if (maintainer == null || !"maintainer".equals(maintainer.getRole()) || maintainer.getStatus() == null || maintainer.getStatus() != 1) {
                throw new BusinessException("维修人员无效或不可用");
            }
            order.setAssignMaintainerId(dto.getAssignMaintainerId());
            order.setAssignTime(LocalDateTime.now());
            order.setStatus("待接单");
            order.setProgress(35);
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), "待分配", "待接单", action, userId, role, dto.getRemark() == null ? "管理员改派" : dto.getRemark());
        } else if ("ADMIN_DELAY_APPROVE".equals(action)) {
            requireRole(role, "admin");
            checkStatus(order.getStatus(), "维修中");
            addFlow(order.getId(), order.getStatus(), order.getStatus(), action, userId, role,
                    dto.getRemark() == null ? "管理员审批延期" : dto.getRemark());
        } else if ("ADMIN_CLOSE".equals(action)) {
            requireRole(role, "admin");
            if (Arrays.asList("已完成", "已关闭", "已取消").contains(order.getStatus())) throw new BusinessException("终态工单不允许重复关闭");
            moveStatus(order, "已关闭", 100, userId, role, dto.getRemark(), action);
        } else {
            throw new BusinessException("不支持的操作");
        }
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
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, RepairOrderStatusDTO dto, Long userId, String role) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if ("maintainer".equals(role) && !userId.equals(order.getAssignMaintainerId())) throw new BusinessException("仅可处理分配给自己的工单");
        if (!STATUS_SET.contains(dto.getStatus())) throw new BusinessException("无效工单状态");
        validateStatusTransition(order.getStatus(), dto.getStatus());
        order.setStatus(dto.getStatus());
        if ("已完成".equals(dto.getStatus())) order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);

        if ("已完成".equals(dto.getStatus()) || "已关闭".equals(dto.getStatus())) {
            NetworkDevice device = new NetworkDevice();
            device.setId(order.getDeviceId());
            device.setStatus("正常");
            device.setUpdateTime(LocalDateTime.now());
            deviceMapper.updateById(device);
        }
    }

    @Override
    public Map<String, Object> stats(Long userId, String role) {
        LambdaQueryWrapper<RepairOrder> base = new LambdaQueryWrapper<>();
        if ("user".equals(role)) base.eq(RepairOrder::getReporterId, userId);
        if ("maintainer".equals(role)) base.eq(RepairOrder::getAssignMaintainerId, userId);
        Map<String, Object> map = new HashMap<>();
        map.put("total", repairOrderMapper.selectCount(base));
        map.put("pending", repairOrderMapper.selectCount(base.clone().in(RepairOrder::getStatus, Arrays.asList("已提交", "待分配", "待接单"))));
        map.put("processing", repairOrderMapper.selectCount(base.clone().in(RepairOrder::getStatus, Arrays.asList("维修人员已接单", "处理中", "待验收"))));
        map.put("finished", repairOrderMapper.selectCount(base.clone().eq(RepairOrder::getStatus, "已完成")));
        return map;
    }

    @Override
    public List<DispatchResultVO> autoDispatch() {
        List<RepairOrder> pendingOrders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, "待分配")
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
                    .in(RepairOrder::getStatus, Arrays.asList("待接单", "维修人员已接单", "处理中", "待验收")));
            Long processing = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, m.getId())
                    .eq(RepairOrder::getStatus, "处理中"));
            unfinishedCountMap.put(m.getId(), unfinished == null ? 0L : unfinished);
            processingCountMap.put(m.getId(), processing == null ? 0L : processing);
            maintainerNameMap.put(m.getId(), m.getRealName());
        }

        PriorityQueue<RepairOrder> maxHeap = repairDispatchAlgorithm.buildMaxHeap(pendingOrders, deviceMap);
        List<DispatchResultVO> result = new ArrayList<>();

        while (!maxHeap.isEmpty()) {
            RepairOrder order = maxHeap.poll();
            Long targetMaintainerId = unfinishedCountMap.keySet().stream()
                    .min(Comparator.comparingDouble(id -> repairDispatchAlgorithm.calcMaintainerLoad(
                            unfinishedCountMap.get(id), processingCountMap.get(id))))
                    .orElseThrow(() -> new BusinessException("未找到可分配维修人员"));

            order.setAssignMaintainerId(targetMaintainerId);
            order.setAssignTime(LocalDateTime.now());
            order.setStatus("待接单");
            order.setProgress(35);
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);
            addFlow(order.getId(), "待分配", "待接单", "AUTO_ASSIGN", null, "system", "系统自动派单");

            unfinishedCountMap.put(targetMaintainerId, unfinishedCountMap.get(targetMaintainerId) + 1L);
            Double score = repairDispatchAlgorithm.calcPriorityScore(order, deviceMap.get(order.getDeviceId()));
            result.add(new DispatchResultVO(order.getOrderNo(), targetMaintainerId,
                    maintainerNameMap.get(targetMaintainerId), score, "按照紧急度评分与负载均衡策略自动分配"));
        }
        return result;
    }

    private String generateOrderNo() {
        String prefix = "RO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String orderNo = prefix + ThreadLocalRandom.current().nextInt(100, 999);
        Long count = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getOrderNo, orderNo));
        if (count != null && count > 0) {
            return prefix + ThreadLocalRandom.current().nextInt(1000, 9999);
        }
        return orderNo;
    }

    private void validateStatusTransition(String from, String to) {
        if (Objects.equals(from, to)) return;
        Map<String, Set<String>> transitionMap = new HashMap<>();
        transitionMap.put("已提交", new HashSet<>(Arrays.asList("审核通过", "审核驳回", "已取消")));
        transitionMap.put("审核通过", new HashSet<>(Collections.singletonList("待分配")));
        transitionMap.put("待分配", new HashSet<>(Collections.singletonList("待接单")));
        transitionMap.put("待接单", new HashSet<>(Collections.singletonList("维修人员已接单")));
        transitionMap.put("维修人员已接单", new HashSet<>(Collections.singletonList("维修中")));
        transitionMap.put("维修中", new HashSet<>(Arrays.asList("待验收", "已关闭")));
        transitionMap.put("待验收", new HashSet<>(Arrays.asList("已完成", "维修中")));
        transitionMap.put("已完成", Collections.emptySet());
        transitionMap.put("审核驳回", new HashSet<>(Collections.singletonList("已取消")));
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
    }

    private void addFlow(Long orderId, String fromStatus, String toStatus, String action, Long userId, String role, String remark) {
        RepairOrderFlow flow = new RepairOrderFlow();
        flow.setRepairOrderId(orderId);
        flow.setFromStatus(fromStatus);
        flow.setToStatus(toStatus);
        flow.setAction(action);
        flow.setOperatorId(userId);
        flow.setOperatorRole(role);
        flow.setRemark(remark);
        flow.setCreateTime(LocalDateTime.now());
        repairOrderFlowMapper.insert(flow);
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
}
