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
package io.gravitee.gamma.module.platform.infra.service_provider;

import io.gravitee.am.sdk.management.invoker.ApiException;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.AmConnectionTestResult;
import io.gravitee.gamma.module.platform.core.am.port.service_provider.AmConnectionTester;
import io.gravitee.gamma.module.platform.infra.service_provider.AmSdkClientFactory.AmApis;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AmSdkConnectionTester implements AmConnectionTester {

    private final AmSdkClientFactory clientFactory;

    @Override
    public AmConnectionTestResult test(String orgId, AmConnection connection) {
        if (connection.baseUrl() == null || connection.baseUrl().isBlank()) {
            return AmConnectionTestResult.failure(400, "baseUrl is required");
        }
        if (connection.serviceAccountAccessToken() == null || connection.serviceAccountAccessToken().isBlank()) {
            return AmConnectionTestResult.failure(400, "service-account access token is required");
        }

        AmApis apis = clientFactory.forConnection(connection);
        try {
            AmSdkInvocations.await(apis.defaults().listEnvironments(orgId));
            return AmConnectionTestResult.success();
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ApiException ae) {
                log.warn("AM test connection failed for org {}: {} ({})", orgId, ae.getCode(), ae.getMessage());
                return AmConnectionTestResult.failure(ae.getCode(), truncate(ae.getResponseBody()));
            }
            log.warn("AM test connection failed for org {}: {}", orgId, e.getMessage());
            return AmConnectionTestResult.failure(503, "AM unreachable: " + e.getMessage());
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
