package com.vitorhugo.joblock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedJobLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedJobLockApplication.class, args);
    }
}
