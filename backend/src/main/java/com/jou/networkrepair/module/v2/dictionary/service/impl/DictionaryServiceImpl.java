
package com.jou.networkrepair.module.v2.dictionary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.PageUtils;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.dictionary.dto.DictionaryPageDTO;
import com.jou.networkrepair.module.v2.dictionary.entity.Dictionary;
import com.jou.networkrepair.module.v2.dictionary.mapper.DictionaryMapper;
import com.jou.networkrepair.module.v2.dictionary.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {

    private final DictionaryMapper mapper;

    @Override
    public PageDataVO<Dictionary> page(DictionaryPageDTO dto) {
        LambdaQueryWrapper<Dictionary> qw = new LambdaQueryWrapper<>();

        com.jou.networkrepair.common.utils.PageUtils.eqIfPresent(qw, Dictionary::getDictType, dto.getDictType());
        com.jou.networkrepair.common.utils.PageUtils.likeIfPresent(qw, Dictionary::getDictLabel, dto.getKeyword());
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            qw.eq(Dictionary::getStatus, Integer.valueOf(dto.getStatus()));
        }

        qw.orderByDesc(Dictionary::getId);
        Page<Dictionary> page = mapper.selectPage(PageUtils.page(dto), qw);
        return PageUtils.of(page);
    }

    @Override
    public Dictionary detail(Long id) {
        Dictionary entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("记录不存在");
        return entity;
    }

    @Override
    public void create(Dictionary entity) {
        trySetTime(entity, true);
        mapper.insert(entity);
    }

    @Override
    public void update(Long id, Dictionary entity) {
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

    private void trySetTime(Dictionary entity, boolean create) {
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
