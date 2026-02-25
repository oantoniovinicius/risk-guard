package com.galeritos.risk_guard;

import org.springframework.boot.SpringApplication;

public class TestRiskGuardApplication {

	public static void main(String[] args) {
		SpringApplication.from(RiskGuardApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
