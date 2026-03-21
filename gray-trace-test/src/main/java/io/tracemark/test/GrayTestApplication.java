package io.tracemark.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GrayTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrayTestApplication.class, args);
    }
}