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
import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
    private final AiWorkspaceAnalyticsApisDomainService workspaceApis;

    public record Entry(String filterName, List<String> ids) {}

    public record ResolvedEntry(String filterName, Map<String, String> labels) {}

    /**
     * @param apiProductId the single API product the labelled request is scoped to, or {@code null}. A reader of that AI
     *                     workspace also gets the labels of the workspace, its APIs and the ids found in its calls: what
     *                     its aggregates show them.
     */
    public record Input(AuditInfo auditInfo, List<Entry> entries, String apiProductId) {
        public Input(AuditInfo auditInfo, List<Entry> entries) {
            this(auditInfo, entries, null);
        }
    }

    public record Output(List<ResolvedEntry> entries) {}

    public Output execute(Input input) {
        var entries = input.entries() == null ? List.<Entry>of() : input.entries();
        validate(entries);

        var context = entries.stream().anyMatch(ResolveFilterLabelsUseCase::isResolvable)
            ? contextLoader.load(input.auditInfo(), AnalyticsScope.MANAGEMENT, ApiDataKind.AGGREGATES)
            : null;
        var workspace = context == null ? Workspace.NONE : workspaceOf(context, input.apiProductId());
        var resolved = entries
            .stream()
            .map(entry -> {
                if (!isResolvable(entry)) {
                    log.warn("Ignoring unsupported observability filter label resolution request for filter [{}]", entry.filterName());
                    return new ResolvedEntry(entry.filterName(), Map.of());
                }
                var ids = entry.ids() == null ? Set.<String>of() : Set.copyOf(entry.ids());
                return new ResolvedEntry(entry.filterName(), labels(context, workspace, parseFilterName(entry.filterName()), ids));
            })
            .toList();

        return new Output(resolved);
    }

    /**
     * Labels only what the caller's rows hold: an API they read, or an id found in the calls of those APIs; and, for
     * the workspace the request is scoped to, an id found in the calls made through it.
     */
    private Map<String, String> labels(AnalyticsQueryContext context, Workspace workspace, FilterSpec.Name filterName, Set<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        var auditInfo = context.auditInfo();
        if (filterName == FilterSpec.Name.API) {
            var apiNamesById = new HashMap<>(context.apiNamesById());
            var workspaceApiIds = ids.stream().filter(workspace.apiIds()::contains).collect(Collectors.toSet());
            if (!workspaceApiIds.isEmpty()) {
                filterValuesQueryService
                    .findValuesInCalls(
                        auditInfo.organizationId(),
                        auditInfo.environmentId(),
                        filterName,
                        null,
                        null,
                        workspaceApiIds,
                        workspace.apiIds(),
                        workspace.id()
                    )
                    .forEach(id -> {
                        var name = workspace.apiNamesById().get(id);
                        if (name != null) {
                            apiNamesById.putIfAbsent(id, name);
                        }
                    });
            }
            return restrict(apiNamesById, ids);
        }
        var inCalls = new HashSet<>(
            filterValuesQueryService.findValuesInCalls(
                auditInfo.organizationId(),
                auditInfo.environmentId(),
                filterName,
                null,
                null,
                ids,
                context.authorizedApiIds()
            )
        );
        if (!workspace.apiIds().isEmpty()) {
            inCalls.addAll(
                filterValuesQueryService.findValuesInCalls(
                    auditInfo.organizationId(),
                    auditInfo.environmentId(),
                    filterName,
                    null,
                    null,
                    ids,
                    workspace.apiIds(),
                    workspace.id()
                )
            );
        }
        if (inCalls.isEmpty()) {
            return Map.of();
        }
        return restrict(filterValueNameResolver.resolveNames(auditInfo.environmentId(), filterName, List.copyOf(inCalls)), inCalls);
    }

    /** Empty unless the caller reads the workspace: its APIs are then counted through its calls only. */
    private Workspace workspaceOf(AnalyticsQueryContext context, String apiProductId) {
        if (apiProductId == null) {
            return Workspace.NONE;
        }
        var apis = workspaceApis.findWorkspaceApis(context.executionContext(), context.auditInfo().actor().userId(), apiProductId);
        var apiNamesById = new HashMap<String, String>();
        apis
            .stream()
            .filter(api -> api.getName() != null)
            .forEach(api -> apiNamesById.put(api.getId(), api.getName()));
        return new Workspace(apiProductId, apis.stream().map(Api::getId).collect(Collectors.toSet()), apiNamesById);
    }

    private record Workspace(String id, Set<String> apiIds, Map<String, String> apiNamesById) {
        static final Workspace NONE = new Workspace(null, Set.of(), Map.of());
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
