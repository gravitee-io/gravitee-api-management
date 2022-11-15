/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.plugin.endpoint.kafka.error;

import static io.gravitee.plugin.endpoint.kafka.configuration.KafkaDefaultConfiguration.RECONNECT_ATTEMPT;
import static io.gravitee.plugin.endpoint.kafka.configuration.KafkaDefaultConfiguration.RECONNECT_BACKOFF_MS;

import io.gravitee.plugin.endpoint.kafka.strategy.QosStrategy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaReceiverErrorTransformer extends AbstractKafkaErrorTransformer {

    private static final int KAFKA_CONNECTION_CHECK_INTERVAL = RECONNECT_ATTEMPT * RECONNECT_BACKOFF_MS;

    public static <K, V> Function<Flux<ConsumerRecord<K, V>>, Publisher<ConsumerRecord<K, V>>> transform(
        final QosStrategy<K, V> qosStrategy
    ) {
        return consumerRecordFlux -> {
            AtomicReference<Consumer<K, V>> consumerRef = new AtomicReference<>();
            Sinks.Many<ConsumerRecord<K, V>> kafkaErrorSink = Sinks.many().unicast().onBackpressureError();
            return consumerRecordFlux
                .zipWith(storeConsumerReference(qosStrategy, consumerRef), (c, o) -> c)
                .materialize()
                .mergeWith(kafkaErrorSink.asFlux().materialize())
                .<ConsumerRecord<K, V>>dematerialize()
                .doOnSubscribe(subscription -> handleKafkaDisconnection(consumerRef, kafkaErrorSink));
        };
    }

    private static <K, V> Flux<Boolean> storeConsumerReference(
        final QosStrategy<K, V> qosStrategy,
        final AtomicReference<Consumer<K, V>> consumerRef
    ) {
        return Flux
            .defer(
                () ->
                    qosStrategy
                        .kafkaReceiver()
                        .doOnConsumer(
                            consumer -> {
                                consumerRef.set(consumer);
                                return true;
                            }
                        )
            )
            .cache()
            .repeat();
    }

    private static <K, V> void handleKafkaDisconnection(
        final AtomicReference<Consumer<K, V>> consumerRef,
        final Sinks.Many<ConsumerRecord<K, V>> kafkaErrorSink
    ) {
        Flux
            .interval(Duration.ofMillis(KAFKA_CONNECTION_CHECK_INTERVAL))
            .publishOn(Schedulers.boundedElastic())
            .doOnNext(
                interval -> {
                    Consumer<K, V> consumer = consumerRef.get();
                    if (consumer != null) {
                        mayThrowConnectionClosedException(kafkaErrorSink, consumer.metrics());
                    }
                }
            )
            .retryWhen(Retry.indefinitely().filter(throwable -> !(throwable instanceof KafkaConnectionClosedException)))
            .subscribe(
                interval -> log.debug("Kafka endpoint connection validated."),
                throwable -> log.warn("Kafka endpoint connector disconnected.")
            );
    }
}
