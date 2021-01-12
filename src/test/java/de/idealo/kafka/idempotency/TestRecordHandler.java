package de.idealo.kafka.idempotency;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
public class TestRecordHandler {

    private final Map<String, Map<String, Integer>> processingStats = new HashMap<>();

    public TestRecordHandler() {
        this.processingStats.put(TestPlainKafkaConsumer.TOPIC, new HashMap<>());
        this.processingStats.put(TestIdempotentKafkaConsumer.TOPIC, new HashMap<>());
    }

    public void handle(final String topic, final ConsumerRecord record) {
        if (!processingStats.containsKey(topic)) {
            throw new RuntimeException("Processed a message from an unexpected topic " + topic);
        }

        final var counter = processingStats.get(topic);
        var payload = record.value().toString();
        counter.put(payload, (counter.containsKey(payload) ? counter.get(payload) : 0) + 1);
    }

    public int getNumOfProcessed(final String topic, final String payload) {
        final var counter = processingStats.get(topic);
        return counter.containsKey(payload) ? counter.get(payload) : 0;
    }
}
