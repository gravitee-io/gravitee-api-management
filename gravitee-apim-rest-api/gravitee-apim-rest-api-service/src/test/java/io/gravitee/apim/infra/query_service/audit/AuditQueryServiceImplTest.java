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
package io.gravitee.apim.infra.query_service.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.ApiAuditQueryFilters;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditQueryServiceImplTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");

    AuditRepository auditRepository;

    AuditQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        auditRepository = mock(AuditRepository.class);

        service = new AuditQueryServiceImpl(auditRepository);
    }

    @Nested
    class SearchApiAudit {

        @Test
        @SneakyThrows
        void should_query_for_api_audits() {
            // Given
            when(auditRepository.search(any(), any())).thenReturn(new Page<>(List.of(), 0, 0, 0));

            var query = new ApiAuditQueryFilters(
                "api-id",
                "organization-id",
                "environment-id",
                Optional.of(2L),
                Optional.of(100L),
                Set.of("event-1", "event-2")
            );
            var pageable = new PageableImpl(0, 50);

            // When
            service.searchApiAudit(query, pageable);

            // Then
            var queryCaptor = ArgumentCaptor.forClass(AuditCriteria.class);
            var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(auditRepository).search(queryCaptor.capture(), pageableCaptor.capture());
            assertThat(queryCaptor.getValue()).satisfies(criteria -> {
                assertThat(criteria.getReferences()).containsExactly(Map.entry(Audit.AuditReferenceType.API, List.of("api-id")));
                assertThat(criteria.getOrganizationId()).isEqualTo("organization-id");
                assertThat(criteria.getEnvironmentIds()).isEqualTo(List.of("environment-id"));
                assertThat(criteria.getEvents()).containsExactlyInAnyOrderElementsOf(List.of("event-1", "event-2"));
                assertThat(criteria.getFrom()).isEqualTo(2L);
                assertThat(criteria.getTo()).isEqualTo(100L);
            });

            assertThat(pageableCaptor.getValue()).satisfies(pageParam -> {
                assertThat(pageParam.pageNumber()).isZero();
                assertThat(pageParam.pageSize()).isEqualTo(50);
            });
        }

        @Test
        @SneakyThrows
        void should_return_audits() {
            // Given
            when(auditRepository.search(any(), any())).thenAnswer(invocation -> {
                var criteria = invocation.getArgument(0, AuditCriteria.class);
                var list = criteria
                    .getEvents()
                    .stream()
                    .map(event ->
                        anAudit()
                            .organizationId(criteria.getOrganizationId())
                            .environmentId(criteria.getEnvironmentIds().get(0))
                            .event(event)
                            .build()
                    )
                    .toList();
                return new Page<>(list, 0, list.size(), list.size());
            });

            var query = new ApiAuditQueryFilters(
                "api-id",
                "organization-id",
                "environment-id",
                Optional.of(2L),
                Optional.of(100L),
                Set.of("event-1")
            );
            var pageable = new PageableImpl(0, 50);

            // When
            var result = service.searchApiAudit(query, pageable);

            // Then
            assertSoftly(softly -> {
                softly.assertThat(result.total()).isOne();
                softly
                    .assertThat(result.audits())
                    .containsExactly(
                        AuditEntity.builder()
                            .id("audit-id")
                            .organizationId("organization-id")
                            .environmentId("environment-id")
                            .referenceType(AuditEntity.AuditReferenceType.API)
                            .referenceId("api-id")
                            .user("user-id")
                            .properties(Map.of("API", "myapi"))
                            .event("event-1")
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .patch(
                                """
                                [{ "op": "add", "path": "/hello", "value": ["world"] }]"""
                            )
                            .build()
                    );
            });
        }

        private Audit.AuditBuilder anAudit() {
            return Audit.builder()
                .id("audit-id")
                .createdAt(Date.from(INSTANT_NOW))
                .organizationId("organization-id")
                .environmentId("environment-id")
                .referenceId("api-id")
                .referenceType(Audit.AuditReferenceType.API)
                .event(Api.AuditEvent.API_CREATED.name())
                .user("user-id")
                .properties(Map.of("API", "myapi"))
                .patch(
                    """
                    [{ "op": "add", "path": "/hello", "value": ["world"] }]"""
                );
        }
    }
}
