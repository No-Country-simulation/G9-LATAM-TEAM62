package com.g9latam.team62.fintech_api;

import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class FintechApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FintechApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(UserService userService) {
		return args -> {
			// Usuario de prueba inicial
			if (userService.findAll().isEmpty()) {
				User testUser = new User();
				testUser.setName("Test User");
				testUser.setEmail("test@example.com");
				testUser.setPassword("password123");
				testUser.setMonthlyIncome(new BigDecimal("5000.00"));
				userService.create(testUser);
				System.out.println("\n==================================================");
				System.out.println(">>> USUARIO DE PRUEBA CREADO");
				System.out.println(">>> Email: test@example.com");
				System.out.println(">>> Password: password123");
				System.out.println("==================================================\n");
			}
		};
	}
}
