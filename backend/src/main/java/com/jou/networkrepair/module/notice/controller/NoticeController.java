package com.jou.networkrepair.module.notice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.module.notice.entity.Notice;
import com.jou.networkrepair.module.notice.enums.NoticeStatusEnum;
import com.jou.networkrepair.module.notice.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeMapper noticeMapper;

    @GetMapping("/page")
    public ApiResult<Page<Notice>> page(@RequestParam Long current, @RequestParam Long size,
                                        @RequestParam(required = false) String title,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false, defaultValue = "publishTime") String sortBy) {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<Notice>()
                .like(title != null && !title.trim().isEmpty(), Notice::getTitle, title)
                .eq(status != null && !status.trim().isEmpty(), Notice::getStatus, status);
        if ("createTime".equalsIgnoreCase(sortBy)) {
            qw.orderByDesc(Notice::getCreateTime).orderByDesc(Notice::getId);
        } else {
            qw.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getId);
        }
        return ApiResult.success(noticeMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/home")
    public ApiResult<List<Notice>> homeList(@RequestParam(required = false, defaultValue = "8") Integer limit) {
        return ApiResult.success(noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, NoticeStatusEnum.ONLINE.name())
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId)
                .last("limit " + Math.max(1, Math.min(limit, 20)))));
    }

    @GetMapping("/{id}")
    public ApiResult<Notice> get(@PathVariable Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) throw new BusinessException("公告不存在");
        return ApiResult.success(notice);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Loggable(module = "公告管理", operationType = "发布", operationDesc = "发布公告")
    public ApiResult<Void> add(@RequestBody Notice notice, HttpServletRequest request) {
        validateNotice(notice);
        NoticeStatusEnum status = NoticeStatusEnum.of(notice.getStatus() == null ? NoticeStatusEnum.DRAFT.name() : notice.getStatus());
        notice.setStatus(status.name());
        notice.setPublishTime(status == NoticeStatusEnum.ONLINE ? LocalDateTime.now() : notice.getPublishTime());
        notice.setPublisherId((Long) request.getAttribute("userId"));
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
        return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Loggable(module = "公告管理", operationType = "编辑", operationDesc = "编辑公告")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody Notice notice) {
        validateNotice(notice);
        Notice exists = noticeMapper.selectById(id);
        if (exists == null) throw new BusinessException("公告不存在");
        NoticeStatusEnum status = NoticeStatusEnum.of(notice.getStatus() == null ? exists.getStatus() : notice.getStatus());
        notice.setId(id);
        notice.setStatus(status.name());
        if (status == NoticeStatusEnum.ONLINE && exists.getPublishTime() == null) notice.setPublishTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
        return ApiResult.success("修改成功", null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Loggable(module = "公告管理", operationType = "上下线", operationDesc = "上下线公告")
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody Notice notice) {
        Notice exists = noticeMapper.selectById(id);
        if (exists == null) throw new BusinessException("公告不存在");
        NoticeStatusEnum status = NoticeStatusEnum.of(notice.getStatus());
        Notice update = new Notice();
        update.setId(id);
        update.setStatus(status.name());
        if (status == NoticeStatusEnum.ONLINE && exists.getPublishTime() == null) update.setPublishTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        noticeMapper.updateById(update);
        return ApiResult.success("状态更新成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Loggable(module = "公告管理", operationType = "删除", operationDesc = "删除公告")
    public ApiResult<Void> delete(@PathVariable Long id) {
        noticeMapper.deleteById(id);
        return ApiResult.success("删除成功", null);
    }

    private void validateNotice(Notice notice) {
        if (notice.getTitle() == null || notice.getTitle().trim().isEmpty()) throw new BusinessException("标题不能为空");
        if (notice.getContent() == null || notice.getContent().trim().isEmpty()) throw new BusinessException("内容不能为空");
    }
}
