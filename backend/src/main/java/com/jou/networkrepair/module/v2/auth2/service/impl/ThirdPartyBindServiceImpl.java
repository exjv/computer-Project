
package com.jou.networkrepair.module.v2.auth2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.PageUtils;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.auth2.dto.ThirdPartyBindPageDTO;
import com.jou.networkrepair.module.v2.auth2.entity.ThirdPartyBind;
import com.jou.networkrepair.module.v2.auth2.mapper.ThirdPartyBindMapper;
import com.jou.networkrepair.module.v2.auth2.service.ThirdPartyBindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ThirdPartyBindServiceImpl implements ThirdPartyBindService {

    private final ThirdPartyBindMapper mapper;

    @Override
    public PageDataVO<ThirdPartyBind> page(ThirdPartyBindPageDTO dto) {
        LambdaQueryWrapper<ThirdPartyBind> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(ThirdPartyBind::getId);
        Page<ThirdPartyBind> page = mapper.selectPage(PageUtils.page(dto), qw);
        return PageUtils.of(page);
    }

    @Override
    public ThirdPartyBind detail(Long id) {
        ThirdPartyBind entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("记录不存在");
        return entity;
    }

    @Override
    public void create(ThirdPartyBind entity) {
        trySetTime(entity, true);
        mapper.insert(entity);
    }

    @Override
    public void update(Long id, ThirdPartyBind entity) {
        entity.setId(id);
        trySetTime(entity, false);
        int rows = mapper.updateById(entity);
        if (rows == 0) throw new BusinessException("更新失败，记录不存在");
    }

    @Override
    public void delete(Long id) {
        int rows = mapper.deleteById(id);
        if (rows == 0) throw new BusinessException("删除失败，记录不存在");
    }

    private void trySetTime(ThirdPartyBind entity, boolean create) {
        try {
            java.lang.reflect.Method setUpdate = entity.getClass().getMethod("setUpdateTime", LocalDateTime.class);
            setUpdate.invoke(entity, LocalDateTime.now());
            if (create) {
                java.lang.reflect.Method setCreate = entity.getClass().getMethod("setCreateTime", LocalDateTime.class);
                setCreate.invoke(entity, LocalDateTime.now());
            }
        } catch (Exception ignored) {
        }
    }
}
