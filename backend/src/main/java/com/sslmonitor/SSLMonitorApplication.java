package com.sslmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SSLMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SSLMonitorApplication.class, args);
    }
} 