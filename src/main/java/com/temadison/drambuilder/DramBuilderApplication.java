package com.temadison.drambuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DramBuilderApplication {

    public static void main(String[] args) {
        SpringApplication.run(DramBuilderApplication.class, args);
    }
}
