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
import io.gravitee.apim.core.api.crud_service.ApiDefinitionCrudService;
import io.gravitee.apim.core.api.domain_service.DeployApiInternalFlowDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardian.model.ApiReference;
import io.gravitee.gamma.rest.core.guardian.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * {@link GuardedApiPort} over the APIM API store.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GuardedApiPortAdapter implements GuardedApiPort {

    private final ApiCrudService apiCrudService;
    private final ApiQueryService apiQueryService;
    private final ApiDefinitionCrudService apiDefinitionCrudService;
    private final DeployApiInternalFlowDomainService deployApiInternalFlow;

    @Override
    public Optional<GuardedApi> findById(String environmentId, String apiId) {
        return apiCrudService.findById(apiId).filter(inEnvironment(environmentId)).map(GuardedApiPortAdapter::toGuardedApi);
    }

    @Override
    public Page<ApiReference> findByGuardian(String environmentId, String guardianId, int pageNumber, int pageSize) {
        var page = apiQueryService.search(
            ApiSearchCriteria.builder().environmentId(environmentId).guardian(guardianId).build(),
            Sortable.builder().field("name").order(Sortable.Order.ASC).build(),
            new PageableImpl(pageNumber, pageSize),
            ApiFieldFilter.builder().definitionExcluded(true).pictureExcluded(true).build()
        );
        return page.map(api -> new ApiReference(api.getId(), api.getName()));
    }

    /**
     * Two guards and an order. The flow comparison closes the window between the caller's read and this call: it
     * names the conflict before anything is written. The conditional write closes the one between the read below and
     * the write itself, and it writes the definition alone — so a {@code PUT} that changed another field of the same
     * API in that window is neither rolled back nor silently overwritten.
     *
     * <p>The order is the one {@link DeployApiInternalFlowDomainService} documents: the deployment to send is built
     * first, because that is what fails on a payload that cannot be read and it fails before any write; then the
     * definition; then the event. A write that does not happen sends nothing.
     */
    @Override
    public GuardedApi updateInternalFlow(AuditInfo auditInfo, String apiId, Flow expectedInternalFlow, Flow internalFlow) {
        var api = apiCrudService
            .findById(apiId)
            .filter(inEnvironment(auditInfo.environmentId()))
            .orElseThrow(() -> new ApiNotFoundException(apiId));
        var definition = v4DefinitionOf(api).orElseThrow(() -> new ApiNotFoundException(apiId));
        if (!Objects.equals(definition.getInternalFlow(), expectedInternalFlow)) {
            throw new ApiInternalFlowChangedException(apiId);
        }
        var deployment = deployApiInternalFlow.prepare(apiId, internalFlow);
        var updated = api
            .toBuilder()
            .apiDefinitionValue(definition.toBuilder().internalFlow(internalFlow).build())
            .updatedAt(TimeProvider.now())
            .build();
        if (!apiDefinitionCrudService.replaceDefinition(updated, api.getUpdatedAt())) {
            throw new ApiInternalFlowChangedException(apiId);
        }
        deployment.ifPresent(patched -> deployApiInternalFlow.deploy(auditInfo, patched));
        return toGuardedApi(updated);
    }

    private static Predicate<Api> inEnvironment(String environmentId) {
        return api -> Objects.equals(api.getEnvironmentId(), environmentId);
    }

    private static GuardedApi toGuardedApi(Api api) {
        var builder = GuardedApi.builder().id(api.getId()).environmentId(api.getEnvironmentId()).name(api.getName()).type(api.getType());
        v4DefinitionOf(api).ifPresent(definition -> builder.supportsGuardians(true).internalFlow(definition.getInternalFlow()));
        return builder.build();
    }

    /** The definition of an API that can hold an internal flow, empty for a native or an edge one. */
    private static Optional<io.gravitee.definition.model.v4.Api> v4DefinitionOf(Api api) {
        return api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api definition
            ? Optional.of(definition)
            : Optional.empty();
    }
}
