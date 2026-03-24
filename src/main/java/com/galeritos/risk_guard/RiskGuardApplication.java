package com.galeritos.risk_guard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RiskGuardApplication {

	public static void main(String[] args) {
		SpringApplication.run(RiskGuardApplication.class, args);
	}
}
