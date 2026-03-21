package com.jou.networkrepair.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("network_device")
public class NetworkDevice {
    private Long id;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String brandModel;
    private String ipAddress;
    private String macAddress;
    private String location;
    private LocalDate purchaseDate;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
