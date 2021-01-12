# Kafka EX1

**Kafka EX1** is an autoconfigurable utility to equip your Spring Kafka listeners with the idempotency,
maintaining the "exactly-once" (the name of the artifact is a play of these words) semantics.

A typical use case for this is when a Kafka consumer executes one or a set of actions (e.g. sending an email) that should not be performed twice.
But if you discover that your live system has a bug, which in some cases leads to non-retryable exceptions thrown to the consumer.
With Kafka you can simply reset the offsets of the consumer and re-process the events from the moment the bug went live. This however demands you
implement an extra-logic in your software, so that it understands what actions were already done and what weren't, so that you execute the missing steps,
but not redo them. In quite many cases it is _universally_ enough just to remember a record and to ignore the whole consumer handler method call.
And **Kafka EX1** will help you here easily. The only thing you need to have is persistence.

## Requirements
* Java 11
* Spring Boot 2.2
* Spring Kafka
* Redis (the only supported persistence provider at the moment)

## Installation
1. Add dependency to your pom.xml.
We are currently working on publishing the "Kafka EX1" artifact to Maven Central. Whilst this hasn't you can build it on your own. 
   
2. Enable AspectJ proxying:
```java
@EnableAspectJAutoProxy
@SpringBootApplication
public class Application {
  // ...    
}
```

3. Set Spring Redis autoconfiguration properties:
```yaml
spring:
  redis:
    sentinel:
      master: as_op_redis
      nodes:
        - 'redis-01.example.net:26379'
        - 'redis-02.example.net:26379'
        - 'redis-03.example.net:26379'
```

## Usage
In order to use the @IdempotentListener annotation you have to supply Kafka listeners with sets of record parameters 
 that are unique at least within their consumer group.

It can be one surrogate event UUID taken from the header if publish records equipped with that:
```java
class MyTopicListener {
    @KafkaListener(topics = "my-topic")
    @IdempotentListener(ttl = 86400)
    public void onEvent(
            @Payload MyEvent event,
            @IdempotencyId @Header(name = "EVENT_ID") String eventId) {
       // ...
    }   
    // ...
}
```
When this listener method is called @IdempotentListener will take care of iterating over all arguments annotated 
 with @IdempotencyId, build a unique key out of them and persist it in Redis with the configured TTL.
 If the annotation parameter "ttl" is not set, the default global setting is used (also configurable; see below).
 
The most universal way of supplying unique record keys is to use the topic name, partition and offset. You don't need to do 
 anything with producers, because Spring will take care of them automatically:
```java
import org.springframework.kafka.support.KafkaHeaders;

class MyTopicListener {
    @KafkaListener(topics = "my-topic")
    @IdempotentListener
    public void onEvent(
            @Payload MyEvent event,
            @IdempotencyId @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @IdempotencyId @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @IdempotencyId @Header(KafkaHeaders.OFFSET) Long offset) {
       // ...
    }   
    // ...
}
```

Of course you can use shared Redis even if there are multiple applications consuming from same topic.
 The library automatically prefixes all keys with consumer group id taken from the autoconfigured Spring Kafka properties. 

## Configuration
There is a number of properties that can change the behavior of Kafka Idempotency.

| Property | Default | Description |
| :---: | :---: | :---: |
| `idealo.kafka.idempotency.enabled` | true | Use this to disable the library completely |  
| `idealo.kafka.idempotency.listener.checkEnabled` | true  | Using this property you can completely ignore the idempotency check even if the annotation `@IdempotentListener` is set. <br />This might be useful e.g. if you want to temporarily force the listeners re-consume all the events ignoring saved idempotency markers. <br />Note that the idempotency markers keep to be persisted independently of this setting. |  
| `idealo.kafka.idempotency.listener.persistenceEnabled` | true | Using this property you can completely disable saving the idempotency markers. <br /> This might be useful is something in your setup is broken, but you do not want to block record consumption. <br />Note that there'll be no way to provide the only-one semantic for the records that were consumed during this setting was set to false. |  
| `idealo.kafka.idempotency.listener.ttl` | 7d | Duration of the guaranteed idempotency per record. <br />After expiring, the information about a consumed record is removed from persistence. <br />This can be overridden per listener, directly in `@IdempotentListener`. |  
| `idealo.kafka.idempotency.listener.keyPrefix` | kafkaidmp | Prefix used for all the keys persisted in Redis that contain the idempotency information. Whilst in Redis it is only possible to use the TTL feature only on the keys, we cannot use Sets in order to at least namespace the data handled by this library. The workaround for this is to use simple "1-character" strings in the root namespace, where the information for the lookup is hold by the keys. This is how a typical key with the default prefix looks like: <br />`kafkaidmp_myconsumerid_1c9bb6f0-5b91-4be7-acad-6bf089ed0bef`. <br />If the traffic in the topic you use this library is really of a high scale, you should monitor the memory footprint of the idempotency data in Redis. This property give you extra means for optimisation. |  
| `idealo.kafka.idempotency.listener.suppressErrors` | false | If true, any exceptions during the lookup or persistence are logged, but not bubbled up to the listener container. <br />A typical case when this matter is e.g. short outages of the Redis cluster. If this happens, the idempotency data cannot be persisted, hence cannot be later looked up and therefore the idempotency is simply not maintained. Such behavior is inconsistent and should be avoided. However in practice the error handling and acknowledgment logic are sometimes not properly configured, which in case of such an outage leads to skipped records, which is normally worse, than inability to maintain the exactly-one semantic. If this is your situation, you can set this option to true. <br />     * Note that this only changes the behavior of the look up hook, as it already makes no sense to throw an exception after the record handler has correctly finished its work: a retry would make it process the same record again, whereas it indeed relies on the idempotency check to maintain the exactly-one semantics.|  
