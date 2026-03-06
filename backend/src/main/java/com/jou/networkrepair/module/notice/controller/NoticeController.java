package com.jou.networkrepair.module.notice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.module.notice.entity.Notice;
import com.jou.networkrepair.module.notice.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeMapper noticeMapper;

    @GetMapping("/page")
    public ApiResult<Page<Notice>> page(@RequestParam Long current, @RequestParam Long size) {
        return ApiResult.success(noticeMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getId)));
    }

    @GetMapping("/{id}")
    public ApiResult<Notice> get(@PathVariable Long id) { return ApiResult.success(noticeMapper.selectById(id)); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> add(@RequestBody Notice notice, HttpServletRequest request) {
        notice.setPublisherId((Long) request.getAttribute("userId")); notice.setCreateTime(LocalDateTime.now()); notice.setUpdateTime(LocalDateTime.now());
        noticeMapper.insert(notice); return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody Notice notice) { notice.setId(id); notice.setUpdateTime(LocalDateTime.now()); noticeMapper.updateById(notice); return ApiResult.success("修改成功", null); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> delete(@PathVariable Long id) { noticeMapper.deleteById(id); return ApiResult.success("删除成功", null); }
}
