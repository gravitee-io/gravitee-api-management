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
package io.gravitee.gateway.services.sync.synchronizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanFetcher {

    private final Logger logger = LoggerFactory.getLogger(PlanFetcher.class);

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private PlanRepository planRepository;

    private int bulkItems;

    private Plan.Status[] statusToFetch;

    public PlanFetcher(int bulkItems, Plan.Status... statusToFetch) {
        this.bulkItems = bulkItems;
        this.statusToFetch = statusToFetch;
    }

    /**
     * Allows to start fetching plans in a bulk fashion way.
     * @param upstream the api upstream which will be chunked into packs of 50 in order to fetch plan v1.
     * @return he same flow of apis.
     */
    @NonNull
    public Flowable<Api> fetchApiPlans(Flowable<? extends io.gravitee.gateway.handlers.api.definition.Api> upstream) {
        return upstream
            .groupBy(io.gravitee.definition.model.Api::getDefinitionVersion)
            .flatMap(
                apisByDefinitionVersion -> {
                    if (apisByDefinitionVersion.getKey() == DefinitionVersion.V1) {
                        return apisByDefinitionVersion.buffer(bulkItems).flatMap(this::fetchV1ApiPlans);
                    } else {
                        return apisByDefinitionVersion.flatMapSingle(this::fetchV2ApiPlans);
                    }
                }
            );
    }

    private Flowable<io.gravitee.gateway.handlers.api.definition.Api> fetchV1ApiPlans(List<? extends Api> apiDefinitions) {
        final Map<String, Api> apisById = apiDefinitions
            .stream()
            .collect(Collectors.toMap(io.gravitee.definition.model.Api::getId, api -> api));

        // Get the api id to load plan only for V1 api definition.
        final List<String> apiV1Ids = new ArrayList<>(apisById.keySet());

        try {
            final Map<String, List<Plan>> plansByApi = planRepository
                .findByApis(apiV1Ids)
                .stream()
                .collect(Collectors.groupingBy(Plan::getApi));

            plansByApi.forEach(
                (key, value) -> {
                    final io.gravitee.gateway.handlers.api.definition.Api definition = apisById.get(key);

                    if (definition.getDefinitionVersion() == DefinitionVersion.V1) {
                        // Deploy only published plan
                        definition.setPlans(value.stream().filter(this::filterPlan).map(this::convert).collect(Collectors.toList()));
                    }
                }
            );
        } catch (TechnicalException te) {
            logger.error("Unexpected error while loading plans of APIs: [{}]", apiV1Ids, te);
        }

        return Flowable.fromIterable(apiDefinitions);
    }

    private Single<Api> fetchV2ApiPlans(io.gravitee.gateway.handlers.api.definition.Api apiDefinition) {
        apiDefinition.setPlans(apiDefinition.getPlans().stream().filter(this::filterPlan).collect(Collectors.toList()));
        return Single.just(apiDefinition);
    }

    protected boolean filterPlan(io.gravitee.definition.model.Plan plan) {
        return Arrays.stream(statusToFetch).anyMatch(status -> status.name().equalsIgnoreCase(plan.getStatus()));
    }

    protected boolean filterPlan(Plan plan) {
        return Arrays.stream(statusToFetch).anyMatch(st -> st.equals(plan.getStatus()));
    }

    private io.gravitee.definition.model.Plan convert(Plan repoPlan) {
        io.gravitee.definition.model.Plan plan = new io.gravitee.definition.model.Plan();

        plan.setId(repoPlan.getId());
        plan.setName(repoPlan.getName());
        plan.setSecurityDefinition(repoPlan.getSecurityDefinition());
        plan.setSelectionRule(repoPlan.getSelectionRule());
        plan.setTags(repoPlan.getTags());

        if (repoPlan.getSecurity() != null) {
            plan.setSecurity(repoPlan.getSecurity().name());
        } else {
            // TODO: must be handle by a migration script
            plan.setSecurity("api_key");
        }

        try {
            if (repoPlan.getDefinition() != null && !repoPlan.getDefinition().trim().isEmpty()) {
                HashMap<String, List<Rule>> paths = objectMapper.readValue(
                    repoPlan.getDefinition(),
                    new TypeReference<HashMap<String, List<Rule>>>() {}
                );

                plan.setPaths(paths);
            }
        } catch (IOException ioe) {
            logger.error("Unexpected error while converting plan: {}", plan, ioe);
        }

        return plan;
    }
}
