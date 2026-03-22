
package com.jou.networkrepair.module.v2.dictionary.service;

import com.jou.networkrepair.common.vo.PageDataVO;
import com.jou.networkrepair.module.v2.dictionary.entity.Dictionary;
import com.jou.networkrepair.module.v2.dictionary.dto.DictionaryPageDTO;

public interface DictionaryService {
    PageDataVO<Dictionary> page(DictionaryPageDTO dto);
    Dictionary detail(Long id);
    void create(Dictionary entity);
    void update(Long id, Dictionary entity);
    void delete(Long id);
}
