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
package io.gravitee.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Visibility;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The definition of an API and the index of the Guardrails it names are written together or not at all: a definition
 * written without its index would let a Guardrail the API runs be undeployed or deleted.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class JdbcApiRepositoryDefinitionTransactionTest extends AbstractRepositoryTest {

    private static final String API_ID = "api-whose-index-write-fails";
    private static final String GUARDRAIL_ID_TOO_LONG_FOR_ITS_COLUMN = "g".repeat(65);

    @Inject
    private ApiRepository apiRepository;

    @Test
    public void should_write_neither_the_definition_nor_the_index_when_the_index_write_fails() throws TechnicalException {
        Date updatedAt = new Date(1_700_000_000_000L);
        apiRepository.create(
            Api.builder()
                .id(API_ID)
                .environmentId("DEFAULT")
                .name("api whose index write fails")
                .version("1")
                .visibility(Visibility.PRIVATE)
                .definition("{\"before\":true}")
                .createdAt(updatedAt)
                .updatedAt(updatedAt)
                .build()
        );
        apiRepository.updateDefinition(API_ID, "{\"before\":true}", Set.of("guardrail-kept"), updatedAt, updatedAt);

        assertThatThrownBy(() ->
            apiRepository.updateDefinition(
                API_ID,
                "{\"after\":true}",
                Set.of(GUARDRAIL_ID_TOO_LONG_FOR_ITS_COLUMN),
                new Date(1_700_000_060_000L),
                updatedAt
            )
        ).isInstanceOf(TechnicalException.class);

        assertThat(apiRepository.findById(API_ID))
            .get()
            .satisfies(api ->
                assertSoftly(softly -> {
                    softly.assertThat(api.getDefinition()).as("definition").isEqualTo("{\"before\":true}");
                    softly.assertThat(api.getUpdatedAt()).as("update date").isEqualTo(updatedAt);
                    softly
                        .assertThat(
                            apiRepository.search(
                                new ApiCriteria.Builder().guardrail("guardrail-kept").build(),
                                ApiFieldFilter.defaultFields()
                            )
                        )
                        .extracting(Api::getId)
                        .as("APIs indexed as running guardrail-kept")
                        .containsExactly(API_ID);
                })
            );
    }

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Override
    protected String getModelPackage() {
        return "";
    }

    @Override
    protected void createModel(Object object) {}
}
