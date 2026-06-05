package dev.kavrin.paymentrisk.callback.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CallbackMessagingProperties.class)
public class CallbackMessagingConfiguration {
}