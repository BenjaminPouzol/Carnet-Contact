package com.example.carnet_contact_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling : sans lui, les méthodes @Scheduled (le nettoyage des
// images) seraient ignorées sans le moindre message d'erreur.
@SpringBootApplication
@EnableScheduling
public class CarnetContactBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CarnetContactBackendApplication.class, args);
	}

}
