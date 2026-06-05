package dev.kavrin.paymentrisk.settlement.infrastructure.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Reactive repository for settlement projection rows.
 */
public interface SettlementProjectionRepository
        extends ReactiveCrudRepository<SettlementProjectionEntity, Long> {

    Mono<SettlementProjectionEntity> findByPaymentId(String paymentId);

    Flux<SettlementProjectionEntity> findByMerchantIdAndStatusAndBusinessDateOrderByUpdatedAtDesc(
            String merchantId,
            String status,
            LocalDate businessDate
    );
}
