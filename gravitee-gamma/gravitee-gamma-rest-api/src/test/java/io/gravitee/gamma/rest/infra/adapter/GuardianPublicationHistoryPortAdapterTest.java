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

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** Tested against a mocked query service: the use-case and resource tests substitute this adapter away. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianPublicationHistoryPortAdapterTest {

    private final EventQueryService eventQueryService = mock(EventQueryService.class);
    private final GuardianPublicationHistoryPortAdapter adapter = new GuardianPublicationHistoryPortAdapter(eventQueryService);

    @Test
    void should_search_the_publications_of_the_generic_event_in_its_environment_and_page() {
        var publication = Event.builder().id("event-1").build();
        when(
            eventQueryService.search(
                argThat(
                    query ->
                        query.environmentId().equals("env-id") &&
                        query.types().equals(List.of(EventType.PUBLISH_GENERIC_EVENT)) &&
                        query.properties().equals(Map.of(Event.EventProperties.GENERIC_EVENT_ID, "generic-event-id")) &&
                        query.apiId().isEmpty() &&
                        query.id().isEmpty()
                ),
                argThat((Pageable pageable) -> pageable.getPageNumber() == 2 && pageable.getPageSize() == 5)
            )
        ).thenReturn(new EventQueryService.SearchResponse(6, List.of(publication)));

        var page = adapter.publications("env-id", "generic-event-id", 2, 5);

        assertSoftly(softly -> {
            softly.assertThat(page.getContent()).as("publications").containsExactly(publication);
            softly.assertThat(page.getPageNumber()).as("page").isEqualTo(2);
            softly.assertThat(page.getTotalElements()).as("total").isEqualTo(6);
        });
    }

    @Test
    void should_answer_an_empty_page_when_nothing_matches() {
        when(eventQueryService.search(any(), any())).thenReturn(new EventQueryService.SearchResponse(0, List.of()));

        var page = adapter.publications("env-id", "generic-event-id", 1, 10);

        assertSoftly(softly -> {
            softly.assertThat(page.getContent()).as("publications").isEmpty();
            softly.assertThat(page.getTotalElements()).as("total").isZero();
        });
    }
}
