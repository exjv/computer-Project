
package com.jou.networkrepair.module.v2.repair2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.PageUtils;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.repair2.dto.RepairFeedbackPageDTO;
import com.jou.networkrepair.module.v2.repair2.entity.RepairFeedback;
import com.jou.networkrepair.module.v2.repair2.mapper.RepairFeedbackMapper;
import com.jou.networkrepair.module.v2.repair2.service.RepairFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RepairFeedbackServiceImpl implements RepairFeedbackService {

    private final RepairFeedbackMapper mapper;

    @Override
    public PageDataVO<RepairFeedback> page(RepairFeedbackPageDTO dto) {
        LambdaQueryWrapper<RepairFeedback> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(RepairFeedback::getId);
        Page<RepairFeedback> page = mapper.selectPage(PageUtils.page(dto), qw);
        return PageUtils.of(page);
    }

    @Override
    public RepairFeedback detail(Long id) {
        RepairFeedback entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("记录不存在");
        return entity;
    }

    @Override
    public void create(RepairFeedback entity) {
        trySetTime(entity, true);
        mapper.insert(entity);
    }

    @Override
    public void update(Long id, RepairFeedback entity) {
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

    private void trySetTime(RepairFeedback entity, boolean create) {
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
