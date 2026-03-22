
package com.jou.networkrepair.module.v2.file2.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.file2.entity.FileAttachment;
import com.jou.networkrepair.module.v2.file2.dto.FileAttachmentPageDTO;

public interface FileAttachmentService {
    PageDataVO<FileAttachment> page(FileAttachmentPageDTO dto);
    FileAttachment detail(Long id);
    void create(FileAttachment entity);
    void update(Long id, FileAttachment entity);
    void delete(Long id);
}
