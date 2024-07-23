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
package io.gravitee.apim.gateway.tests.sdk.converters;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.Collections;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class V4ApiDeploymentPreparer implements ApiDeploymentPreparer<Api> {

    @Override
    public ReactableApi<Api> toReactable(Api definition, String environmentId) {
        final io.gravitee.gateway.reactive.handlers.api.v4.Api api = new io.gravitee.gateway.reactive.handlers.api.v4.Api(definition);
        api.setEnvironmentId(environmentId);
        return api;
    }

    @Override
    public void ensureMinimalRequirementForApi(Api definition) {
        if (definition.getType() == null) {
            throw new PreconditionViolationException("'type' field must be defined on a V4 API Definition");
        }
        this.addDefaultKeylessPlanIfNeeded(definition);
        this.addDefaultEndpointGroupIfNeeded(definition);
    }

    /**
     * Override api plans to create a default Keyless plan and ensure their are published
     * @param api is the api to override
     */
    protected void addDefaultKeylessPlanIfNeeded(Api api) {
        if (api.getPlans() == null || api.getPlans().isEmpty()) {
            // By default, add a keyless plan to the API
            Plan plan = new Plan();
            plan.setId("default_plan");
            plan.setName("Default plan");
            final PlanSecurity planSecurity = new PlanSecurity();
            planSecurity.setType("key-less");
            plan.setSecurity(planSecurity);
            plan.setMode(PlanMode.STANDARD);
            plan.setStatus(PlanStatus.PUBLISHED);

            api.setPlans(Collections.singletonList(plan));
        } else {
            api.getPlans().stream().filter(plan -> plan.getStatus() == null).forEach(plan -> plan.setStatus(PlanStatus.PUBLISHED));
        }
    }

    /**
     * Add a default endpoint group to the api
     */
    private void addDefaultEndpointGroupIfNeeded(Api api) {
        if (ApiType.PROXY.equals(api.getType())) {
            // TODO: implement this when sync api endpoints will be implemented
        }
    }
}
