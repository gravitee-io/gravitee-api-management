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
package io.gravitee.apim.core.api_product.use_case;

import static io.gravitee.apim.core.utils.CollectionUtils.isEmpty;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductFlows;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the flows of an API Product and the mode they run in, leaving every other field of the product as stored.
 *
 * <p>The product runs its flows on the requests of its member APIs, which are HTTP proxies, so the flows are validated
 * as proxy flows and may only hold request and response steps.
 */
@UseCase
@RequiredArgsConstructor
public class UpdateApiProductFlowsUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductCrudService apiProductCrudService;
    private final FlowCrudService flowCrudService;
    private final FlowValidationDomainService flowValidationDomainService;
    private final AuditDomainService auditService;
    private final LicenseDomainService licenseDomainService;

    public Output execute(Input input) {
        if (!licenseDomainService.isApiProductDeploymentAllowed(input.auditInfo().organizationId())) {
            throw new ForbiddenFeatureException("api-product");
        }
        var apiProduct = apiProductQueryService
            .findById(input.apiProductId())
            .filter(product -> input.auditInfo().environmentId().equals(product.getEnvironmentId()))
            .orElseThrow(() -> new ApiProductNotFoundException(input.apiProductId()));
        var requested = input.apiProductFlows();
        rejectStepsOutsideRequestAndResponse(requested.flows());
        // A product declares no resources, and its flows must never resolve those of its member APIs.
        var flows = flowValidationDomainService.validateAndSanitizeHttpV4(ApiType.PROXY, requested.flows(), Set.of());

        var before = new ApiProductFlows(apiProduct.getFlowExecution(), flowCrudService.getApiProductFlows(apiProduct.getId()));
        var savedFlows = flowCrudService.saveApiProductFlows(apiProduct.getId(), flows);
        apiProduct.setFlowExecution(requested.flowExecution());
        apiProduct.setUpdatedAt(TimeProvider.now());
        apiProductCrudService.update(apiProduct);
        var after = new ApiProductFlows(requested.flowExecution(), savedFlows);

        createAuditLog(apiProduct, before, after, input.auditInfo());
        return new Output(after);
    }

    private static void rejectStepsOutsideRequestAndResponse(List<Flow> flows) {
        for (int index = 0; index < flows.size(); index++) {
            var flow = flows.get(index);
            if (!isEmpty(flow.getPublish()) || !isEmpty(flow.getSubscribe())) {
                var flowName = flow.getName() != null ? flow.getName() : "#" + index;
                throw new ValidationDomainException(
                    "Flow '" + flowName + "' has publish or subscribe steps; API Product flows only run in the request and response phases"
                );
            }
        }
    }

    private void createAuditLog(ApiProduct apiProduct, ApiProductFlows before, ApiProductFlows after, AuditInfo auditInfo) {
        auditService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ApiProductAuditEvent.API_PRODUCT_UPDATED)
                .apiProductId(apiProduct.getId())
                .actor(auditInfo.actor())
                .oldValue(before)
                .newValue(after)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.API_PRODUCT, apiProduct.getId()))
                .build()
        );
    }

    public record Input(String apiProductId, ApiProductFlows apiProductFlows, AuditInfo auditInfo) {}

    public record Output(ApiProductFlows apiProductFlows) {}
}
