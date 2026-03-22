
package com.jou.networkrepair.module.v2.repair2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.repair2.entity.RepairRecord;
import com.jou.networkrepair.module.v2.repair2.dto.RepairRecordPageDTO;

public interface RepairRecordService {
    PageDataVO<RepairRecord> page(RepairRecordPageDTO dto);
    RepairRecord detail(Long id);
    void create(RepairRecord entity);
    void update(Long id, RepairRecord entity);
    void delete(Long id);
}
