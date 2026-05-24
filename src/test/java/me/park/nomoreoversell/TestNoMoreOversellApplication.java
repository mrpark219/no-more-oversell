package me.park.nomoreoversell;

import org.springframework.boot.SpringApplication;

public class TestNoMoreOversellApplication {

    public static void main(String[] args) {
        SpringApplication.from(NoMoreOversellApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
