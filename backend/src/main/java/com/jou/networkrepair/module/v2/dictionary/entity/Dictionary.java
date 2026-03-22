
package com.jou.networkrepair.module.v2.dictionary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dictionary")
public class Dictionary {
    private Long id;

    private String dictType;
    private String dictCode;
    private String dictLabel;
    private Integer sortNo;
    private Integer status;
    private String remark;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

}
