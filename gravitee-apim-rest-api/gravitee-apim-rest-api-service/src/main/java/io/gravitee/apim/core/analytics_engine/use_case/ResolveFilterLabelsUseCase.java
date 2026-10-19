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
package io.gravitee.apim.core.analytics_engine.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class ResolveFilterLabelsUseCase {

    private static final int MAX_ENTRIES = 10;
    private static final int MAX_IDS_PER_ENTRY = 100;

    private static final Set<FilterSpec.Name> RESOLVABLE_FILTERS = Set.of(
        FilterSpec.Name.API,
        FilterSpec.Name.APPLICATION,
        FilterSpec.Name.PLAN,
        FilterSpec.Name.API_PRODUCT
    );

    private final FilterValueNameResolver filterValueNameResolver;
    private final AnalyticsQueryContextLoaderResolver contextLoader;
    private final FilterValuesQueryService filterValuesQueryService;

    public record Entry(String filterName, List<String> ids) {}

    public record ResolvedEntry(String filterName, Map<String, String> labels) {}

    public record Input(AuditInfo auditInfo, List<Entry> entries) {}

    public record Output(List<ResolvedEntry> entries) {}

    public Output execute(Input input) {
        var entries = input.entries() == null ? List.<Entry>of() : input.entries();
        validate(entries);

        var context = entries.stream().anyMatch(ResolveFilterLabelsUseCase::isResolvable)
            ? contextLoader.load(input.auditInfo(), AnalyticsScope.MANAGEMENT, ApiDataKind.AGGREGATES)
            : null;
        var resolved = entries
            .stream()
            .map(entry -> {
                if (!isResolvable(entry)) {
                    log.warn("Ignoring unsupported observability filter label resolution request for filter [{}]", entry.filterName());
                    return new ResolvedEntry(entry.filterName(), Map.of());
                }
                var ids = entry.ids() == null ? Set.<String>of() : Set.copyOf(entry.ids());
                return new ResolvedEntry(entry.filterName(), labels(context, parseFilterName(entry.filterName()), ids));
            })
            .toList();

        return new Output(resolved);
    }

    /** Labels only what the caller's rows hold: an API they read, or an id found in the calls of those APIs. */
    private Map<String, String> labels(AnalyticsQueryContext context, FilterSpec.Name filterName, Set<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        if (filterName == FilterSpec.Name.API) {
            return restrict(context.apiNamesById(), ids);
        }
        var auditInfo = context.auditInfo();
        var inCalls = filterValuesQueryService.findValuesInCalls(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            filterName,
            null,
            null,
            ids,
            context.authorizedApiIds()
        );
        if (inCalls.isEmpty()) {
            return Map.of();
        }
        return restrict(filterValueNameResolver.resolveNames(auditInfo.environmentId(), filterName, List.copyOf(inCalls)), inCalls);
    }

    private static Map<String, String> restrict(Map<String, String> labels, Set<String> ids) {
        return labels
            .entrySet()
            .stream()
            .filter(label -> ids.contains(label.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isResolvable(Entry entry) {
        var filterName = parseFilterName(entry.filterName());
        return filterName != null && RESOLVABLE_FILTERS.contains(filterName);
    }

    private static void validate(List<Entry> entries) {
        if (entries.size() > MAX_ENTRIES) {
            throw new ValidationDomainException(
                "Too many filter entries to resolve",
                Map.of("maxEntries", String.valueOf(MAX_ENTRIES), "entries", String.valueOf(entries.size()))
            );
        }

        entries.forEach(entry -> {
            var ids = entry.ids() == null ? List.<String>of() : entry.ids();
            if (ids.size() > MAX_IDS_PER_ENTRY) {
                throw new ValidationDomainException(
                    "Too many filter ids to resolve",
                    Map.of(
                        "filterName",
                        String.valueOf(entry.filterName()),
                        "maxIds",
                        String.valueOf(MAX_IDS_PER_ENTRY),
                        "ids",
                        String.valueOf(ids.size())
                    )
                );
            }
        });
    }

    private static FilterSpec.Name parseFilterName(String name) {
        if (name == null) {
            return null;
        }
        return Arrays.stream(FilterSpec.Name.values())
            .filter(filterName -> filterName.name().equals(name))
            .findFirst()
            .orElse(null);
    }
}
