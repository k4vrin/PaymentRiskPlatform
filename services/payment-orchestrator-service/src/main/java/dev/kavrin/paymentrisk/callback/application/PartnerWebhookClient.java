package dev.kavrin.paymentrisk.callback.application;

import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import reactor.core.publisher.Mono;

/**
 * Calls the merchant/partner webhook endpoint.
 */
public interface PartnerWebhookClient {

    Mono<Void> call(CallPartnerWebhookCommand command);
}