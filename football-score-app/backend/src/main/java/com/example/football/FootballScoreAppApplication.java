package com.example.football;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
public class FootballScoreAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(FootballScoreAppApplication.class, args);
    }
}