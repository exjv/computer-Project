
package com.jou.networkrepair.common.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class PageQueryDTO {
    @Min(value = 1, message = "页码必须>=1")
    private Long current = 1L;

    @Min(value = 1, message = "每页条数必须>=1")
    @Max(value = 200, message = "每页条数不能超过200")
    private Long size = 10L;

    private String keyword;
    private String status;
    private String sortField;
    private String sortOrder;
}
