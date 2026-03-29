package com.jou.networkrepair.module.notice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.notice.entity.Notice;
import com.jou.networkrepair.module.notice.mapper.NoticeMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {
    private final NoticeMapper noticeMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;

    @GetMapping("/home")
    public ApiResult<Map<String, Object>> home() {
        Map<String, Object> data = buildBaseHome();
        data.put("notices", latestNotices(8));
        return ApiResult.success(data);
    }

    @GetMapping("/workbench")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Map<String, Object>> workbench(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = String.valueOf(request.getAttribute("role"));

        Map<String, Object> data = buildBaseHome();
        data.put("notices", latestNotices(6));
        data.put("quickEntries", quickEntries(role));
        data.put("todo", todoSummary(userId, role));
        data.put("stats", statsSummary(userId, role));
        data.put("role", role);
        return ApiResult.success(data);
    }

    private Map<String, Object> buildBaseHome() {
        Map<String, Object> data = new HashMap<>();
        data.put("systemName", "校园网络设备管理与故障报修系统");
        data.put("systemDesc", "面向校园网络运维场景，提供设备档案管理、故障报修、工单流转、维修协同与服务质量闭环能力。");
        data.put("campusInfo", "所属单位：XX大学网络与信息中心");
        data.put("networkStatus", "校园网运行状态：总体稳定（示例）");
        data.put("scenarios", Arrays.asList(
                "教学楼有线/无线网络故障报修",
                "机房核心设备巡检与维修",
                "网络出口链路异常响应",
                "工单进度跟踪与验收反馈"
        ));

        Map<String, Object> unitMeta = new LinkedHashMap<>();
        unitMeta.put("campus", "XX大学主校区");
        unitMeta.put("servicePhone", "校园网络服务电话：010-12345678");
        unitMeta.put("serviceTime", "服务时间：工作日 08:00-18:00");
        data.put("unitMeta", unitMeta);
        return data;
    }

    private List<Notice> latestNotices(int limit) {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .in(Notice::getStatus, Arrays.asList("ONLINE", "PUBLISHED"))
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId)
                .last("limit " + Math.max(1, Math.min(limit, 20))));
    }

    private List<Map<String, String>> quickEntries(String role) {
        if ("admin".equals(role)) {
            return Arrays.asList(
                    entry("工单审批与分配", "/repair-orders", "repair:order:approve"),
                    entry("设备管理", "/devices", "device:manage"),
                    entry("用户管理", "/users", "user:manage"),
                    entry("日志审计", "/logs", "log:operation:view")
            );
        }
        if ("maintainer".equals(role)) {
            return Arrays.asList(
                    entry("我的待接单", "/repair-orders", "repair:order:view:self"),
                    entry("维修记录", "/repair-records", "repair:record:view"),
                    entry("设备档案", "/devices", "device:view")
            );
        }
        return Arrays.asList(
                entry("发起报修", "/repair-orders", "repair:order:create"),
                entry("我的工单", "/repair-orders", "repair:order:view:self"),
                entry("个人中心", "/profile", "")
        );
    }

    private Map<String, Object> todoSummary(Long userId, String role) {
        Map<String, Object> todo = new LinkedHashMap<>();
        if ("admin".equals(role)) {
            todo.put("pendingApprove", countOrdersByStatus(Arrays.asList("SUBMITTED", "已提交", "已提交/待审核")));
            todo.put("pendingAssign", countOrdersByStatus(Arrays.asList("PENDING_ASSIGN", "待分配")));
            todo.put("processing", countOrdersByStatus(Arrays.asList("IN_PROGRESS", "维修中", "处理中")));
        } else if ("maintainer".equals(role)) {
            todo.put("pendingAccept", countMaintainerOrders(userId, Arrays.asList("PENDING_ACCEPT", "待接单")));
            todo.put("inProgress", countMaintainerOrders(userId, Arrays.asList("ACCEPTED", "维修人员已接单", "IN_PROGRESS", "维修中")));
            todo.put("pendingAcceptance", countMaintainerOrders(userId, Arrays.asList("PENDING_ACCEPTANCE", "待验收", "待验收/待确认")));
        } else {
            todo.put("myOpenOrders", countReporterOrders(userId, Arrays.asList("SUBMITTED", "已提交", "PENDING_ASSIGN", "待分配", "PENDING_ACCEPT", "待接单", "IN_PROGRESS", "维修中", "PENDING_ACCEPTANCE", "待验收", "待验收/待确认")));
            todo.put("myPendingConfirm", countReporterOrders(userId, Arrays.asList("PENDING_ACCEPTANCE", "待验收", "待验收/待确认")));
        }
        return todo;
    }

    private Map<String, Object> statsSummary(Long userId, String role) {
        Map<String, Object> stats = new LinkedHashMap<>();
        if ("admin".equals(role)) {
            stats.put("deviceTotal", deviceMapper.selectCount(null));
            stats.put("deviceFault", deviceMapper.selectCount(new LambdaQueryWrapper<NetworkDevice>().in(NetworkDevice::getStatus, Arrays.asList("故障", "维修中", "FAULT", "REPAIRING"))));
            stats.put("orderTotal", repairOrderMapper.selectCount(null));
            stats.put("orderFinished", countOrdersByStatus(Arrays.asList("COMPLETED", "已完成")));
        } else if ("maintainer".equals(role)) {
            stats.put("myAssigned", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getAssignMaintainerId, userId)));
            stats.put("myFinished", countMaintainerOrders(userId, Arrays.asList("COMPLETED", "已完成")));
        } else {
            stats.put("myReported", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getReporterId, userId)));
            stats.put("myFinished", countReporterOrders(userId, Arrays.asList("COMPLETED", "已完成")));
        }
        return stats;
    }

    private Long countOrdersByStatus(List<String> statuses) {
        return repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().in(RepairOrder::getStatus, statuses));
    }

    private Long countMaintainerOrders(Long userId, List<String> statuses) {
        return repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getAssignMaintainerId, userId)
                .in(RepairOrder::getStatus, statuses));
    }

    private Long countReporterOrders(Long userId, List<String> statuses) {
        return repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getReporterId, userId)
                .in(RepairOrder::getStatus, statuses));
    }

    private Map<String, String> entry(String name, String path, String perm) {
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("path", path);
        map.put("perm", perm);
        return map;
    }
}
