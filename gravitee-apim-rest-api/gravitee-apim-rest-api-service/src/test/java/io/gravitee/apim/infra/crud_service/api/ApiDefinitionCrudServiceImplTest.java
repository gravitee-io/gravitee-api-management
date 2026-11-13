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
package io.gravitee.apim.infra.crud_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDefinitionCrudServiceImplTest {

    private static final ZonedDateTime UPDATED_AT = Instant.parse("2026-11-20T10:00:00Z").atZone(ZoneOffset.UTC);
    private static final ZonedDateTime EXPECTED_UPDATED_AT = Instant.parse("2026-11-20T09:00:00Z").atZone(ZoneOffset.UTC);

    private final ApiRepository apiRepository = mock(ApiRepository.class);
    private final ApiDefinitionCrudServiceImpl service = new ApiDefinitionCrudServiceImpl(apiRepository);

    @BeforeEach
    void setUp() throws TechnicalException {
        when(apiRepository.updateDefinition(any(), any(), any(), any(), any())).thenReturn(true);
    }

    /**
     * The index is the reason this write exists in one call: the definition and the Guardians it names reach the
     * store together, so nothing can read an index the definition contradicts.
     */
    @Test
    void should_index_the_guardians_the_written_definition_names() throws TechnicalException {
        var api = runningGuardians("guardian-1", "guardian-2");

        boolean written = service.replaceDefinition(api, EXPECTED_UPDATED_AT);

        var guardianIds = ArgumentCaptor.forClass(Set.class);
        verify(apiRepository).updateDefinition(
            eq(api.getId()),
            any(),
            guardianIds.capture(),
            eq(Date.from(UPDATED_AT.toInstant())),
            eq(Date.from(EXPECTED_UPDATED_AT.toInstant()))
        );
        assertSoftly(softly -> {
            softly.assertThat(written).as("the write happened").isTrue();
            softly.assertThat(guardianIds.getValue()).as("indexed guardians").containsExactly("guardian-1", "guardian-2");
        });
    }

    @Test
    void should_index_no_guardian_for_a_definition_naming_none() throws TechnicalException {
        service.replaceDefinition(runningGuardians(), EXPECTED_UPDATED_AT);

        var guardianIds = ArgumentCaptor.forClass(Set.class);
        verify(apiRepository).updateDefinition(any(), any(), guardianIds.capture(), any(), any());
        assertThat(guardianIds.getValue()).as("indexed guardians").isEmpty();
    }

    /** A definition that cannot carry an internal flow runs no Guardian, and indexing one would be a lie. */
    @Test
    void should_index_no_guardian_for_a_definition_that_carries_no_internal_flow() throws TechnicalException {
        service.replaceDefinition(ApiFixtures.aProxyApiV2().toBuilder().updatedAt(UPDATED_AT).build(), EXPECTED_UPDATED_AT);

        var guardianIds = ArgumentCaptor.forClass(Set.class);
        verify(apiRepository).updateDefinition(any(), any(), guardianIds.capture(), any(), any());
        assertThat(guardianIds.getValue()).as("indexed guardians").isEmpty();
    }

    @Test
    void should_answer_that_nothing_was_written_when_the_api_moved() throws TechnicalException {
        when(apiRepository.updateDefinition(any(), any(), any(), any(), any())).thenReturn(false);

        assertThat(service.replaceDefinition(runningGuardians("guardian-1"), EXPECTED_UPDATED_AT)).as("the write happened").isFalse();
    }

    @Test
    void should_fail_loudly_when_the_store_fails() throws TechnicalException {
        when(apiRepository.updateDefinition(any(), any(), any(), any(), any())).thenThrow(new TechnicalException("boom"));

        assertThatThrownBy(() -> service.replaceDefinition(runningGuardians("guardian-1"), EXPECTED_UPDATED_AT))
            .as("a store failure")
            .isInstanceOf(TechnicalDomainException.class);
    }

    private static Api runningGuardians(String... guardianIds) {
        var api = ApiFixtures.aProxyApiV4().toBuilder().updatedAt(UPDATED_AT).build();
        api
            .getApiDefinitionHttpV4()
            .setInternalFlow(
                Flow.builder()
                    .request(
                        Arrays.stream(guardianIds)
                            .map(id -> GuardianSteps.of(id, id))
                            .toList()
                    )
                    .build()
            );
        return api;
    }
}
