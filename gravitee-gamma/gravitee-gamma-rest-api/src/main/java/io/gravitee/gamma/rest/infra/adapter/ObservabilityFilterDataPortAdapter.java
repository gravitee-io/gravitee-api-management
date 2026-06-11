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

import io.gravitee.apim.core.analytics_engine.use_case.GetFilterValuesUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ResolveFilterLabelsUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValuesPage;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link ObservabilityFilterDataPort} adapter that delegates to the platform's analytics use cases
 * ({@link GetFilterValuesUseCase}, {@link ResolveFilterLabelsUseCase}) — they already own the
 * Elasticsearch distinct-value translation, the management-DB name lookups, and the caller-scoped
 * authorized-API set. This reuses the proven logs/analytics machinery instead of re-implementing it,
 * keeping the unified Gamma surface a thin, vocabulary-driven facade over it.
 *
 * <p>The audit/actor context (organization, environment, user) is read from the ambient request
 * state ({@link GraviteeContext} + the Spring security principal), mirroring how the apim management
 * resources build their {@link AuditInfo}.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ObservabilityFilterDataPortAdapter implements ObservabilityFilterDataPort {

    private final GetFilterValuesUseCase getFilterValuesUseCase;
    private final ResolveFilterLabelsUseCase resolveFilterLabelsUseCase;

    @Override
    public FilterValuesPage listKeywordValues(String filterName, String query, Long from, Long to, int page, int perPage) {
        try {
            Instant fromInstant = from != null ? Instant.ofEpochMilli(from) : null;
            Instant toInstant = to != null ? Instant.ofEpochMilli(to) : null;
            var output = getFilterValuesUseCase.execute(
                new GetFilterValuesUseCase.Input(currentAuditInfo(), filterName, fromInstant, toInstant, page, perPage, query)
            );
            var valuesPage = output.valuesPage();
            List<FilterValue> data = valuesPage.data().stream().map(ObservabilityFilterDataPortAdapter::toCoreValue).toList();
            // The ES composite-aggregation paths report no exact total (cursor pagination), so fall back
            // to a lower bound derived from the current slice.
            long total = valuesPage.totalFilteredCount() != null
                ? valuesPage.totalFilteredCount()
                : (long) (page - 1) * perPage + data.size();
            return new FilterValuesPage(data, total);
        } catch (ValidationDomainException e) {
            // The platform analytics catalog doesn't list values for this filter yet (unknown name or
            // unsupported type on its side) — surface a coherent "not supported" 400 for the caller.
            throw UnsupportedObservabilityFilterException.valueListingNotSupported(filterName, "KEYWORD");
        }
    }

    @Override
    public List<ResolvedLabels> resolveLabels(List<ResolveRequest> requests) {
        var entries = requests
            .stream()
            .map(r -> new ResolveFilterLabelsUseCase.Entry(r.filterName(), r.ids()))
            .toList();
        var output = resolveFilterLabelsUseCase.execute(new ResolveFilterLabelsUseCase.Input(currentAuditInfo(), entries));
        return output
            .entries()
            .stream()
            .map(e -> new ResolvedLabels(e.filterName(), e.labels()))
            .toList();
    }

    /**
     * id-based filters carry the entity id in {@code FilterValue.id} and the display name in
     * {@code value}; the unified wire contract sends the id as the value and the name as the label.
     * Direct-value filters have a {@code null} id, so the raw value is the wire value with no label.
     */
    private static FilterValue toCoreValue(io.gravitee.apim.core.analytics_engine.model.FilterValue value) {
        return value.id() != null ? new FilterValue(value.id(), value.value()) : new FilterValue(value.value(), null);
    }

    private static AuditInfo currentAuditInfo() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        AuditActor actor;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails user) {
            actor = AuditActor.builder().userId(user.getUsername()).userSource(user.getSource()).userSourceId(user.getSourceId()).build();
        } else {
            String userId = (authentication != null && authentication.getPrincipal() != null)
                ? authentication.getPrincipal().toString()
                : "unknown";
            actor = AuditActor.builder().userId(userId).build();
        }
        return AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(actor)
            .build();
    }
}
