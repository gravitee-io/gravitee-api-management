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
package io.gravitee.repository.log.v4.model.connection;

public final class NativeApiMetricKeys {

    public static final String CLIENT_ID = "keyword_native-kafka_client-id";
    public static final String BROKER_ID = "keyword_native-kafka_broker-id";
    public static final String CONNECTION_STATUS = "keyword_native-kafka_connection-status";
    public static final String CONNECTION_DURATION_MS = "long_native-kafka_connection-duration-ms";

    private NativeApiMetricKeys() {}
}
