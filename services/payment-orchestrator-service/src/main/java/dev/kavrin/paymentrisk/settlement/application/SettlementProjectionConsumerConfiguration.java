package dev.kavrin.paymentrisk.settlement.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SettlementProjectionConsumerProperties.class)
public class SettlementProjectionConsumerConfiguration {
}