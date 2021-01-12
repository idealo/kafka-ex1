package de.idealo.kafka.idempotency;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

@Component
public class TestPlainKafkaConsumer implements ConsumerSeekAware {

    public static final String TOPIC = "test-topic-plain";

    private final TestRecordHandler handler;
    private ConsumerSeekCallback consumerSeekCallback;

    public TestPlainKafkaConsumer(final TestRecordHandler handler) {
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
    public void receivePlain(ConsumerRecord<?, ?> consumerRecord) {
        handler.handle(consumerRecord.topic(), consumerRecord);
    }

}
