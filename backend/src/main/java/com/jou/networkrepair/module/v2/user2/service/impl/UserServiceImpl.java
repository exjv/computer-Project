
package com.jou.networkrepair.module.v2.user2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.PageUtils;
import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.user2.dto.UserPageDTO;
import com.jou.networkrepair.module.v2.user2.entity.User;
import com.jou.networkrepair.module.v2.user2.mapper.UserMapper;
import com.jou.networkrepair.module.v2.user2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper mapper;

    @Override
    public PageDataVO<User> page(UserPageDTO dto) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();

        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            qw.eq(User::getStatus, Integer.valueOf(dto.getStatus()));
        }
        com.jou.networkrepair.common.utils.PageUtils.likeIfPresent(qw, User::getJobNo, dto.getJobNo());
        com.jou.networkrepair.common.utils.PageUtils.likeIfPresent(qw, User::getUsername, dto.getUsername());
        com.jou.networkrepair.common.utils.PageUtils.likeIfPresent(qw, User::getRealName, dto.getKeyword());

        qw.orderByDesc(User::getId);
        Page<User> page = mapper.selectPage(PageUtils.page(dto), qw);
        return PageUtils.of(page);
    }

    @Override
    public User detail(Long id) {
        User entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("记录不存在");
        return entity;
    }

    @Override
    public void create(User entity) {
        trySetTime(entity, true);
        mapper.insert(entity);
    }

    @Override
    public void update(Long id, User entity) {
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

    private void trySetTime(User entity, boolean create) {
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
