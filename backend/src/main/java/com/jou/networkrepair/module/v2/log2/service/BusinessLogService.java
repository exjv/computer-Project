
package com.jou.networkrepair.module.v2.log2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.log2.entity.BusinessLog;
import com.jou.networkrepair.module.v2.log2.dto.BusinessLogPageDTO;

public interface BusinessLogService {
    PageDataVO<BusinessLog> page(BusinessLogPageDTO dto);
    BusinessLog detail(Long id);
    void create(BusinessLog entity);
    void update(Long id, BusinessLog entity);
    void delete(Long id);
}
