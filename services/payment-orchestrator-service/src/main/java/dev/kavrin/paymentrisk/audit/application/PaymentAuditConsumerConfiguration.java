package dev.kavrin.paymentrisk.audit.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentAuditConsumerProperties.class)
public class PaymentAuditConsumerConfiguration {
}