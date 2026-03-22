
package com.jou.networkrepair.module.v2.rbac.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.rbac.entity.UserRole;
import com.jou.networkrepair.module.v2.rbac.dto.UserRolePageDTO;

public interface UserRoleService {
    PageDataVO<UserRole> page(UserRolePageDTO dto);
    UserRole detail(Long id);
    void create(UserRole entity);
    void update(Long id, UserRole entity);
    void delete(Long id);
}
