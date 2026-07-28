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
import io.gravitee.apim.core.api_product.model.ApiProductAuditQueryFilters;
import io.gravitee.apim.core.audit.domain_service.SearchAuditDomainService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchApiProductAuditUseCaseTest {

    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";

    AuditQueryServiceInMemory auditQueryService = new AuditQueryServiceInMemory();
    AuditMetadataQueryServiceInMemory auditMetadataQueryService = new AuditMetadataQueryServiceInMemory();
    SearchAuditDomainService searchAuditDomainService = new SearchAuditDomainService(auditQueryService, auditMetadataQueryService);

    SearchApiProductAuditUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchApiProductAuditUseCase(searchAuditDomainService);
    }

    @AfterEach
    void tearDown() {
        auditQueryService.reset();
    }

    @Test
    void should_return_only_the_audits_of_the_requested_api_product() {
        var expected = AuditFixtures.anApiProductAudit();
        auditQueryService.initWith(
            List.of(
                expected,
                AuditFixtures.anApiProductAudit().toBuilder().id("audit2").referenceId("other-product").build(),
                AuditFixtures.anApiProductAudit().toBuilder().id("audit3").environmentId("env2").build()
            )
        );

        var result = useCase.execute(new SearchApiProductAuditUseCase.Input(filters()));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft.assertThat(result.data()).containsExactly(expected);
        });
    }

    @Test
    void should_key_the_reference_metadata_by_its_own_type_not_as_an_api() {
        auditQueryService.initWith(List.of(AuditFixtures.anApiProductAudit()));

        var result = useCase.execute(new SearchApiProductAuditUseCase.Input(filters()));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.metadata()).containsKey("API_PRODUCT:" + API_PRODUCT_ID + ":name");
            soft.assertThat(result.metadata()).doesNotContainKey("API:" + API_PRODUCT_ID + ":name");
        });
    }

    private ApiProductAuditQueryFilters filters() {
        return new ApiProductAuditQueryFilters(
            API_PRODUCT_ID,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            Optional.of(0L),
            Optional.of(Instant.now().toEpochMilli()),
            Set.of()
        );
    }
}
