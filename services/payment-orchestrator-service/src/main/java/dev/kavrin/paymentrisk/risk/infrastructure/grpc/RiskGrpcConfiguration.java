package dev.kavrin.paymentrisk.risk.infrastructure.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RiskGrpcProperties.class)
class RiskGrpcConfiguration {

    @Bean(destroyMethod = "shutdown")
    ManagedChannel riskManagedChannel(RiskGrpcProperties properties) {
        return ManagedChannelBuilder
                .forAddress(properties.host(), properties.port())
                .usePlaintext()
                .build();
    }
}