package com.jou.networkrepair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.jou.networkrepair",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.jou\\.networkrepair\\.module\\.v2\\..*"
        )
)
public class NetworkRepairApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetworkRepairApplication.class, args);
    }
}
