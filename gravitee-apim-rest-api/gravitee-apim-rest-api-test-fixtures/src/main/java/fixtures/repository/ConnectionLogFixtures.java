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
package fixtures.repository;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import java.util.UUID;

public class ConnectionLogFixtures {

    private static final BaseConnectionLog.BaseConnectionLogBuilder<?, ?> BASE = BaseConnectionLog
        .builder()
        .apiId("api-id")
        .applicationId("app-id")
        .planId("plan-id")
        .clientIdentifier("client-identifier")
        .status(200)
        .method(HttpMethod.GET)
        .transactionId("transaction-id")
        .requestEnded(true)
        .timestamp("2020-02-01T20:00:00.00Z")
        .gatewayResponseTime(42)
        .uri("/my-api");

    public ConnectionLogFixtures(String defaultApiId, String defaultApplicationId, String defaultPlanId) {
        BASE.apiId(defaultApiId).applicationId(defaultApplicationId).planId(defaultPlanId);
    }

    public BaseConnectionLog aConnectionLog() {
        return BASE.requestId(UUID.randomUUID().toString()).build();
    }

    public BaseConnectionLog aConnectionLog(String requestId) {
        return BASE.requestId(requestId).build();
    }
}
