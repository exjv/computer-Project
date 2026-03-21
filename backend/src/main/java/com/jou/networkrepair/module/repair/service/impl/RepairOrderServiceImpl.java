package com.jou.networkrepair.module.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.algorithm.RepairDispatchAlgorithm;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
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
    private static final Set<String> STATUS_SET = new HashSet<>(Arrays.asList("待处理", "已分配", "处理中", "已完成", "已关闭"));

    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final RepairDispatchAlgorithm repairDispatchAlgorithm;

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
        order.setStatus("待处理");
        order.setReportTime(LocalDateTime.now());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.insert(order);

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
        if ("处理中".equals(order.getStatus()) || "已分配".equals(order.getStatus())) throw new BusinessException("工单处理中，无法删除");
        repairOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, RepairOrderAssignDTO dto) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if (!"待处理".equals(order.getStatus())) throw new BusinessException("仅待处理工单允许分配");
        SysUser maintainer = userMapper.selectById(dto.getAssignMaintainerId());
        if (maintainer == null || !"maintainer".equals(maintainer.getRole()) || maintainer.getStatus() == null || maintainer.getStatus() != 1) {
            throw new BusinessException("维修人员无效或不可用");
        }
        order.setAssignMaintainerId(dto.getAssignMaintainerId());
        order.setAssignTime(LocalDateTime.now());
        order.setStatus("已分配");
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
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
        map.put("pending", repairOrderMapper.selectCount(base.clone().eq(RepairOrder::getStatus, "待处理")));
        map.put("processing", repairOrderMapper.selectCount(base.clone().eq(RepairOrder::getStatus, "处理中")));
        map.put("finished", repairOrderMapper.selectCount(base.clone().eq(RepairOrder::getStatus, "已完成")));
        return map;
    }

    @Override
    public List<DispatchResultVO> autoDispatch() {
        List<RepairOrder> pendingOrders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, "待处理")
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
                    .in(RepairOrder::getStatus, Arrays.asList("已分配", "处理中")));
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
            order.setStatus("已分配");
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);

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
        transitionMap.put("待处理", new HashSet<>(Collections.singletonList("已分配")));
        transitionMap.put("已分配", new HashSet<>(Collections.singletonList("处理中")));
        transitionMap.put("处理中", new HashSet<>(Arrays.asList("已完成", "已关闭")));
        transitionMap.put("已完成", Collections.emptySet());
        transitionMap.put("已关闭", Collections.emptySet());
        Set<String> nextSet = transitionMap.getOrDefault(from, Collections.emptySet());
        if (!nextSet.contains(to)) throw new BusinessException("状态流转不合法：" + from + " -> " + to);
    }
}
