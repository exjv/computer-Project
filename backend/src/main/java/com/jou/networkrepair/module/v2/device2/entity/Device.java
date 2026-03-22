
package com.jou.networkrepair.module.v2.device2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("device")
public class Device {
    private Long id;

    private String deviceNo;
    private String deviceName;
    private Long deviceTypeId;
    private String deviceType;
    private String status;
    private String departmentOrLocation;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

}
