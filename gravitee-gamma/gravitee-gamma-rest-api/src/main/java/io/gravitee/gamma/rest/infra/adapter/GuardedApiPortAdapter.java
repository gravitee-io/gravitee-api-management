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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.DeployApiInternalFlowDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiCannotRunGuardrailsException;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardrail.model.ApiReference;
import io.gravitee.gamma.rest.core.guardrail.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * {@link GuardedApiPort} over the APIM API store.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GuardedApiPortAdapter implements GuardedApiPort {

    /** Shown in the API's deployment history, on the entry a Guardrail change republishes. */
    private static final String DEPLOYMENT_LABEL = "Guardrails updated";

    private final ApiCrudService apiCrudService;
    private final ApiQueryService apiQueryService;
    private final DeployApiInternalFlowDomainService deployApiInternalFlow;

    @Override
    public Optional<GuardedApi> findById(String environmentId, String apiId) {
        return apiCrudService
            .findById(apiId)
            .filter(api -> api.belongsToEnvironment(environmentId))
            .map(GuardedApis::of);
    }

    @Override
    public Page<ApiReference> findByGuardrail(String environmentId, String guardrailId, int pageNumber, int pageSize) {
        var page = apiQueryService.search(
            ApiSearchCriteria.builder().environmentId(environmentId).guardrail(guardrailId).build(),
            Sortable.builder().field("name").order(Sortable.Order.ASC).build(),
            new PageableImpl(pageNumber, pageSize),
            ApiFieldFilter.builder().definitionExcluded(true).pictureExcluded(true).build()
        );
        return page.map(api -> new ApiReference(api.getId(), api.getName()));
    }

    /**
     * Two guards. The flow comparison closes the window between the caller's read and this call: it names the
     * conflict before anything is written. The conditional write closes the one between the read below and the write
     * itself, and it writes the definition alone — so a {@code PUT} that changed another field of the same API in
     * that window is neither rolled back nor silently overwritten.
     *
     * <p>{@link DeployApiInternalFlowDomainService#replaceInternalFlow} writes the definition and hands it to the
     * gateways, and does neither when the write loses the compare-and-swap.
     */
    @Override
    public GuardedApi updateInternalFlow(AuditInfo auditInfo, String apiId, Flow expectedInternalFlow, Flow internalFlow) {
        var api = apiCrudService
            .findById(apiId)
            .filter(stored -> stored.belongsToEnvironment(auditInfo.environmentId()))
            .orElseThrow(() -> new ApiNotFoundException(apiId));
        if (!api.holdsInternalFlow()) {
            throw new ApiCannotRunGuardrailsException(apiId);
        }
        if (!Objects.equals(api.internalFlow(), expectedInternalFlow)) {
            throw new ApiInternalFlowChangedException(apiId);
        }
        var updated = api.withInternalFlow(internalFlow).toBuilder().updatedAt(TimeProvider.now()).build();
        if (!deployApiInternalFlow.replaceInternalFlow(auditInfo, updated, api.getUpdatedAt(), DEPLOYMENT_LABEL)) {
            throw new ApiInternalFlowChangedException(apiId);
        }
        return GuardedApis.of(updated);
    }
}
