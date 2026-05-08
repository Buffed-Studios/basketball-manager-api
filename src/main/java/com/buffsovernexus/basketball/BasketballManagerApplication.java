package com.buffsovernexus.basketball;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BasketballManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BasketballManagerApplication.class, args);
    }
}

