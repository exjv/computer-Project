
package com.jou.networkrepair.module.v2.auth2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.auth2.entity.ThirdPartyBind;
import com.jou.networkrepair.module.v2.auth2.dto.ThirdPartyBindPageDTO;

public interface ThirdPartyBindService {
    PageDataVO<ThirdPartyBind> page(ThirdPartyBindPageDTO dto);
    ThirdPartyBind detail(Long id);
    void create(ThirdPartyBind entity);
    void update(Long id, ThirdPartyBind entity);
    void delete(Long id);
}
