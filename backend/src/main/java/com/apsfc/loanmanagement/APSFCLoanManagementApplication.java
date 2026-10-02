package com.apsfc.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class APSFCLoanManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(APSFCLoanManagementApplication.class, args);
    }
}