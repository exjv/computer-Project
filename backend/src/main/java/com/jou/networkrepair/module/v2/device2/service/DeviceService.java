
package com.jou.networkrepair.module.v2.device2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.device2.entity.Device;
import com.jou.networkrepair.module.v2.device2.dto.DevicePageDTO;

public interface DeviceService {
    PageDataVO<Device> page(DevicePageDTO dto);
    Device detail(Long id);
    void create(Device entity);
    void update(Long id, Device entity);
    void delete(Long id);
}
