package dev.kavrin.paymentrisk.ops.application.metrics;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpsMetricsConsumerProperties.class)
public class OpsMetricsConsumerConfiguration {
}