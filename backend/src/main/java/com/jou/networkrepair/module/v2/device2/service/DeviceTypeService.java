
package com.jou.networkrepair.module.v2.device2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.device2.entity.DeviceType;
import com.jou.networkrepair.module.v2.device2.dto.DeviceTypePageDTO;

public interface DeviceTypeService {
    PageDataVO<DeviceType> page(DeviceTypePageDTO dto);
    DeviceType detail(Long id);
    void create(DeviceType entity);
    void update(Long id, DeviceType entity);
    void delete(Long id);
}
