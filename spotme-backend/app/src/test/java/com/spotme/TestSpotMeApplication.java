package com.spotme;

import org.springframework.boot.SpringApplication;

public class TestSpotMeApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpotMeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
