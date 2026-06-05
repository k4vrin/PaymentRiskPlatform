package dev.kavrin.paymentrisk.callback.application;

import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import reactor.core.publisher.Mono;

/**
 * Publishes partner callback commands for asynchronous delivery.
 */
public interface PartnerCallbackCommandPublisher {

    Mono<Void> publish(CallPartnerWebhookCommand command);
}