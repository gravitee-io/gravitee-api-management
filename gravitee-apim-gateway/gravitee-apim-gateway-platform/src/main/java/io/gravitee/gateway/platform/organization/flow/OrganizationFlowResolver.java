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
package io.gravitee.gateway.platform.organization.flow;

import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.flow.condition.ConditionalFlowResolver;
import io.gravitee.gateway.flow.condition.evaluation.ExpressionLanguageFlowConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import java.util.Collections;
import java.util.List;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OrganizationFlowResolver extends ConditionalFlowResolver {

    private final OrganizationManager organizationManager;

    public OrganizationFlowResolver(final OrganizationManager organizationManager) {
        super(
            new CompositeConditionEvaluator(
                new HttpMethodConditionEvaluator(),
                new PathBasedConditionEvaluator(),
                new ExpressionLanguageFlowConditionEvaluator()
            )
        );
        this.organizationManager = organizationManager;
    }

    @Override
    protected List<FlowV2> resolve0(final ExecutionContext context) {
        ReactableOrganization reactableOrganization = organizationManager.getOrganization(
            (String) context.getAttribute(ExecutionContext.ATTR_ORGANIZATION)
        );
        if (reactableOrganization != null) {
            return reactableOrganization.getFlows() != null ? reactableOrganization.getFlows() : Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
