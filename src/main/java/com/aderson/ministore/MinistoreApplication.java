package com.aderson.ministore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MinistoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinistoreApplication.class, args);
    }
}
