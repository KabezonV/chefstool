package com.chefstool.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChefstoolBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChefstoolBackendApplication.class, args);
	}

}
