
package com.jou.networkrepair.module.v2.device2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("device_type")
public class DeviceType {
    private Long id;

    private String typeCode;
    private String typeName;
    private Integer status;
    private Integer sortNo;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

}
