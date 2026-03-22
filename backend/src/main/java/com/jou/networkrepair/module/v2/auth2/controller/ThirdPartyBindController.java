
package com.jou.networkrepair.module.v2.auth2.controller;

import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.auth2.dto.ThirdPartyBindPageDTO;
import com.jou.networkrepair.module.v2.auth2.entity.ThirdPartyBind;
import com.jou.networkrepair.module.v2.auth2.service.ThirdPartyBindService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * ThirdPartyBind 管理接口（V2）。
 */
@RestController
@RequestMapping("/api/v2/auth/thirdPartyBind")
@RequiredArgsConstructor
public class ThirdPartyBindController {

    private final ThirdPartyBindService service;

    /** 分页查询（支持关键字与状态筛选）。 */
    @GetMapping("/page")
    public ApiResult<PageDataVO<ThirdPartyBind>> page(@Validated ThirdPartyBindPageDTO dto) {
        return ApiResult.success(service.page(dto));
    }

    /** 主键查询详情。 */
    @GetMapping("/{id}")
    public ApiResult<ThirdPartyBind> detail(@PathVariable Long id) {
        return ApiResult.success(service.detail(id));
    }

    /** 新增记录。 */
    @PostMapping
    public ApiResult<Void> create(@RequestBody ThirdPartyBind entity) {
        service.create(entity);
        return ApiResult.success("新增成功", null);
    }

    /** 更新记录。 */
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody ThirdPartyBind entity) {
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
