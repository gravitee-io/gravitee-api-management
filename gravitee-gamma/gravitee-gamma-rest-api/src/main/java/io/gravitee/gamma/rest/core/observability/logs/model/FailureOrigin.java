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
package io.gravitee.gamma.rest.core.observability.logs.model;

/**
 * Which side of the gateway proxying path a logged failure sits on — the primary debugging signal
 * of the event-stream logs screen. Derived server-side: the gateway-reported failure side is
 * authoritative when present, otherwise the error key and connection status are classified by the
 * infra-layer {@code FailureOriginClassifier} (rules shared through
 * {@code NativeFailureOriginRules} in repository-api).
 *
 * <p>Deliberately API-type-agnostic: for native Kafka APIs the upstream side is the Kafka broker;
 * message APIs (once they join the LOGS signal) share the same tripartition, their upstream being
 * the endpoint broker (Kafka, MQTT, RabbitMQ…).
 *
 * @author GraviteeSource Team
 */
public enum FailureOrigin {
    /** No failure recorded on this log entry. */
    NONE,
    /** Failure between the client and the gateway (authentication, SASL handshake, protocol…). */
    CLIENT_TO_GATEWAY,
    /** Failure between the gateway and the upstream broker (availability, topics, groups…). */
    GATEWAY_TO_BROKER,
    /** Failure inside the gateway itself. */
    GATEWAY_INTERNAL,
    /** Failure whose side cannot be determined (no explicit side reported, ambiguous error key). */
    UNKNOWN,
}
