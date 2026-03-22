
package com.jou.networkrepair.module.v2.log2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.PageUtils;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.log2.dto.BusinessLogPageDTO;
import com.jou.networkrepair.module.v2.log2.entity.BusinessLog;
import com.jou.networkrepair.module.v2.log2.mapper.BusinessLogMapper;
import com.jou.networkrepair.module.v2.log2.service.BusinessLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BusinessLogServiceImpl implements BusinessLogService {

    private final BusinessLogMapper mapper;

    @Override
    public PageDataVO<BusinessLog> page(BusinessLogPageDTO dto) {
        LambdaQueryWrapper<BusinessLog> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(BusinessLog::getId);
        Page<BusinessLog> page = mapper.selectPage(PageUtils.page(dto), qw);
        return PageUtils.of(page);
    }

    @Override
    public BusinessLog detail(Long id) {
        BusinessLog entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("记录不存在");
        return entity;
    }

    @Override
    public void create(BusinessLog entity) {
        trySetTime(entity, true);
        mapper.insert(entity);
    }

    @Override
    public void update(Long id, BusinessLog entity) {
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

    private void trySetTime(BusinessLog entity, boolean create) {
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
