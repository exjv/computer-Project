
package com.jou.networkrepair.module.v2.rbac.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.rbac.entity.Role;
import com.jou.networkrepair.module.v2.rbac.dto.RolePageDTO;

public interface RoleService {
    PageDataVO<Role> page(RolePageDTO dto);
    Role detail(Long id);
    void create(Role entity);
    void update(Long id, Role entity);
    void delete(Long id);
}
