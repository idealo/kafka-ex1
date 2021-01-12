package de.idealo.kafka.idempotency;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestKafkaProducer {

    public static final String HEADER_RECORD_ID = "record-id";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Sends a record with an header HEADER_RECORD_ID containing a unique identifier generated from the payload.
     * @param topic
     * @param payload
     */
    public void sendWithHeaderId(String topic, String payload) {
        var record = new ProducerRecord(topic, payload);
        record.headers().add(HEADER_RECORD_ID, (topic + payload).getBytes());
        kafkaTemplate.send(record);
    }
}
