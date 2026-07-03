/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.fake;

import io.gravitee.gateway.reactive.api.context.EntrypointConnectContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaConnectionContext;
import io.gravitee.gateway.reactive.api.exception.InterruptConnectionException;
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;
import io.reactivex.rxjava3.core.Completable;

/**
 * Test-only Kafka policy that interrupts the connection at setup time. Throwing
 * {@link InterruptConnectionException} at the connection-level (initialize / entrypoint-connect) bypasses the
 * request-flow {@code KafkaApiReactorPolicyChains} wrapping, so the reactor's {@code handleError} typeSwitch
 * sees the bare {@code InterruptConnectionException} and emits {@code CONNECTION_ERROR}.
 */
public class KafkaInterruptingConnectPolicy implements KafkaPolicy {

    public static final String ID = "kafka-interrupting-connect";
    public static final String FAILURE_MESSAGE = "policy-driven connection interruption";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Completable onInitialize(KafkaConnectionContext ctx) {
        return Completable.error(new InterruptConnectionException(FAILURE_MESSAGE));
    }

    @Override
    public Completable onEntrypointConnect(EntrypointConnectContext ctx) {
        return Completable.error(new InterruptConnectionException(FAILURE_MESSAGE));
    }
}
