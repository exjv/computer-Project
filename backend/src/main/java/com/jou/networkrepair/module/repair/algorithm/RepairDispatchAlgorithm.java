package com.jou.networkrepair.module.repair.algorithm;

import com.jou.networkrepair.module.repair.entity.RepairOrder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.PriorityQueue;

/**
 * 自动派单算法
 * 算法目的：对待处理工单进行优先级排序，优先处理影响更大、等待更久的工单。
 * 优先级计算：priority = 设备重要度*0.5 + 故障严重度*0.3 + 等待时长*0.2
 * 分配策略：每次从最大堆取最高优先级工单，分配给当前工作负载最小的维修人员。
 */
@Component
public class RepairDispatchAlgorithm {

    public double calcPriorityScore(RepairOrder order) {
        double deviceLevel = estimateDeviceLevel(order.getTitle());
        double faultSeverity = estimateFaultSeverity(order.getPriority());
        double waitingTime = estimateWaitingHours(order.getReportTime());
        return deviceLevel * 0.5 + faultSeverity * 0.3 + waitingTime * 0.2;
    }

    public PriorityQueue<RepairOrder> buildMaxHeap(Iterable<RepairOrder> orders) {
        PriorityQueue<RepairOrder> maxHeap = new PriorityQueue<>((a, b) -> Double.compare(calcPriorityScore(b), calcPriorityScore(a)));
        for (RepairOrder order : orders) {
            maxHeap.offer(order);
        }
        return maxHeap;
    }

    private double estimateDeviceLevel(String title) {
        if (title == null) return 2;
        if (title.contains("核心") || title.contains("出口") || title.contains("防火墙")) return 5;
        if (title.contains("交换机") || title.contains("路由")) return 4;
        return 3;
    }

    private double estimateFaultSeverity(String priority) {
        if ("高".equals(priority)) return 5;
        if ("中".equals(priority)) return 3;
        return 1;
    }

    private double estimateWaitingHours(LocalDateTime reportTime) {
        if (reportTime == null) return 0;
        long hours = Duration.between(reportTime, LocalDateTime.now()).toHours();
        return Math.min(5, hours / 8.0);
    }
}
