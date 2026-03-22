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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {
    private final NoticeMapper noticeMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;

    @GetMapping("/home")
    public ApiResult<Map<String, Object>> home() {
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getId).last("limit 5"));
        Map<String, Object> data = new HashMap<>();
        data.put("systemName", "校园网络设备管理与故障报修系统");
        data.put("systemDesc", "用于校园网络设备故障报修、维修调度、设备管理与统计分析的一体化业务平台");
        data.put("campusInfo", "示例单位：XX大学网络与信息中心");
        data.put("scenes", Arrays.asList(
                "校园网络设备台账管理",
                "故障报修与工单协同流转",
                "维修过程留痕与日志审计",
                "统计分析与报表导出"
        ));
        data.put("loginEntries", Arrays.asList("系统/业务管理员", "维修人员", "报修用户"));
        data.put("notices", notices);
        return ApiResult.success(data);
    }

    @GetMapping("/workbench")
    public ApiResult<Map<String, Object>> workbench(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getId).last("limit 5"));
        Map<String, Object> data = new HashMap<>();
        data.put("systemName", "校园网络设备管理与故障报修系统");
        data.put("campusInfo", "XX大学网络与信息中心");
        data.put("notices", notices);
        data.put("quickEntries", quickEntries(role));
        data.put("todo", todoSummary(userId, role));
        data.put("stats", statsSummary(userId, role));
        return ApiResult.success(data);
    }

    private List<Map<String, String>> quickEntries(String role) {
        if ("admin".equals(role)) {
            return Arrays.asList(
                    entry("工单审批", "/repair-orders"),
                    entry("设备管理", "/devices"),
                    entry("用户管理", "/users"),
                    entry("日志审计", "/logs")
            );
        }
        if ("maintainer".equals(role)) {
            return Arrays.asList(
                    entry("我的待接单", "/repair-orders"),
                    entry("维修记录", "/repair-records"),
                    entry("设备档案", "/devices")
            );
        }
        return Arrays.asList(
                entry("发起报修", "/repair-orders"),
                entry("我的工单", "/repair-orders"),
                entry("个人中心", "/profile")
        );
    }

    private Map<String, Object> todoSummary(Long userId, String role) {
        Map<String, Object> todo = new HashMap<>();
        if ("admin".equals(role)) {
            todo.put("pendingApprove", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, "已提交")));
            todo.put("pendingAssign", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, "待分配")));
        } else if ("maintainer".equals(role)) {
            todo.put("pendingAccept", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, userId).eq(RepairOrder::getStatus, "待接单")));
            todo.put("processing", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, userId).in(RepairOrder::getStatus, Arrays.asList("维修人员已接单", "维修中", "待验收"))));
        } else {
            todo.put("myOpenOrders", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getReporterId, userId).in(RepairOrder::getStatus, Arrays.asList("已提交", "待分配", "待接单", "维修中", "待验收"))));
            todo.put("myPendingConfirm", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getReporterId, userId).eq(RepairOrder::getStatus, "待验收")));
        }
        return todo;
    }

    private Map<String, Object> statsSummary(Long userId, String role) {
        Map<String, Object> stats = new HashMap<>();
        if ("admin".equals(role)) {
            stats.put("deviceTotal", deviceMapper.selectCount(null));
            stats.put("deviceFault", deviceMapper.selectCount(new LambdaQueryWrapper<NetworkDevice>().in(NetworkDevice::getStatus, Arrays.asList("故障", "维修中"))));
            stats.put("orderTotal", repairOrderMapper.selectCount(null));
            stats.put("orderFinished", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, "已完成")));
        } else if ("maintainer".equals(role)) {
            stats.put("myAccepted", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, userId).eq(RepairOrder::getStatus, "维修人员已接单")));
            stats.put("myFinished", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getAssignMaintainerId, userId).eq(RepairOrder::getStatus, "已完成")));
        } else {
            stats.put("myReported", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getReporterId, userId)));
            stats.put("myFinished", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getReporterId, userId).eq(RepairOrder::getStatus, "已完成")));
        }
        return stats;
    }

    private Map<String, String> entry(String name, String path) {
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("path", path);
        return map;
    }
}
