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

@Component
public class RepairDispatchAlgorithm {

    private static final double A = 0.30D; // 紧急程度
    private static final double B = 0.25D; // 设备重要性
    private static final double C = 0.20D; // 等待时长
    private static final double D = 0.15D; // 影响范围
    private static final double E = 0.10D; // 位置权重

    private static final double X = 0.30D; // 未完成工单数
    private static final double Y = 0.30D; // 处理中任务数
    private static final double Z = 0.25D; // 历史平均处理时长
    private static final double K = 0.15D; // 技能匹配惩罚

    public double calcPriorityScore(RepairOrder order, NetworkDevice device) {
        double priorityWeight = PriorityWeightEnum.from(order.getPriority());
        double deviceWeight = estimateDeviceWeight(device);
        double waitWeight = estimateWaitWeight(order.getReportTime());
        double impactWeight = estimateImpactWeight(order.getDescription());
        String location = null;
        if (device != null) {
            location = device.getBuildingLocation() == null ? device.getLocation() : device.getBuildingLocation();
        }
        double locationWeight = estimateLocationWeight(location);
        return A * priorityWeight + B * deviceWeight + C * waitWeight + D * impactWeight + E * locationWeight;
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

    public double calcMaintainerLoad(long unfinishedCount, long processingCount, double avgHandleHours, boolean skillMatched) {
        double avgHoursScore = Math.min(100D, Math.max(0D, avgHandleHours));
        double skillPenalty = skillMatched ? 0D : 100D;
        return X * unfinishedCount + Y * processingCount + Z * avgHoursScore + K * skillPenalty;
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
        return Math.min(100D, minutes / 8.0D);
    }

    private double estimateLocationWeight(String location) {
        if (location == null) return LocationWeightEnum.OTHER.weight();
        if (location.contains("网络中心")) return LocationWeightEnum.NETWORK_CENTER.weight();
        if (location.contains("数据中心") || location.contains("机房")) return LocationWeightEnum.DATA_CENTER.weight();
        if (location.contains("教学楼") || location.contains("图书馆")) return LocationWeightEnum.TEACHING_BUILDING.weight();
        if (location.contains("宿舍")) return LocationWeightEnum.DORMITORY.weight();
        return LocationWeightEnum.OTHER.weight();
    }

    private double estimateImpactWeight(String description) {
        if (description == null || description.trim().isEmpty()) return 30D;
        String d = description.toLowerCase();
        if (d.contains("全网") || d.contains("大面积") || d.contains("整栋") || d.contains("核心")) return 100D;
        if (d.contains("楼层") || d.contains("教学") || d.contains("机房")) return 80D;
        if (d.contains("宿舍") || d.contains("办公室") || d.contains("多用户")) return 60D;
        return 40D;
    }
}
