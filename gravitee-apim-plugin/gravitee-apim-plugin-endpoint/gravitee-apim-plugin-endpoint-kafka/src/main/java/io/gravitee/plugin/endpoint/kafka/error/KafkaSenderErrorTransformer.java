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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderResult;
import reactor.util.retry.Retry;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaSenderErrorTransformer extends AbstractKafkaErrorTransformer {

    private static final int KAFKA_CONNECTION_CHECK_INTERVAL = RECONNECT_ATTEMPT * RECONNECT_BACKOFF_MS;

    public static <K, V, T> Function<Flux<SenderResult<T>>, Publisher<SenderResult<T>>> transform(final KafkaSender<K, V> kafkaSender) {
        return consumerRecordFlux -> {
            AtomicReference<Producer<K, V>> producerRef = new AtomicReference<>();
            Sinks.Many<SenderResult<T>> kafkaErrorSink = Sinks.many().unicast().onBackpressureError();
            return consumerRecordFlux
                .zipWith(storeProduceReference(kafkaSender, producerRef), (r, o) -> r)
                .materialize()
                .mergeWith(kafkaErrorSink.asFlux().materialize())
                .<SenderResult<T>>dematerialize()
                .doOnSubscribe(subscription -> handleKafkaDisconnection(producerRef, kafkaErrorSink))
                .doOnError(throwable -> kafkaSender.close());
        };
    }

    private static <K, V> Flux<Boolean> storeProduceReference(
        final KafkaSender<K, V> kafkaSender,
        final AtomicReference<Producer<K, V>> producerRef
    ) {
        return Flux
            .defer(
                () ->
                    kafkaSender.doOnProducer(
                        producer -> {
                            producerRef.set(producer);
                            return true;
                        }
                    )
            )
            .cache()
            .repeat();
    }

    private static <V, K, T> void handleKafkaDisconnection(
        final AtomicReference<Producer<K, V>> producerRef,
        final Sinks.Many<SenderResult<T>> kafkaErrorSink
    ) {
        Flux
            .interval(Duration.ofMillis(KAFKA_CONNECTION_CHECK_INTERVAL))
            .publishOn(Schedulers.boundedElastic())
            .doOnNext(
                interval -> {
                    Producer<K, V> producer = producerRef.get();
                    if (producer != null) {
                        mayThrowConnectionClosedException(kafkaErrorSink, producer.metrics());
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
