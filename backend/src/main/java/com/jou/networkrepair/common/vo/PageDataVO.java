
package com.jou.networkrepair.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageDataVO<T> {
    private Long current;
    private Long size;
    private Long total;
    private List<T> records;
}
