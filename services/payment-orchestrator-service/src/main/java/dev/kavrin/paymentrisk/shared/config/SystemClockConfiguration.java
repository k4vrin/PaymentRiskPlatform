package dev.kavrin.paymentrisk.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class SystemClockConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
