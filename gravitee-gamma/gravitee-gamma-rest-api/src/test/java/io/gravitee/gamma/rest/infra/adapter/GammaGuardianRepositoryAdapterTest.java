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

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianDefinition;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardian.model.GuardianSearchQuery;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

/**
 * The adapter is where the translations the core must not know about live, and the one layer the
 * resource and use-case tests substitute away, so it is tested here against a mocked SPI.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GammaGuardianRepositoryAdapterTest {

    private final io.gravitee.repository.management.api.GuardianRepository spi = mock(
        io.gravitee.repository.management.api.GuardianRepository.class
    );
    private final GammaGuardianRepositoryAdapter adapter = new GammaGuardianRepositoryAdapter(spi);

    private static io.gravitee.gamma.rest.core.guardian.model.Guardian aGuardianWithId(String id) {
        return aGuardian().definition(aGuardianDefinition().id(id).build()).build();
    }

    /** A stored row carrying the fixture's name, as the name lookup would return it. */
    private static io.gravitee.repository.management.model.Guardian aStoredRowNamedLikeTheFixture(String id) {
        var row = new io.gravitee.repository.management.model.Guardian();
        row.setId(id);
        row.setEnvironmentId(ENVIRONMENT_ID);
        row.setName("Credential leakage");
        row.setDefinition(
            "{\"id\":\"%s\",\"environmentId\":\"%s\",\"name\":\"Credential leakage\",\"phase\":\"REQUEST\",\"apiTypes\":[\"llm-proxy\"]}".formatted(
                id,
                ENVIRONMENT_ID
            )
        );
        return row;
    }

    @Nested
    class Create {

        @Test
        void should_refuse_a_clashing_id_rather_than_failing_technically() throws Exception {
            // Mongo throws Spring's unchecked exception straight out of insert()
            when(spi.create(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

            assertThatThrownBy(() -> adapter.create(aGuardianWithId("taken")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("taken");
        }

        @Test
        void should_refuse_a_clashing_id_reported_through_the_checked_spi_exception() throws Exception {
            // JDBC surfaces the same clash wrapped in the SPI's checked exception
            when(spi.create(any())).thenThrow(new TechnicalException("insert failed", new DuplicateKeyException("pk_guardians")));

            assertThatThrownBy(() -> adapter.create(aGuardianWithId("taken")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("taken");
        }

        /**
         * Two unique keys can refuse the write and the store does not say which, so the name decides.
         * Another Guardian holding it means the name, which is a 409 and not the id's 400.
         */
        @Test
        void should_refuse_a_clashing_name_as_a_conflict_rather_than_a_taken_id() throws Exception {
            when(spi.create(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(ENVIRONMENT_ID, "Credential leakage")).thenReturn(
                List.of(aStoredRowNamedLikeTheFixture("another-guardian"))
            );

            assertThatThrownBy(() -> adapter.create(aGuardianWithId("taken")))
                .isInstanceOf(GuardianNameAlreadyExistsException.class)
                .hasMessageContaining("Credential leakage");
        }

        /** The row holding the name is the one being written, so the id is the only explanation left. */
        @Test
        void should_report_a_taken_id_when_the_only_name_match_is_the_guardian_itself() throws Exception {
            when(spi.create(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(ENVIRONMENT_ID, "Credential leakage")).thenReturn(
                List.of(aStoredRowNamedLikeTheFixture("taken"))
            );

            assertThatThrownBy(() -> adapter.create(aGuardianWithId("taken")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("taken");
        }

        /**
         * The name lookup is a second guess at a failure already in hand. Letting its own failure out
         * would turn a 400 into a 500, so the refusal the caller deserves survives it.
         */
        @Test
        void should_keep_the_refusal_when_the_name_lookup_itself_fails() throws Exception {
            when(spi.create(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(any(), any())).thenThrow(new TechnicalException("connection reset"));

            assertThatThrownBy(() -> adapter.create(aGuardianWithId("taken")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("taken");
        }

        @Test
        void should_still_report_a_genuine_technical_failure_as_technical() throws Exception {
            // the guard must not swallow an unrelated failure into a 400
            when(spi.create(any())).thenThrow(new TechnicalException("connection reset"));

            assertThatThrownBy(() -> adapter.create(aGuardian().build())).isInstanceOf(TechnicalDomainException.class);
        }
    }

    /**
     * A rename can break the unique name as readily as a creation can, so the update path translates a
     * refused write too. Unlike a creation it cannot clash on the id, which the path fixes.
     */
    @Nested
    class Update {

        @Test
        void should_refuse_a_clashing_name_as_a_conflict() throws Exception {
            when(spi.update(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(ENVIRONMENT_ID, "Credential leakage")).thenReturn(
                List.of(aStoredRowNamedLikeTheFixture("another-guardian"))
            );

            assertThatThrownBy(() -> adapter.update(aGuardianWithId(GUARDIAN_ID)))
                .isInstanceOf(GuardianNameAlreadyExistsException.class)
                .hasMessageContaining("Credential leakage");
        }

        /** No other Guardian holds the name, so this is not a name clash and must not read as one. */
        @Test
        void should_rethrow_a_duplicate_key_that_is_not_a_name_clash() throws Exception {
            when(spi.update(any())).thenThrow(new TechnicalException("update failed", new DuplicateKeyException("pk_guardians")));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(any(), any())).thenReturn(List.of());

            assertThatThrownBy(() -> adapter.update(aGuardianWithId(GUARDIAN_ID))).isInstanceOf(TechnicalDomainException.class);
        }

        @Test
        void should_still_report_a_genuine_technical_failure_as_technical() throws Exception {
            when(spi.update(any())).thenThrow(new TechnicalException("connection reset"));

            assertThatThrownBy(() -> adapter.update(aGuardianWithId(GUARDIAN_ID))).isInstanceOf(TechnicalDomainException.class);
        }
    }

    @Nested
    class Mapping {

        @Test
        void should_write_the_definition_into_its_column_and_read_the_same_one_back() throws Exception {
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Guardian.class);
            when(spi.create(captor.capture())).thenAnswer(call -> call.getArgument(0));

            var guardian = aGuardian().build();
            var created = adapter.create(guardian);

            assertThat(captor.getValue().getDefinition()).contains("\"name\":\"Credential leakage\"").contains("llm-proxy");
            assertThat(created.getDefinition()).isEqualTo(guardian.getDefinition());
            assertThat(created.getPolicies()).containsExactly(aPolicy());
            assertThat(created.getApiTypes()).containsExactly(ApiType.LLM_PROXY);
        }

        @Test
        void should_project_the_fields_the_search_needs_onto_their_own_columns() throws Exception {
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Guardian.class);
            when(spi.create(captor.capture())).thenAnswer(call -> call.getArgument(0));

            adapter.create(aGuardian().definition(aGuardianDefinition().phase(Phase.RESPONSE).build()).build());

            var row = captor.getValue();
            assertThat(row.getId()).isEqualTo(GUARDIAN_ID);
            assertThat(row.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
            assertThat(row.getName()).isEqualTo("Credential leakage");
            // stored as the constant name, whatever the wire spelling
            assertThat(row.getPhase().name()).isEqualTo("RESPONSE");
        }

        @Test
        void should_read_the_definition_rather_than_the_projected_columns() throws Exception {
            // the columns are a projection written on every save, so a row whose columns disagree with
            // its definition is a bug upstream; the definition is what the console gets either way
            var row = new io.gravitee.repository.management.model.Guardian();
            row.setId(GUARDIAN_ID);
            row.setEnvironmentId(ENVIRONMENT_ID);
            row.setName("stale column");
            row.setDefinition(
                "{\"id\":\"%s\",\"environmentId\":\"%s\",\"name\":\"the real name\",\"phase\":\"REQUEST\",\"apiTypes\":[\"llm-proxy\"]}".formatted(
                    GUARDIAN_ID,
                    ENVIRONMENT_ID
                )
            );
            when(spi.findById(GUARDIAN_ID)).thenReturn(Optional.of(row));

            var found = adapter.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow();

            assertThat(found.getName()).isEqualTo("the real name");
            assertThat(found.getApiTypes()).isEqualTo(Set.of(ApiType.LLM_PROXY));
        }

        @Test
        void should_fail_loudly_on_a_row_with_no_stored_definition() throws Exception {
            var row = new io.gravitee.repository.management.model.Guardian();
            row.setId(GUARDIAN_ID);
            row.setEnvironmentId(ENVIRONMENT_ID);
            when(spi.findById(GUARDIAN_ID)).thenReturn(Optional.of(row));

            assertThatThrownBy(() -> adapter.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessageContaining(GUARDIAN_ID);
        }

        @Test
        void should_carry_the_timestamps_across_the_date_boundary() throws Exception {
            when(spi.create(any())).thenAnswer(call -> call.getArgument(0));

            var guardian = aGuardian().build();
            var created = adapter.create(guardian);

            assertThat(created.getCreatedAt()).isEqualTo(guardian.getCreatedAt());
            assertThat(created.getUpdatedAt()).isEqualTo(guardian.getUpdatedAt());
        }
    }

    @Nested
    class FindByEnvironmentId {

        @Test
        void should_answer_empty_for_a_guardian_of_another_environment() throws Exception {
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Guardian.class);
            when(spi.create(captor.capture())).thenAnswer(call -> call.getArgument(0));
            adapter.create(aGuardian().build());
            when(spi.findById(GUARDIAN_ID)).thenReturn(Optional.of(captor.getValue()));

            assertThat(adapter.findByEnvironmentId("other-env", GUARDIAN_ID)).isEmpty();
        }

        @Test
        void should_answer_empty_for_an_unknown_id() throws Exception {
            when(spi.findById("nope")).thenReturn(Optional.empty());

            assertThat(adapter.findByEnvironmentId(ENVIRONMENT_ID, "nope")).isEmpty();
        }
    }

    @Nested
    class Search {

        @Test
        void should_count_pages_from_one_for_the_core_and_from_zero_for_the_spi() throws Exception {
            var pageable = ArgumentCaptor.forClass(Pageable.class);
            when(spi.search(any(), pageable.capture(), any())).thenReturn(new Page<>(List.of(), 1, 0, 0));

            var page = adapter.search(new GuardianSearchQuery("env-1", null, 3, 20, "-updatedAt"));

            assertThat(pageable.getValue().pageNumber()).isEqualTo(2);
            assertThat(pageable.getValue().pageSize()).isEqualTo(20);
            assertThat(page.getPageNumber()).isEqualTo(3);
        }

        @Test
        void should_let_the_repository_apply_its_default_sort_when_none_is_given() throws Exception {
            when(spi.search(any(), any(), any())).thenReturn(new Page<>(List.of(), 1, 0, 0));

            adapter.search(new GuardianSearchQuery("env-1", null, 1, 10, null));

            verify(spi).search(any(), any(), org.mockito.ArgumentMatchers.isNull());
        }
    }
}
