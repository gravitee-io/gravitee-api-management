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

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.use_case.SearchApiAuditUseCase;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.rest.api.model.EventType;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EventAndAuditChangeLogTest {

    private static final TimeWindow WINDOW = TimeWindow.parse("1h", null, NOW);

    private final EventQueryService events = mock(EventQueryService.class);
    private final SearchApiAuditUseCase searchApiAudit = mock(SearchApiAuditUseCase.class);
    private final EventAndAuditChangeLog changeLog = new EventAndAuditChangeLog(events, searchApiAudit);

    @Test
    void should_list_the_deployments_starts_and_stops_of_the_api() {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.USER, "admin");
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER, "7");
        when(events.search(any(), any())).thenReturn(
            new EventQueryService.SearchResponse(
                1,
                List.of(Event.builder().type(EventType.PUBLISH_API).createdAt(NOW.atZone(ZoneOffset.UTC)).properties(properties).build())
            )
        );

        var changes = changeLog.deployments(CALLER, "orders-id", WINDOW);

        assertThat(changes).containsExactly(
            new Change(NOW, Change.Source.DEPLOYMENT, "PUBLISH_API", "admin", Map.of("deployment_number", "7"))
        );
        var query = ArgumentCaptor.forClass(EventQueryService.SearchQuery.class);
        verify(events).search(query.capture(), any());
        assertThat(query.getValue().environmentId()).isEqualTo("e-prod");
        assertThat(query.getValue().apiId()).contains("orders-id");
        assertThat(query.getValue().types()).containsExactly(
            EventType.PUBLISH_API,
            EventType.UNPUBLISH_API,
            EventType.START_API,
            EventType.STOP_API
        );
        assertThat(query.getValue().from()).isEqualTo(Optional.of(WINDOW.from().toEpochMilli()));
        assertThat(query.getValue().to()).isEqualTo(Optional.of(NOW.toEpochMilli()));
    }

    @Test
    void should_list_the_audit_trail_with_names_in_place_of_ids() {
        var entry = AuditEntity.builder()
            .event("APIKEY_REVOKED")
            .user("user-id")
            .createdAt(NOW.atZone(ZoneOffset.UTC))
            .properties(Map.of("API_KEY", "key-id", "APPLICATION", "app-id"))
            .build();
        when(searchApiAudit.execute(any())).thenReturn(
            new SearchApiAuditUseCase.Output(
                List.of(entry),
                Map.of("USER:user-id:name", "Jane Admin", "APPLICATION:app-id:name", "Mobile app", "API_KEY:key-id:name", "key-id"),
                1
            )
        );

        var changes = changeLog.audit(CALLER, "orders-id", WINDOW);

        assertThat(changes).containsExactly(
            new Change(NOW, Change.Source.AUDIT, "APIKEY_REVOKED", "Jane Admin", Map.of("API_KEY", "key-id", "APPLICATION", "Mobile app"))
        );
        var input = ArgumentCaptor.forClass(SearchApiAuditUseCase.Input.class);
        verify(searchApiAudit).execute(input.capture());
        var filters = input.getValue().query();
        assertThat(filters.apiId()).isEqualTo("orders-id");
        assertThat(filters.environmentId()).isEqualTo("e-prod");
        assertThat(filters.from()).isEqualTo(Optional.of(WINDOW.from().toEpochMilli()));
        assertThat(filters.to()).isEqualTo(Optional.of(NOW.toEpochMilli()));
    }
}
