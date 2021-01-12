package de.idealo.kafka.idempotency;

import static de.idealo.kafka.idempotency.TestKafkaProducer.HEADER_RECORD_ID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class TestIdempotentKafkaConsumer implements ConsumerSeekAware {

    public static final String TOPIC = "test-topic-idemp";

    private final TestRecordHandler handler;
    private ConsumerSeekCallback consumerSeekCallback;

    public TestIdempotentKafkaConsumer(final TestRecordHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerSeekCallback(ConsumerSeekCallback callback) {
        this.consumerSeekCallback = callback;
    }

    public ConsumerSeekCallback getConsumerSeekCallback() {
        return consumerSeekCallback;
    }

    @KafkaListener(topics = TOPIC)
    @IdempotentListener(ttl = 60)
    public void receive(ConsumerRecord<?, ?> consumerRecord,
            @IdempotencyId @Header(HEADER_RECORD_ID) String recordId,
            @IdempotencyId @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @IdempotencyId @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @IdempotencyId @Header(KafkaHeaders.OFFSET) Long offset
    ) {
        handler.handle(consumerRecord.topic(), consumerRecord);
    }
}
