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
        // The Java orchestrator is a gRPC client: it opens a channel to the Go
        // risk-scoring service host/port and sends protobuf ScorePayment calls.
        return ManagedChannelBuilder
                .forAddress(properties.host(), properties.port())
                .usePlaintext()
                .build();
    }
}
