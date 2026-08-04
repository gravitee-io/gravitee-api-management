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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRange;
import io.gravitee.gamma.rest.core.observability.dashboard.model.TimeRangeType;
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.repository.management.model.GammaDashboard;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Wraps the {@code GammaDashboardRepository} SPI (shipped by OBS-14, {@code gravitee-apim-repository-api})
 * for the core layer. Anticorruption mapping is non-trivial: the persisted {@code GammaDashboard.Filter}
 * uses {@code field}/{@code label}/{@code operator}(lowercase)/{@code value}/{@code editable}, not the
 * {@code name}/UPPERCASE-{@code operator}/{@code values} shape this endpoint's wire format uses via the
 * shared {@link FilterCondition}/{@link FilterOperator}.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GammaDashboardRepositoryAdapter implements DashboardRepository {

    private static final ObjectMapper WIDGETS_MAPPER = new ObjectMapper();

    private final io.gravitee.repository.management.api.GammaDashboardRepository gammaDashboardRepository;

    @Override
    public List<Dashboard> findByEnvironmentId(String environmentId) {
        return RepositoryCalls.wrap(
            () ->
                gammaDashboardRepository.findByEnvironmentId(environmentId).stream().map(GammaDashboardRepositoryAdapter::toCore).toList(),
            "Failed to list dashboards for environment '" + environmentId + "'"
        );
    }

    @Override
    public Optional<Dashboard> findByIdAndEnvironmentId(String id, String environmentId) {
        return RepositoryCalls.wrap(
            () -> gammaDashboardRepository.findByIdAndEnvironmentId(id, environmentId).map(GammaDashboardRepositoryAdapter::toCore),
            "Failed to fetch dashboard '" + id + "' for environment '" + environmentId + "'"
        );
    }

    private static Dashboard toCore(GammaDashboard source) {
        return new Dashboard(
            source.getId(),
            source.getEnvironmentId(),
            source.getTitle(),
            source.getDescription(),
            source.getFilters() == null ? List.of() : source.getFilters().stream().map(GammaDashboardRepositoryAdapter::toCore).toList(),
            toCore(source.getTimeRange()),
            parseWidgets(source.getWidgets()),
            source.getVersion(),
            source.getCreatedBy(),
            source.getCreatedAt() == null ? null : source.getCreatedAt().toInstant(),
            source.getUpdatedAt() == null ? null : source.getUpdatedAt().toInstant()
        );
    }

    private static DashboardFilter toCore(GammaDashboard.Filter source) {
        List<String> values = source.getValue() == null ? List.of() : List.copyOf(source.getValue());
        return new DashboardFilter(
            new FilterCondition(source.getField(), toOperator(source.getField(), source.getOperator()), values),
            source.getLabel(),
            source.isEditable()
        );
    }

    private static FilterOperator toOperator(String field, String operator) {
        try {
            return FilterOperator.valueOf(operator.toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new TechnicalDomainException(
                "Persisted dashboard filter '" + field + "' has an unsupported operator '" + operator + "'",
                e
            );
        }
    }

    private static TimeRange toCore(GammaDashboard.TimeRange source) {
        if (source == null) {
            return null;
        }
        return new TimeRange(toTimeRangeType(source.getType()), source.getPeriod(), source.getFrom(), source.getTo());
    }

    private static TimeRangeType toTimeRangeType(String type) {
        try {
            return TimeRangeType.valueOf(type.toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new TechnicalDomainException("Persisted dashboard has an unsupported time range type '" + type + "'", e);
        }
    }

    private static JsonNode parseWidgets(String widgets) {
        if (widgets == null || widgets.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            return WIDGETS_MAPPER.readTree(widgets);
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Persisted dashboard widgets payload is not valid JSON", e);
        }
    }
}
