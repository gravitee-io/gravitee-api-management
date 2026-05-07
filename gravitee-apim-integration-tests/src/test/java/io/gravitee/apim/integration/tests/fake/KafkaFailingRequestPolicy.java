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

import io.gravitee.gateway.reactive.api.context.kafka.KafkaExecutionContext;
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;
import io.reactivex.rxjava3.core.Completable;

/**
 * Test-only Kafka policy that fails every request flow. The thrown exception is wrapped by
 * {@code KafkaApiReactorPolicyChains} into a {@code KafkaException}, which the reactor's error
 * dispatch routes to {@code reportSessionError} → {@code SESSION_ERROR}.
 */
public class KafkaFailingRequestPolicy implements KafkaPolicy {

    public static final String ID = "kafka-failing-request";
    public static final String FAILURE_MESSAGE = "policy-driven session failure";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Completable onRequest(KafkaExecutionContext ctx) {
        return Completable.error(new RuntimeException(FAILURE_MESSAGE));
    }
}
