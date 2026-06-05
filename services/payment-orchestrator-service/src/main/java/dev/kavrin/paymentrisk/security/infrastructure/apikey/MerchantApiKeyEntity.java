package dev.kavrin.paymentrisk.security.infrastructure.apikey;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Stored merchant API key metadata.
 *
 * <p>The raw API key secret must never be stored. Only a hash of the secret is
 * persisted so leaked database rows cannot be used directly as credentials.</p>
 */
@Builder
@Table("merchant_api_keys")
public record MerchantApiKeyEntity(
        @Id Long id,
        @Column("key_id")
        String keyId,
        @Column("secret_hash")
        String secretHash,
        @Column("merchant_id")
        String merchantId,
        @Column("status")
        String status,
        @Column("created_at")
        Instant createdAt,
        @Column("rotated_at")
        Instant rotatedAt
) {
}
