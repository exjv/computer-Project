package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dictionary")
public class DictionaryData {
    private Long id;
    private String dictType;
    private String dictCode;
    private String dictLabel;
    private String dictValue;
    private Integer sortNo;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
