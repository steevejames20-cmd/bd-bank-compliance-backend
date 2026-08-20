package com.bridge.bdbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BdBankComplianceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BdBankComplianceApplication.class, args);
    }
}
