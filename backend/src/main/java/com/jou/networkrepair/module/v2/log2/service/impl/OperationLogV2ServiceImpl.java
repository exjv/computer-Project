
package com.jou.networkrepair.module.v2.log2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.PageUtils;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.log2.dto.OperationLogV2PageDTO;
import com.jou.networkrepair.module.v2.log2.entity.OperationLogV2;
import com.jou.networkrepair.module.v2.log2.mapper.OperationLogV2Mapper;
import com.jou.networkrepair.module.v2.log2.service.OperationLogV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogV2ServiceImpl implements OperationLogV2Service {

    private final OperationLogV2Mapper mapper;

    @Override
    public PageDataVO<OperationLogV2> page(OperationLogV2PageDTO dto) {
        LambdaQueryWrapper<OperationLogV2> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(OperationLogV2::getId);
        Page<OperationLogV2> page = mapper.selectPage(PageUtils.page(dto), qw);
        return PageUtils.of(page);
    }

    @Override
    public OperationLogV2 detail(Long id) {
        OperationLogV2 entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("记录不存在");
        return entity;
    }

    @Override
    public void create(OperationLogV2 entity) {
        trySetTime(entity, true);
        mapper.insert(entity);
    }

    @Override
    public void update(Long id, OperationLogV2 entity) {
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

    private void trySetTime(OperationLogV2 entity, boolean create) {
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
