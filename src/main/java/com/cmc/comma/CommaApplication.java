package com.cmc.comma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CommaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommaApplication.class, args);
    }

}
