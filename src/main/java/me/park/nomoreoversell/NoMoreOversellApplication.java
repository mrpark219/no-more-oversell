package me.park.nomoreoversell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class NoMoreOversellApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoMoreOversellApplication.class, args);
    }

}
