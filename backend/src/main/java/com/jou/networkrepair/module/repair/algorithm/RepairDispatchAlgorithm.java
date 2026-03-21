package com.jou.networkrepair.module.repair.algorithm;

import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.repair.algorithm.enums.DeviceLevelEnum;
import com.jou.networkrepair.module.repair.algorithm.enums.LocationWeightEnum;
import com.jou.networkrepair.module.repair.algorithm.enums.PriorityWeightEnum;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * 基于优先级-负载均衡的校园网络故障工单智能调度算法。
 * 1) 对待分配工单计算紧急度评分，采用最大堆保证每次优先取最高分工单。
 * 2) 对维修人员使用综合负载值，选择当前最空闲人员。
 *
 * 时间复杂度：构建堆 O(n log n)，每次分配选最小负载 O(m)，总计约 O(n log n + n*m)。
 * 空间复杂度：堆与映射 O(n+m)。
 */
@Component
public class RepairDispatchAlgorithm {

    /** 系数可作为论文中的可调参数。 */
    private static final double A = 0.35D; // priority
    private static final double B = 0.30D; // device level
    private static final double C = 0.20D; // waiting time
    private static final double D = 0.15D; // location level

    private static final double X = 0.40D; // 未完成工单数权重
    private static final double Y = 0.60D; // 正在处理中工单数权重

    /**
     * Score(order) = a*PriorityWeight + b*DeviceWeight + c*WaitWeight + d*LocationWeight
     */
    public double calcPriorityScore(RepairOrder order, NetworkDevice device) {
        double priorityWeight = PriorityWeightEnum.from(order.getPriority());
        double deviceWeight = estimateDeviceWeight(device);
        double waitWeight = estimateWaitWeight(order.getReportTime());
        double locationWeight = estimateLocationWeight(device == null ? null : device.getLocation());
        return A * priorityWeight + B * deviceWeight + C * waitWeight + D * locationWeight;
    }

    public PriorityQueue<RepairOrder> buildMaxHeap(Iterable<RepairOrder> orders, Map<Long, NetworkDevice> deviceMap) {
        PriorityQueue<RepairOrder> maxHeap = new PriorityQueue<>((o1, o2) -> Double.compare(
                calcPriorityScore(o2, deviceMap.get(o2.getDeviceId())),
                calcPriorityScore(o1, deviceMap.get(o1.getDeviceId()))));
        for (RepairOrder order : orders) {
            maxHeap.offer(order);
        }
        return maxHeap;
    }

    /** 综合负载值：Load(user)=x*未完成+y*处理中 */
    public double calcMaintainerLoad(long unfinishedCount, long processingCount) {
        return X * unfinishedCount + Y * processingCount;
    }

    private double estimateDeviceWeight(NetworkDevice device) {
        if (device == null) return DeviceLevelEnum.TERMINAL.weight();
        String type = device.getDeviceType() == null ? "" : device.getDeviceType();
        if (type.contains("防火墙") || type.contains("核心") || type.contains("服务器")) return DeviceLevelEnum.CORE.weight();
        if (type.contains("路由器") || type.contains("交换机")) return DeviceLevelEnum.AGGREGATION.weight();
        if (type.contains("无线AP")) return DeviceLevelEnum.ACCESS.weight();
        return DeviceLevelEnum.TERMINAL.weight();
    }

    private double estimateWaitWeight(LocalDateTime reportTime) {
        if (reportTime == null) return 0D;
        long minutes = Math.max(0, Duration.between(reportTime, LocalDateTime.now()).toMinutes());
        return Math.min(100D, minutes / 10.0D);
    }

    private double estimateLocationWeight(String location) {
        if (location == null) return LocationWeightEnum.OTHER.weight();
        if (location.contains("网络中心")) return LocationWeightEnum.NETWORK_CENTER.weight();
        if (location.contains("数据中心") || location.contains("机房")) return LocationWeightEnum.DATA_CENTER.weight();
        if (location.contains("教学楼") || location.contains("图书馆")) return LocationWeightEnum.TEACHING_BUILDING.weight();
        if (location.contains("宿舍")) return LocationWeightEnum.DORMITORY.weight();
        return LocationWeightEnum.OTHER.weight();
    }
}
