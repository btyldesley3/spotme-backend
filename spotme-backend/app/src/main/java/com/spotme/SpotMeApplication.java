package com.spotme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.spotme")
public class SpotMeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotMeApplication.class, args);
	}

}
