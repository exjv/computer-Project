
package com.jou.networkrepair.module.v2.repair2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.repair2.entity.RepairOrderFlow;
import com.jou.networkrepair.module.v2.repair2.dto.RepairOrderFlowPageDTO;

public interface RepairOrderFlowService {
    PageDataVO<RepairOrderFlow> page(RepairOrderFlowPageDTO dto);
    RepairOrderFlow detail(Long id);
    void create(RepairOrderFlow entity);
    void update(Long id, RepairOrderFlow entity);
    void delete(Long id);
}
