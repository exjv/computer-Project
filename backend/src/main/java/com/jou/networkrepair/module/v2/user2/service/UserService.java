
package com.jou.networkrepair.module.v2.user2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.user2.entity.User;
import com.jou.networkrepair.module.v2.user2.dto.UserPageDTO;

public interface UserService {
    PageDataVO<User> page(UserPageDTO dto);
    User detail(Long id);
    void create(User entity);
    void update(Long id, User entity);
    void delete(Long id);
}
