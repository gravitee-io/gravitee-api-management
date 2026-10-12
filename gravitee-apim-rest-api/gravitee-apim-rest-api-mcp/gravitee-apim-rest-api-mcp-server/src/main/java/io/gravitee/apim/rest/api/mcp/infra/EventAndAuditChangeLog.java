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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.core.api.model.ApiAuditQueryFilters;
import io.gravitee.apim.core.audit.domain_service.SearchAuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.use_case.SearchApiAuditUseCase;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.ChangeLog;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class EventAndAuditChangeLog implements ChangeLog {

    private static final int MAX_CHANGES = 50;
    private static final List<EventType> DEPLOYMENTS = List.of(
        EventType.PUBLISH_API,
        EventType.UNPUBLISH_API,
        EventType.START_API,
        EventType.STOP_API
    );

    private final EventQueryService events;
    private final SearchApiAuditUseCase searchApiAudit;

    public EventAndAuditChangeLog(EventQueryService events, SearchApiAuditUseCase searchApiAudit) {
        this.events = events;
        this.searchApiAudit = searchApiAudit;
    }

    @Override
    public List<Change> deployments(AuditInfo caller, String apiId, TimeWindow window) {
        var query = new EventQueryService.SearchQuery(
            caller.environmentId(),
            Optional.empty(),
            Optional.of(apiId),
            DEPLOYMENTS,
            Map.of(),
            Optional.of(window.from().toEpochMilli()),
            Optional.of(window.to().toEpochMilli())
        );
        return ApimContext.run(caller, () -> events.search(query, new PageableImpl(1, MAX_CHANGES)))
            .events()
            .stream()
            .map(EventAndAuditChangeLog::toChange)
            .toList();
    }

    @Override
    public List<Change> audit(AuditInfo caller, String apiId, TimeWindow window) {
        var filters = new ApiAuditQueryFilters(
            apiId,
            caller.organizationId(),
            caller.environmentId(),
            Optional.of(window.from().toEpochMilli()),
            Optional.of(window.to().toEpochMilli()),
            Set.of()
        );
        var output = ApimContext.run(caller, () ->
            searchApiAudit.execute(new SearchApiAuditUseCase.Input(filters, new PageableImpl(1, MAX_CHANGES)))
        );
        return output
            .data()
            .stream()
            .map(entry -> toChange(entry, output.metadata()))
            .toList();
    }

    private static Change toChange(Event event) {
        var details = new TreeMap<String, String>();
        copy(event, Event.EventProperties.DEPLOYMENT_NUMBER, details);
        copy(event, Event.EventProperties.DEPLOYMENT_LABEL, details);
        return new Change(
            event.getCreatedAt().toInstant(),
            Change.Source.DEPLOYMENT,
            event.getType().name(),
            event.getProperties().get(Event.EventProperties.USER),
            details
        );
    }

    private static void copy(Event event, Event.EventProperties property, Map<String, String> details) {
        var value = event.getProperties().get(property);
        if (value != null) {
            details.put(property.getLabel(), value);
        }
    }

    /** Audit properties hold ids; the names come from the metadata, where the domain service resolved them. */
    private static Change toChange(AuditEntity entry, Map<String, String> names) {
        var details = new TreeMap<String, String>();
        if (entry.getProperties() != null) {
            entry
                .getProperties()
                .forEach((type, id) -> details.put(type, names.getOrDefault(SearchAuditDomainService.nameMetadataKey(type, id), id)));
        }
        var actor = names.getOrDefault(SearchAuditDomainService.nameMetadataKey("USER", entry.getUser()), entry.getUser());
        return new Change(entry.getCreatedAt().toInstant(), Change.Source.AUDIT, entry.getEvent(), actor, details);
    }
}
