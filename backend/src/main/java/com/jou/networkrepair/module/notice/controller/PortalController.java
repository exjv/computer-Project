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
        data.put("campusInfo", "示例单位：XX大学网络与信息中心");
        data.put("notices", notices);
        return ApiResult.success(data);
    }
}
