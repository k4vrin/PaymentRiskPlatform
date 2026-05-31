package dev.kavrin.paymentrisk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
				+ "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
				+ "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration,"
				+ "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
})
@ActiveProfiles("test")
@Import(TestPostgresConfiguration.class)
class PaymentOrchestratorServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
