
package com.jou.networkrepair.module.v2.repair2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.PageUtils;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.repair2.dto.RepairOrderFlowPageDTO;
import com.jou.networkrepair.module.v2.repair2.entity.RepairOrderFlow;
import com.jou.networkrepair.module.v2.repair2.mapper.RepairOrderFlowMapper;
import com.jou.networkrepair.module.v2.repair2.service.RepairOrderFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RepairOrderFlowServiceImpl implements RepairOrderFlowService {

    private final RepairOrderFlowMapper mapper;

    @Override
    public PageDataVO<RepairOrderFlow> page(RepairOrderFlowPageDTO dto) {
        LambdaQueryWrapper<RepairOrderFlow> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(RepairOrderFlow::getId);
        Page<RepairOrderFlow> page = mapper.selectPage(PageUtils.page(dto), qw);
        return PageUtils.of(page);
    }

    @Override
    public RepairOrderFlow detail(Long id) {
        RepairOrderFlow entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("记录不存在");
        return entity;
    }

    @Override
    public void create(RepairOrderFlow entity) {
        trySetTime(entity, true);
        mapper.insert(entity);
    }

    @Override
    public void update(Long id, RepairOrderFlow entity) {
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

    private void trySetTime(RepairOrderFlow entity, boolean create) {
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
