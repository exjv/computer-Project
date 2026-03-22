
package com.jou.networkrepair.module.v2.repair2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.repair2.entity.RepairOrder;
import com.jou.networkrepair.module.v2.repair2.dto.RepairOrderPageDTO;

public interface RepairOrderService {
    PageDataVO<RepairOrder> page(RepairOrderPageDTO dto);
    RepairOrder detail(Long id);
    void create(RepairOrder entity);
    void update(Long id, RepairOrder entity);
    void delete(Long id);
}
