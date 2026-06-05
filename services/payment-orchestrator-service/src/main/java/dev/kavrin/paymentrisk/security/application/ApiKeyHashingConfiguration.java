package dev.kavrin.paymentrisk.security.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiKeyHashingProperties.class)
public class ApiKeyHashingConfiguration {
}