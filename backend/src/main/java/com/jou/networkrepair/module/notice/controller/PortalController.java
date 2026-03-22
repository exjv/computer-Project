package com.jou.networkrepair.module.notice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.module.notice.entity.Notice;
import com.jou.networkrepair.module.notice.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {
    private final NoticeMapper noticeMapper;

    @GetMapping("/home")
    public ApiResult<Map<String, Object>> home() {
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getId).last("limit 5"));
        Map<String, Object> data = new HashMap<>();
        data.put("systemName", "校园网络设备管理与故障报修系统");
        data.put("systemDesc", "用于校园网络设备故障报修、维修调度、设备管理与统计分析的一体化业务平台");
        data.put("campusInfo", "所属单位：XX大学网络与信息中心");
        data.put("networkStatus", "校园网运行状态：总体稳定（示例）");
        data.put("scenarios", new String[]{"教学楼有线/无线故障报修", "机房核心设备巡检与维修", "网络出口链路异常响应", "运维工单进度追踪与验收"});
        Map<String, Object> unitMeta = new LinkedHashMap<>();
        unitMeta.put("campus", "XX大学主校区");
        unitMeta.put("servicePhone", "校园网络服务电话：010-12345678");
        unitMeta.put("serviceTime", "服务时间：工作日 08:00-18:00");
        data.put("unitMeta", unitMeta);
        data.put("notices", notices);
        return ApiResult.success(data);
    }
}
