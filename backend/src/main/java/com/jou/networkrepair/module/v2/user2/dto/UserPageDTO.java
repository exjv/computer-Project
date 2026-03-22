
package com.jou.networkrepair.module.v2.user2.dto;

import com.jou.networkrepair.common.dto.PageQueryDTO;
import lombok.Data;

@Data
public class UserPageDTO extends PageQueryDTO {
    private String jobNo;
    private String username;
}
