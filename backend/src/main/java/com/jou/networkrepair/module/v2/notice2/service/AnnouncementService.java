
package com.jou.networkrepair.module.v2.notice2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.notice2.entity.Announcement;
import com.jou.networkrepair.module.v2.notice2.dto.AnnouncementPageDTO;

public interface AnnouncementService {
    PageDataVO<Announcement> page(AnnouncementPageDTO dto);
    Announcement detail(Long id);
    void create(Announcement entity);
    void update(Long id, Announcement entity);
    void delete(Long id);
}
