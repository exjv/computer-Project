package com.jou.networkrepair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.jou.networkrepair",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.jou\\.networkrepair\\.module\\.v2\\..*"
        )
)
@MapperScan(basePackages = {
        "com.jou.networkrepair.module.device.mapper",
        "com.jou.networkrepair.module.log.mapper",
        "com.jou.networkrepair.module.notice.mapper",
        "com.jou.networkrepair.module.repair.mapper",
        "com.jou.networkrepair.module.system.mapper",
        "com.jou.networkrepair.module.user.mapper"
})
public class NetworkRepairApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetworkRepairApplication.class, args);
    }
}
