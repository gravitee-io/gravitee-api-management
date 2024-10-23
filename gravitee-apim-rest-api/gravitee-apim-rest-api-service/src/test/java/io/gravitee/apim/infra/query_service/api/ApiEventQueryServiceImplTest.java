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
package io.gravitee.apim.infra.query_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiEventQueryService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiEventQueryServiceImplTest {

    @Mock
    EventLatestRepository eventLatestRepository;

    @Captor
    ArgumentCaptor<EventCriteria> eventCriteriaCaptor;

    ApiEventQueryService cut;

    @BeforeEach
    void setUp() {
        cut = new ApiEventQueryServiceImpl(eventLatestRepository);
    }

    @Test
    void search_should_return_empty_if_no_event() {
        when(eventLatestRepository.search(any(), eq(Event.EventProperties.API_ID), eq(0L), eq(1L))).thenReturn(List.of());
        final Optional<Api> lastPublishedApi = cut.findLastPublishedApi("org-id", "env-id", "api-id");
        assertThat(lastPublishedApi).isEmpty();
        verifyEventCriteria();
    }

    @SneakyThrows
    @Test
    void search_should_return_last_published_api() {
        final Api api = ApiFixtures.aProxyApiV4();

        final Event event = new Event(
            "event-id",
            Set.of("env-id"),
            Set.of("org-id"),
            EventType.PUBLISH_API,
            GraviteeJacksonMapper.getInstance().writeValueAsString(ApiAdapter.INSTANCE.toRepository(api)),
            null,
            Map.of(),
            new Date(),
            new Date()
        );
        when(eventLatestRepository.search(any(), eq(Event.EventProperties.API_ID), eq(0L), eq(1L))).thenReturn(List.of(event));
        final Optional<Api> lastPublishedApi = cut.findLastPublishedApi("org-id", "env-id", "api-id");
        assertThat(lastPublishedApi)
            .hasValueSatisfying(result -> {
                assertThat(result.getApiDefinitionHttpV4()).isEqualTo(api.getApiDefinitionHttpV4());
            });
        verifyEventCriteria();
    }

    private void verifyEventCriteria() {
        verify(eventLatestRepository).search(eventCriteriaCaptor.capture(), eq(Event.EventProperties.API_ID), eq(0L), eq(1L));
        final EventCriteria builtEventCriteria = eventCriteriaCaptor.getValue();
        assertThat(builtEventCriteria)
            .satisfies(eventCriteria -> {
                assertThat(eventCriteria.getEnvironments()).containsExactly("env-id");
                assertThat(eventCriteria.getTypes()).containsExactly(EventType.PUBLISH_API);
                assertThat(eventCriteria.getProperties())
                    .hasSize(1)
                    .containsExactly(Map.entry(Event.EventProperties.API_ID.getValue(), "api-id"));
            });
    }
}
