
package com.jou.networkrepair.module.v2.dictionary.dto;

import com.jou.networkrepair.common.dto.PageQueryDTO;
import lombok.Data;

@Data
public class DictionaryPageDTO extends PageQueryDTO {
    private String dictType;
}
