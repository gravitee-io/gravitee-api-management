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

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.Collections;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LegacyApiDeploymentPreparer implements ApiDeploymentPreparer<Api> {

    @Override
    public ReactableApi<Api> toReactable(Api definition, String environmentId) {
        final io.gravitee.gateway.handlers.api.definition.Api api = new io.gravitee.gateway.handlers.api.definition.Api(definition);
        api.setEnvironmentId(environmentId);
        return api;
    }

    @Override
    public void ensureMinimalRequirementForApi(Api definition) {
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
            plan.setSecurity("key_less");
            plan.setStatus("published");

            api.setPlans(Collections.singletonList(plan));
        } else {
            api
                .getPlans()
                .stream()
                .filter(plan -> plan.getStatus() == null || plan.getStatus().isEmpty())
                .forEach(plan -> plan.setStatus("published"));
        }
    }

    /**
     * Add a default endpoint group to the api
     */
    private void addDefaultEndpointGroupIfNeeded(Api api) {
        if (api.getProxy().getGroups() == null || api.getProxy().getGroups().isEmpty()) {
            // Create a default endpoint group
            EndpointGroup group = new EndpointGroup();
            group.setName("default");
            group.setEndpoints(Collections.emptySet());
            api.getProxy().setGroups(Collections.singleton(group));
        }
    }
}
