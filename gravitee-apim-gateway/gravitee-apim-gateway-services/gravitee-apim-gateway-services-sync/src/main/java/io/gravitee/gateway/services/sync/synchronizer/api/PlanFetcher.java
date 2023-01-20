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
package io.gravitee.gateway.services.sync.synchronizer.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlanFetcher {

    private final Logger logger = LoggerFactory.getLogger(PlanFetcher.class);
    private final ObjectMapper objectMapper;
    private final PlanRepository planRepository;

    public PlanFetcher(ObjectMapper objectMapper, PlanRepository planRepository) {
        this.objectMapper = objectMapper;
        this.planRepository = planRepository;
    }

    /**
     * Allows to start fetching plans in a bulk fashion way.
     * @param upstream the api upstream which will be chunked into packs in order to fetch plan v1.
     * @param bulkSize the bulk size to chunk api upstream.
     * @return he same flow of apis.
     */
    @NonNull
    public Flowable<ReactableApi<?>> fetchApiPlans(Flowable<ReactableApi<?>> upstream, int bulkSize) {
        return upstream
            .groupBy(ReactableApi::getDefinitionVersion)
            .flatMap(
                apisByDefinitionVersion -> {
                    if (apisByDefinitionVersion.getKey() == DefinitionVersion.V1) {
                        return apisByDefinitionVersion.buffer(bulkSize).flatMap(this::fetchV1ApiPlans);
                    } else {
                        return apisByDefinitionVersion;
                    }
                }
            );
    }

    private Flowable<ReactableApi<?>> fetchV1ApiPlans(List<ReactableApi<?>> apiDefinitions) {
        final Map<String, Api> apisById = apiDefinitions
            .stream()
            .map(reactableApi -> (io.gravitee.gateway.handlers.api.definition.Api) reactableApi)
            .collect(Collectors.toMap(io.gravitee.gateway.handlers.api.definition.Api::getId, api -> api));

        // Get the api id to load plan only for V1 api definition.
        final List<String> apiV1Ids = new ArrayList<>(apisById.keySet());

        try {
            final Map<String, List<Plan>> plansByApi = planRepository
                .findByApis(apiV1Ids)
                .stream()
                .collect(Collectors.groupingBy(Plan::getApi));

            plansByApi.forEach(
                (key, value) -> {
                    final io.gravitee.gateway.handlers.api.definition.Api api = apisById.get(key);

                    if (api.getDefinitionVersion() == DefinitionVersion.V1) {
                        // Deploy only published plan
                        api
                            .getDefinition()
                            .setPlans(
                                value
                                    .stream()
                                    .filter(
                                        plan ->
                                            Plan.Status.PUBLISHED.equals(plan.getStatus()) ||
                                            Plan.Status.DEPRECATED.equals(plan.getStatus())
                                    )
                                    .map(this::convert)
                                    .collect(Collectors.toList())
                            );
                    }
                }
            );
        } catch (TechnicalException te) {
            logger.error("Unexpected error while loading plans of APIs: [{}]", apiV1Ids, te);
        }

        return Flowable.fromIterable(apiDefinitions);
    }

    private io.gravitee.definition.model.Plan convert(Plan repoPlan) {
        io.gravitee.definition.model.Plan plan = new io.gravitee.definition.model.Plan();

        plan.setId(repoPlan.getId());
        plan.setName(repoPlan.getName());
        plan.setSecurityDefinition(repoPlan.getSecurityDefinition());
        plan.setSelectionRule(repoPlan.getSelectionRule());
        plan.setTags(repoPlan.getTags());
        plan.setStatus(repoPlan.getStatus().name());
        plan.setApi(repoPlan.getApi());

        if (repoPlan.getSecurity() != null) {
            plan.setSecurity(repoPlan.getSecurity().name());
        } else {
            // TODO: must be handle by a migration script
            plan.setSecurity("api_key");
        }

        try {
            if (repoPlan.getDefinition() != null && !repoPlan.getDefinition().trim().isEmpty()) {
                HashMap<String, List<Rule>> paths = objectMapper.readValue(repoPlan.getDefinition(), new TypeReference<>() {});

                plan.setPaths(paths);
            }
        } catch (IOException ioe) {
            logger.error("Unexpected error while converting plan: {}", plan, ioe);
        }

        return plan;
    }
}
