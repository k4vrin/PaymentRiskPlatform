package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;

public class CallbackRetryRequestedException extends RuntimeException {

    private final CallPartnerWebhookCommand retryCommand;

    public CallbackRetryRequestedException(
            CallPartnerWebhookCommand retryCommand,
            Throwable cause
    ) {
        super("Partner callback retry requested", cause);
        this.retryCommand = retryCommand;
    }

    public CallPartnerWebhookCommand retryCommand() {
        return retryCommand;
    }
}