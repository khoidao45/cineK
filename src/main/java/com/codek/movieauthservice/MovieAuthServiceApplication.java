package com.codek.movieauthservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableJpaAuditing
public class MovieAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieAuthServiceApplication.class, args);
    }

}
