package dev.kavrin.paymentrisk;

import org.springframework.boot.SpringApplication;

public class TestPaymentOrchestratorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentOrchestratorServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
