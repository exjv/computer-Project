
package com.jou.networkrepair.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.dto.PageQueryDTO;
import com.jou.networkrepair.common.vo.PageDataVO;

public class PageUtils {
    private PageUtils() {}

    public static <T> Page<T> page(PageQueryDTO dto) {
        return new Page<>(dto.getCurrent(), dto.getSize());
    }

    public static <T> PageDataVO<T> of(Page<T> page) {
        return new PageDataVO<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public static <T> void likeIfPresent(LambdaQueryWrapper<T> qw, SFunction<T, ?> field, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like(field, keyword.trim());
        }
    }

    public static <T> void eqIfPresent(LambdaQueryWrapper<T> qw, SFunction<T, ?> field, String value) {
        if (value != null && !value.trim().isEmpty()) {
            qw.eq(field, value.trim());
        }
    }
}
