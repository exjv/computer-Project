
package com.jou.networkrepair.module.v2.log2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.log2.entity.OperationLogV2;
import com.jou.networkrepair.module.v2.log2.dto.OperationLogV2PageDTO;

public interface OperationLogV2Service {
    PageDataVO<OperationLogV2> page(OperationLogV2PageDTO dto);
    OperationLogV2 detail(Long id);
    void create(OperationLogV2 entity);
    void update(Long id, OperationLogV2 entity);
    void delete(Long id);
}
