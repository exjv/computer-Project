
package com.jou.networkrepair.module.v2.log2.controller;

import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.log2.dto.BusinessLogPageDTO;
import com.jou.networkrepair.module.v2.log2.entity.BusinessLog;
import com.jou.networkrepair.module.v2.log2.service.BusinessLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * BusinessLog 管理接口（V2）。
 */
@RestController
@RequestMapping("/api/v2/log/businessLog")
@RequiredArgsConstructor
public class BusinessLogController {

    private final BusinessLogService service;

    /** 分页查询（支持关键字与状态筛选）。 */
    @GetMapping("/page")
    public ApiResult<PageDataVO<BusinessLog>> page(@Validated BusinessLogPageDTO dto) {
        return ApiResult.success(service.page(dto));
    }

    /** 主键查询详情。 */
    @GetMapping("/{id}")
    public ApiResult<BusinessLog> detail(@PathVariable Long id) {
        return ApiResult.success(service.detail(id));
    }

    /** 新增记录。 */
    @PostMapping
    public ApiResult<Void> create(@RequestBody BusinessLog entity) {
        service.create(entity);
        return ApiResult.success("新增成功", null);
    }

    /** 更新记录。 */
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody BusinessLog entity) {
        service.update(id, entity);
        return ApiResult.success("修改成功", null);
    }

    /** 删除记录。 */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.success("删除成功", null);
    }
}
