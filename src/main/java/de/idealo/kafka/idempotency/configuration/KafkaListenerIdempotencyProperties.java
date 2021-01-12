package de.idealo.kafka.idempotency.configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import de.idealo.kafka.idempotency.IdempotentListener;

@ConfigurationProperties(prefix = IdealoKafkaIdempotencyAutoconfiguration.PROPERTY_PREFIX + ".listener")
public class KafkaListenerIdempotencyProperties {

    /**
     * Using this property you can completely ignore the idempotency check even if the annotation {@link IdempotentListener} is set.
     * This might be useful e.g. if you want to temporarily force the listeners re-consume all the events ignoring saved idempotency markers.
     * Note that the idempotency markers keep to be persisted independently of this setting.
     */
    private boolean checkEnabled = true;

    /**
     * Using this property you can completely disable saving the idempotency markers.
     * This might be useful is something in your setup is broken, but you do not want to block record consumption.
     * Note that there'll be no way to provide the only-one semantic for the records that were consumed during this setting was set to false.
     */
    private boolean persistenceEnabled = true;

    /**
     * Duration of the guaranteed idempotency per record.
     * After expiring, the information about a consumed record is removed from persistence.
     * If all Kafka topics in your domain have the same retention time, this property should be set equal to it.
     * This can be overridden per listener, directly in @IdempotentListener.
     */
    private Duration ttl = Duration.of(7, ChronoUnit.DAYS);

    /**
     * Prefix used for all the keys persisted in Redis that contain the idempotency information.
     * Whilst in Redis it is only possible to use the TTL feature only on the keys, we cannot use Sets in order to at least namespace the data
     *   handled by this library. The workaround for this is to use simple "1-character" strings in the root namespace, where the information
     *   for the lookup is hold by the keys. This is how a typical key with the default prefix looks like:
     *   kafkaidmp_myconsumerid_1c9bb6f0-5b91-4be7-acad-6bf089ed0bef
     * If the traffic in the topic you use this library is really of a high scale, you should monitor the memory footprint of the idempotency
     *   data in Redis. This property give you extra means for optimisation.
     */
    private String keyPrefix = "";

    /**
     * If true, any exceptions during the lookup or persistence are logged, but not bubbled up to the listener container.
     * A typical case when this matter is e.g. short outages of the Redis cluster. If this happens, the idempotency data cannot
     *  be persisted, hence cannot be later looked up and therefore the idempotency is simply not maintained.
     *  Such behavior is inconsistent and should be avoided. However in practice the error handling and acknowledgment logic
     *  are sometimes not properly configured, which in case of such an outage leads to skipped records, which is normally worse, than
     *  inability to maintain the exactly-one semantic. If this is your situation, you can set this option to true.
     * Note that this only changes the behavior of the look up hook, as it already makes no sense to throw an exception after the record handler
     *  has correctly finished its work: a retry would make it process the same record again, whereas it indeed relies on the idempotency check
     *  to maintain the exactly-one semantics.
     */
    private boolean suppressErrors = false;

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(final Duration ttl) {
        this.ttl = ttl;
    }

    public boolean isCheckEnabled() {
        return checkEnabled;
    }

    public void setCheckEnabled(final boolean checkEnabled) {
        this.checkEnabled = checkEnabled;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public void setPersistenceEnabled(final boolean persistenceEnabled) {
        this.persistenceEnabled = persistenceEnabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(final String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }


    public boolean isSuppressErrors() {
        return suppressErrors;
    }

    public void setSuppressErrors(final boolean suppressErrors) {
        this.suppressErrors = suppressErrors;
    }
}
