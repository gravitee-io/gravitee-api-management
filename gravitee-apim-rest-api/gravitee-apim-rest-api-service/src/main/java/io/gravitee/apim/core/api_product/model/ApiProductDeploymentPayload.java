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
package io.gravitee.apim.core.api_product.model;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Purpose-built serialization DTO for the DEPLOY_API_PRODUCT event payload.
 * Carries API Product metadata together with embedded plan definitions so the
 * gateway does not need to perform an extra DB query at sync time.
 */
@Data
@Builder
public class ApiProductDeploymentPayload {

    private String id;
    private String name;
    private String description;
    private String version;
    private Set<String> apiIds;
    private String environmentId;
    private Set<String> tags;
    private List<Plan> plans;
    private Analytics analytics;
    private List<Flow> flows;
    private FlowExecution flowExecution;

    @ToString.Exclude
    private List<Resource> resources;

    /** A product that never saved a flow mode runs its flows in the default one. */
    public static ApiProductDeploymentPayload of(ApiProduct apiProduct, List<Plan> plans, List<Flow> flows) {
        return ApiProductDeploymentPayload.builder()
            .id(apiProduct.getId())
            .name(apiProduct.getName())
            .description(apiProduct.getDescription())
            .version(apiProduct.getVersion())
            .apiIds(apiProduct.getApiIds())
            .environmentId(apiProduct.getEnvironmentId())
            .tags(apiProduct.getTags())
            .plans(plans)
            .analytics(apiProduct.getAnalytics())
            .flows(flows)
            .flowExecution(apiProduct.getFlowExecution() != null ? apiProduct.getFlowExecution() : new FlowExecution())
            .resources(apiProduct.getResources())
            .build();
    }
}
