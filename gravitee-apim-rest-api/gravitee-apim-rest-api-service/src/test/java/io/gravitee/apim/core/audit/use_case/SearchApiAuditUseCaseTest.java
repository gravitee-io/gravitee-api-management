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
package io.gravitee.apim.core.audit.use_case;

import fixtures.core.model.AuditFixtures;
import inmemory.AuditMetadataQueryServiceInMemory;
import inmemory.AuditQueryServiceInMemory;
import io.gravitee.apim.core.api.model.ApiAuditQueryFilters;
import io.gravitee.apim.core.audit.domain_service.SearchAuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchApiAuditUseCaseTest {

    public static final String API_ID = "api-id";
    public static final String ORGANIZATION_ID = "organization-id";
    public static final String ENVIRONMENT_ID = "environment-id";
    AuditQueryServiceInMemory auditQueryService = new AuditQueryServiceInMemory();
    AuditMetadataQueryServiceInMemory auditMetadataQueryService = new AuditMetadataQueryServiceInMemory();
    SearchAuditDomainService searchAuditDomainService = new SearchAuditDomainService(auditQueryService, auditMetadataQueryService);

    SearchApiAuditUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchApiAuditUseCase(searchAuditDomainService);
    }

    @AfterEach
    void tearDown() {
        auditQueryService.reset();
    }

    @Test
    void should_return_audits_with_metadata_of_the_requested_api() {
        // Given
        var expected = AuditFixtures.anApiAudit();
        auditQueryService.initWith(
            List.of(
                expected,
                AuditFixtures.anApiAudit().toBuilder().id("audit2").referenceId("other-api").build(),
                AuditFixtures.anApiAudit().toBuilder().id("audit3").environmentId("env2").build(),
                AuditFixtures.anApiAudit().toBuilder().id("audit4").organizationId("org2").build()
            )
        );

        var result = useCase.execute(
            new SearchApiAuditUseCase.Input(
                new ApiAuditQueryFilters(
                    API_ID,
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    Optional.of(0L),
                    Optional.of(Instant.now().toEpochMilli()),
                    Set.of()
                )
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft.assertThat(result.data()).containsExactly(expected);
            soft
                .assertThat(result.metadata())
                .containsExactlyInAnyOrderEntriesOf(Map.of("USER:user-id:name", "user-id-name", "API:api-id:name", "api-id-name"));
        });
    }

    @Test
    void should_return_audits_sorted_by_desc_createdAt() {
        // Given
        auditQueryService.initWith(
            List.of(
                AuditFixtures.anApiAudit().toBuilder().id("1").createdAt(ZonedDateTime.parse("2020-02-01T20:22:02.00Z")).build(),
                AuditFixtures.anApiAudit().toBuilder().id("2").createdAt(ZonedDateTime.parse("2020-02-02T20:22:02.00Z")).build(),
                AuditFixtures.anApiAudit().toBuilder().id("3").createdAt(ZonedDateTime.parse("2020-02-03T20:22:02.00Z")).build()
            )
        );

        var result = useCase.execute(
            new SearchApiAuditUseCase.Input(
                new ApiAuditQueryFilters(
                    API_ID,
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    Optional.of(0L),
                    Optional.of(Instant.now().toEpochMilli()),
                    Set.of()
                )
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(3);
            soft.assertThat(result.data()).extracting(AuditEntity::getId).containsExactly("3", "2", "1");
        });
    }

    @Test
    void should_return_the_page_requested() {
        // Given
        var expectedTotal = 15;
        var pageNumber = 2;
        var pageSize = 5;
        auditQueryService.initWith(
            IntStream.range(0, expectedTotal)
                .mapToObj(i -> AuditFixtures.anApiAudit().toBuilder().id(String.valueOf(i)).build())
                .toList()
        );

        var result = useCase.execute(
            new SearchApiAuditUseCase.Input(
                new ApiAuditQueryFilters(
                    API_ID,
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    Optional.of(0L),
                    Optional.of(Instant.now().toEpochMilli()),
                    Set.of()
                ),
                new PageableImpl(pageNumber, pageSize)
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(expectedTotal);
            soft.assertThat(result.data()).extracting(AuditEntity::getId).containsExactly("5", "6", "7", "8", "9");
        });
    }
}
