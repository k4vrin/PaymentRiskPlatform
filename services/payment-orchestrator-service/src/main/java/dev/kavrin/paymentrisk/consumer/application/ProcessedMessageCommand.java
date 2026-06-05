package dev.kavrin.paymentrisk.consumer.application;

public record ProcessedMessageCommand(
        String consumerName,
        String topic,
        int partition,
        long offset,
        String eventId
) {

    public ProcessedMessageCommand {
        consumerName = requireText(consumerName, "consumerName");
        topic = requireText(topic, "topic");
        eventId = requireText(eventId, "eventId");
        if (partition < 0) {
            throw new IllegalArgumentException("partition must not be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }
}
