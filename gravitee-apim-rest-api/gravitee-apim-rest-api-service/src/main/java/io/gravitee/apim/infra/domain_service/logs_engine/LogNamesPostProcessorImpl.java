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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.logs_engine.domain_service.LogNamesPostProcessor;
import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.definition.model.v4.ApiType;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
public class LogNamesPostProcessorImpl implements LogNamesPostProcessor {

    private static final String UNKNOWN_APPLICATION = "Unknown";

    @Override
    public SearchLogsResponse mapLogNames(UserContext context, SearchLogsResponse response) {
        var apiTypeById = context
            .apis()
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(api -> api.getType() != null)
            .collect(Collectors.toMap(Api::getId, Api::getType, (a, b) -> a));

        var enrichedLogs = response
            .data()
            .stream()
            .map(log -> enrichLog(context, log, apiTypeById))
            .toList();
        return new SearchLogsResponse(enrichedLogs, response.pagination());
    }

    private ApiLog enrichLog(UserContext context, ApiLog log, Map<String, ApiType> apiTypeById) {
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
                .map(m -> m.get(log.gateway()))
                .orElse(log.gateway());

        var apiType = apiTypeById.get(log.apiId());

        return log.toBuilder().apiName(apiName).apiType(apiType).plan(plan).application(application).gateway(gateway).build();
    }
}
