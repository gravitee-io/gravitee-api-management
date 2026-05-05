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
package io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi;

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asMapOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.RequestV2MetricsV4Fields;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetrics;

final class NativeApiMetricsSourceMapper {

    private NativeApiMetricsSourceMapper() {}

    static NativeApiMetrics from(JsonNode source) {
        return NativeApiMetrics.builder()
            .apiId(asTextOrNull(source.get(RequestV2MetricsV4Fields.API_ID.v4Metrics())))
            .requestId(asTextOrNull(source.get(RequestV2MetricsV4Fields.REQUEST_ID.v4Metrics())))
            .transactionId(asTextOrNull(source.get(RequestV2MetricsV4Fields.TRANSACTION_ID.v4Metrics())))
            .timestamp(asTextOrNull(source.get(RequestV2MetricsV4Fields.TIMESTAMP)))
            .applicationId(asTextOrNull(source.get(RequestV2MetricsV4Fields.APPLICATION_ID.v4Metrics())))
            .planId(asTextOrNull(source.get(RequestV2MetricsV4Fields.PLAN_ID.v4Metrics())))
            .clientIdentifier(asTextOrNull(source.get(RequestV2MetricsV4Fields.CLIENT_IDENTIFIER.v4Metrics())))
            .subscriptionId(asTextOrNull(source.get(RequestV2MetricsV4Fields.SUBSCRIPTION_ID)))
            .entrypointId(asTextOrNull(source.get(RequestV2MetricsV4Fields.ENTRYPOINT_ID.v4Metrics())))
            .gateway(asTextOrNull(source.get(RequestV2MetricsV4Fields.GATEWAY)))
            .remoteAddress(asTextOrNull(source.get(RequestV2MetricsV4Fields.REMOTE_ADDRESS)))
            .localAddress(asTextOrNull(source.get(RequestV2MetricsV4Fields.LOCAL_ADDRESS)))
            .host(asTextOrNull(source.get(RequestV2MetricsV4Fields.HOST)))
            .errorKey(asTextOrNull(source.get(RequestV2MetricsV4Fields.ERROR_KEY)))
            .message(asTextOrNull(source.get(RequestV2MetricsV4Fields.MESSAGE.v4Metrics())))
            .additionalMetrics(asMapOrNull(source.get(RequestV2MetricsV4Fields.ADDITIONAL_METRICS)))
            .build();
    }
}
