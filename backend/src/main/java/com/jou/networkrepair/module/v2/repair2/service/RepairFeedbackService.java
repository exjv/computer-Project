
package com.jou.networkrepair.module.v2.repair2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.repair2.entity.RepairFeedback;
import com.jou.networkrepair.module.v2.repair2.dto.RepairFeedbackPageDTO;

public interface RepairFeedbackService {
    PageDataVO<RepairFeedback> page(RepairFeedbackPageDTO dto);
    RepairFeedback detail(Long id);
    void create(RepairFeedback entity);
    void update(Long id, RepairFeedback entity);
    void delete(Long id);
}
