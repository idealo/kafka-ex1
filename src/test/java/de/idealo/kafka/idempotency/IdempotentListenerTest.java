package de.idealo.kafka.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import redis.embedded.RedisServer;

import de.idealo.kafka.idempotency.configuration.RedisProperties;

@SpringBootTest(
        properties = {
            "spring.kafka.consumer.group-id: test-group",
            "spring.kafka.consumer.auto-offset-reset: earliest",
            "spring.kafka.bootstrap-servers: ${spring.embedded.kafka.brokers}",
	        "spring.kafka.listener.poll-timeout: 300ms"
		}
)
@EmbeddedKafka(partitions = 1, topics = {TestPlainKafkaConsumer.TOPIC, TestIdempotentKafkaConsumer.TOPIC})
public class IdempotentListenerTest {

	@Autowired
	private TestRecordHandler handler;

	@Autowired
	private TestPlainKafkaConsumer consumerPlain;

	@Autowired
	private TestIdempotentKafkaConsumer consumerIdempotent;

	@Autowired
	private TestKafkaProducer producer;

	@Autowired
	private RedisProperties redisProperties;

	private RedisServer redisServer;

	@BeforeEach
	void setUp() {
		this.redisServer = new RedisServer(redisProperties.getRedisPort());
		redisServer.start();
	}

	@AfterEach
	void tearDown() {
		redisServer.stop();
	}

	@Test
	void test() throws InterruptedException {
		var payload1 = "message1";
		var payload2 = "message2";

		producer.sendWithHeaderId(TestPlainKafkaConsumer.TOPIC, payload1);
		producer.sendWithHeaderId(TestPlainKafkaConsumer.TOPIC, payload2);
		producer.sendWithHeaderId(TestIdempotentKafkaConsumer.TOPIC, payload1);
		producer.sendWithHeaderId(TestIdempotentKafkaConsumer.TOPIC, payload2);

		Thread.sleep(1000);
		resetOffsets();
		Thread.sleep(1000);
		resetOffsets();
		Thread.sleep(1000);

		assertEquals(3, handler.getNumOfProcessed(TestPlainKafkaConsumer.TOPIC, payload1));
		assertEquals(3, handler.getNumOfProcessed(TestPlainKafkaConsumer.TOPIC, payload2));

		assertEquals(1, handler.getNumOfProcessed(TestIdempotentKafkaConsumer.TOPIC, payload1));
		assertEquals(1, handler.getNumOfProcessed(TestIdempotentKafkaConsumer.TOPIC, payload2));
	}

	private void resetOffsets() {
		consumerPlain.getConsumerSeekCallback().seek(TestPlainKafkaConsumer.TOPIC, 0, 0);
		consumerIdempotent.getConsumerSeekCallback().seek(TestIdempotentKafkaConsumer.TOPIC, 0, 0);
	}

}
