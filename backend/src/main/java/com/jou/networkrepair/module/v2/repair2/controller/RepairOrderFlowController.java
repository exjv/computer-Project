
package com.jou.networkrepair.module.v2.repair2.controller;

import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.repair2.dto.RepairOrderFlowPageDTO;
import com.jou.networkrepair.module.v2.repair2.entity.RepairOrderFlow;
import com.jou.networkrepair.module.v2.repair2.service.RepairOrderFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * RepairOrderFlow 管理接口（V2）。
 */
@RestController
@RequestMapping("/api/v2/repair/repairOrderFlow")
@RequiredArgsConstructor
public class RepairOrderFlowController {

    private final RepairOrderFlowService service;

    /** 分页查询（支持关键字与状态筛选）。 */
    @GetMapping("/page")
    public ApiResult<PageDataVO<RepairOrderFlow>> page(@Validated RepairOrderFlowPageDTO dto) {
        return ApiResult.success(service.page(dto));
    }

    /** 主键查询详情。 */
    @GetMapping("/{id}")
    public ApiResult<RepairOrderFlow> detail(@PathVariable Long id) {
        return ApiResult.success(service.detail(id));
    }

    /** 新增记录。 */
    @PostMapping
    public ApiResult<Void> create(@RequestBody RepairOrderFlow entity) {
        service.create(entity);
        return ApiResult.success("新增成功", null);
    }

    /** 更新记录。 */
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RepairOrderFlow entity) {
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
