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
package io.gravitee.apim.infra.domain_service.logs_engine;

import io.gravitee.apim.core.logs_engine.domain_service.LogNamesPostProcessor;
import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.core.user.model.UserContext;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
public class LogNamesPostProcessorImpl implements LogNamesPostProcessor {

    private static final String UNKNOWN_APPLICATION = "Unknown";

    @Override
    public SearchLogsResponse mapLogNames(UserContext context, SearchLogsResponse response) {
        var enrichedLogs = response
            .data()
            .stream()
            .map(log -> enrichLog(context, log))
            .toList();
        return new SearchLogsResponse(enrichedLogs, response.pagination());
    }

    private ApiLog enrichLog(UserContext context, ApiLog log) {
        var apiName = context
            .apiNameById()
            .map(m -> m.get(log.apiId()))
            .orElse(null);

        var plan = log.plan() == null
            ? null
            : log
                .plan()
                .withName(
                    context
                        .planNameById()
                        .map(m -> m.get(log.plan().id()))
                        .orElse(null)
                );

        var application = log.application() == null
            ? null
            : log
                .application()
                .withName(
                    context
                        .applicationNameById()
                        .map(m -> m.get(log.application().id()))
                        .orElse(UNKNOWN_APPLICATION)
                );

        var gateway = log.gateway() == null
            ? null
            : context
                .gatewayHostnameById()
                .map(m -> m.getOrDefault(log.gateway(), log.gateway()))
                .orElse(log.gateway());

        return log.withApiName(apiName).withPlan(plan).withApplication(application).withGateway(gateway);
    }
}
