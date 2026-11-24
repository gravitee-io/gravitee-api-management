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

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailDefinition;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailSearchQuery;
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
class GammaGuardrailRepositoryAdapterTest {

    private final io.gravitee.repository.management.api.GuardrailRepository spi = mock(
        io.gravitee.repository.management.api.GuardrailRepository.class
    );
    private final GammaGuardrailRepositoryAdapter adapter = new GammaGuardrailRepositoryAdapter(spi);

    private static io.gravitee.gamma.rest.core.guardrail.model.Guardrail aGuardrailWithId(String id) {
        return aGuardrail().definition(aGuardrailDefinition().id(id).build()).build();
    }

    /** A stored row carrying the fixture's name, as the name lookup would return it. */
    private static io.gravitee.repository.management.model.Guardrail aStoredRowNamedLikeTheFixture(String id) {
        var row = new io.gravitee.repository.management.model.Guardrail();
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

    @Test
    void should_fetch_requested_guardrails_in_one_environment_scoped_call() throws Exception {
        var ids = Set.of("first", "second", "missing");
        when(spi.findByEnvironmentIdAndIds(ENVIRONMENT_ID, ids)).thenReturn(
            List.of(aStoredRowNamedLikeTheFixture("second"), aStoredRowNamedLikeTheFixture("first"))
        );

        var found = adapter.findByEnvironmentIdAndIds(ENVIRONMENT_ID, ids);

        assertThat(found).as("all returned Guardrails mapped").extracting(Guardrail::getId).containsExactly("second", "first");
        verify(spi).findByEnvironmentIdAndIds(ENVIRONMENT_ID, ids);
        verifyNoMoreInteractions(spi);
    }

    @Nested
    class Create {

        @Test
        void should_refuse_a_clashing_id_rather_than_failing_technically() throws Exception {
            // Mongo throws Spring's unchecked exception straight out of insert()
            when(spi.create(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

            assertThatThrownBy(() -> adapter.create(aGuardrailWithId("taken")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("taken");
        }

        @Test
        void should_refuse_a_clashing_id_reported_through_the_checked_spi_exception() throws Exception {
            // JDBC surfaces the same clash wrapped in the SPI's checked exception
            when(spi.create(any())).thenThrow(new TechnicalException("insert failed", new DuplicateKeyException("pk_guardrails")));

            assertThatThrownBy(() -> adapter.create(aGuardrailWithId("taken")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("taken");
        }

        /**
         * Two unique keys can refuse the write and the store does not say which, so the name decides.
         * Another Guardrail holding it means the name, which is a 409 and not the id's 400.
         */
        @Test
        void should_refuse_a_clashing_name_as_a_conflict_rather_than_a_taken_id() throws Exception {
            when(spi.create(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(ENVIRONMENT_ID, "Credential leakage")).thenReturn(
                List.of(aStoredRowNamedLikeTheFixture("another-guardrail"))
            );

            assertThatThrownBy(() -> adapter.create(aGuardrailWithId("taken")))
                .isInstanceOf(GuardrailNameAlreadyExistsException.class)
                .hasMessageContaining("Credential leakage");
        }

        /** The row holding the name is the one being written, so the id is the only explanation left. */
        @Test
        void should_report_a_taken_id_when_the_only_name_match_is_the_guardrail_itself() throws Exception {
            when(spi.create(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(ENVIRONMENT_ID, "Credential leakage")).thenReturn(
                List.of(aStoredRowNamedLikeTheFixture("taken"))
            );

            assertThatThrownBy(() -> adapter.create(aGuardrailWithId("taken")))
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

            assertThatThrownBy(() -> adapter.create(aGuardrailWithId("taken")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("taken");
        }

        @Test
        void should_still_report_a_genuine_technical_failure_as_technical() throws Exception {
            // the guard must not swallow an unrelated failure into a 400
            when(spi.create(any())).thenThrow(new TechnicalException("connection reset"));

            assertThatThrownBy(() -> adapter.create(aGuardrail().build())).isInstanceOf(TechnicalDomainException.class);
        }
    }

    /** The draft version travels with the row, and the store alone decides whether a conditional write goes through. */
    @Nested
    class The_version {

        @Test
        void should_be_read_from_the_stored_row() throws Exception {
            var row = aStoredRowNamedLikeTheFixture(GUARDRAIL_ID);
            row.setVersion(7);
            when(spi.findById(GUARDRAIL_ID)).thenReturn(Optional.of(row));

            assertThat(adapter.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID))
                .get()
                .extracting(io.gravitee.gamma.rest.core.guardrail.model.Guardrail::getVersion)
                .isEqualTo(7);
        }

        @Test
        void should_be_written_to_the_stored_row() throws Exception {
            when(spi.update(any())).thenAnswer(call -> call.getArgument(0));

            adapter.update(aGuardrailWithId(GUARDRAIL_ID).toBuilder().version(7).build());

            var written = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Guardrail.class);
            verify(spi).update(written.capture());
            assertThat(written.getValue().getVersion()).isEqualTo(7);
        }

        /** The comparison is the store's, in the same step as the write: the adapter only passes it on. */
        @Test
        void should_leave_the_comparison_to_the_store() throws Exception {
            when(spi.updateIfVersionMatches(any(), eq(3))).thenAnswer(call -> Optional.of(call.getArgument(0)));

            var written = adapter.updateIfVersionMatches(aGuardrailWithId(GUARDRAIL_ID).toBuilder().version(4).build(), 3);

            assertThat(written).get().extracting(io.gravitee.gamma.rest.core.guardrail.model.Guardrail::getVersion).isEqualTo(4);
        }

        @Test
        void should_answer_nothing_when_the_store_wrote_nothing() throws Exception {
            when(spi.updateIfVersionMatches(any(), eq(3))).thenReturn(Optional.empty());

            assertThat(adapter.updateIfVersionMatches(aGuardrailWithId(GUARDRAIL_ID).toBuilder().version(4).build(), 3)).isEmpty();
        }

        @Test
        void should_report_a_name_clash_on_a_conditional_write_as_a_conflict() throws Exception {
            when(spi.updateIfVersionMatches(any(), eq(3))).thenThrow(new DuplicateKeyException("E11000 duplicate key"));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(ENVIRONMENT_ID, "Credential leakage")).thenReturn(
                List.of(aStoredRowNamedLikeTheFixture("another-guardrail"))
            );

            assertThatThrownBy(() -> adapter.updateIfVersionMatches(aGuardrailWithId(GUARDRAIL_ID), 3)).isInstanceOf(
                GuardrailNameAlreadyExistsException.class
            );
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
                List.of(aStoredRowNamedLikeTheFixture("another-guardrail"))
            );

            assertThatThrownBy(() -> adapter.update(aGuardrailWithId(GUARDRAIL_ID)))
                .isInstanceOf(GuardrailNameAlreadyExistsException.class)
                .hasMessageContaining("Credential leakage");
        }

        /** No other Guardrail holds the name, so this is not a name clash and must not read as one. */
        @Test
        void should_rethrow_a_duplicate_key_that_is_not_a_name_clash() throws Exception {
            when(spi.update(any())).thenThrow(new TechnicalException("update failed", new DuplicateKeyException("pk_guardrails")));
            when(spi.findByEnvironmentIdAndNameIgnoreCase(any(), any())).thenReturn(List.of());

            assertThatThrownBy(() -> adapter.update(aGuardrailWithId(GUARDRAIL_ID))).isInstanceOf(TechnicalDomainException.class);
        }

        @Test
        void should_still_report_a_genuine_technical_failure_as_technical() throws Exception {
            when(spi.update(any())).thenThrow(new TechnicalException("connection reset"));

            assertThatThrownBy(() -> adapter.update(aGuardrailWithId(GUARDRAIL_ID))).isInstanceOf(TechnicalDomainException.class);
        }
    }

    @Nested
    class Mapping {

        @Test
        void should_write_the_definition_into_its_column_and_read_the_same_one_back() throws Exception {
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Guardrail.class);
            when(spi.create(captor.capture())).thenAnswer(call -> call.getArgument(0));

            var guardrail = aGuardrail().build();
            var created = adapter.create(guardrail);

            assertThat(captor.getValue().getDefinition()).contains("\"name\":\"Credential leakage\"").contains("llm-proxy");
            assertThat(created.getDefinition()).isEqualTo(guardrail.getDefinition());
            assertThat(created.getPolicies()).containsExactly(aPolicy());
            assertThat(created.getApiTypes()).containsExactly(ApiType.LLM_PROXY);
        }

        @Test
        void should_project_the_fields_the_search_needs_onto_their_own_columns() throws Exception {
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Guardrail.class);
            when(spi.create(captor.capture())).thenAnswer(call -> call.getArgument(0));

            adapter.create(aGuardrail().definition(aGuardrailDefinition().phase(Phase.RESPONSE).build()).build());

            var row = captor.getValue();
            assertThat(row.getId()).isEqualTo(GUARDRAIL_ID);
            assertThat(row.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
            assertThat(row.getName()).isEqualTo("Credential leakage");
            // stored as the constant name, whatever the wire spelling
            assertThat(row.getPhase().name()).isEqualTo("RESPONSE");
        }

        @Test
        void should_read_the_definition_rather_than_the_projected_columns() throws Exception {
            // the columns are a projection written on every save, so a row whose columns disagree with
            // its definition is a bug upstream; the definition is what the console gets either way
            var row = new io.gravitee.repository.management.model.Guardrail();
            row.setId(GUARDRAIL_ID);
            row.setEnvironmentId(ENVIRONMENT_ID);
            row.setName("stale column");
            row.setDefinition(
                "{\"id\":\"%s\",\"environmentId\":\"%s\",\"name\":\"the real name\",\"phase\":\"REQUEST\",\"apiTypes\":[\"llm-proxy\"]}".formatted(
                    GUARDRAIL_ID,
                    ENVIRONMENT_ID
                )
            );
            when(spi.findById(GUARDRAIL_ID)).thenReturn(Optional.of(row));

            var found = adapter.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow();

            assertThat(found.getName()).isEqualTo("the real name");
            assertThat(found.getApiTypes()).isEqualTo(Set.of(ApiType.LLM_PROXY));
        }

        @Test
        void should_fail_loudly_on_a_row_with_no_stored_definition() throws Exception {
            var row = new io.gravitee.repository.management.model.Guardrail();
            row.setId(GUARDRAIL_ID);
            row.setEnvironmentId(ENVIRONMENT_ID);
            when(spi.findById(GUARDRAIL_ID)).thenReturn(Optional.of(row));

            assertThatThrownBy(() -> adapter.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessageContaining(GUARDRAIL_ID);
        }

        @Test
        void should_carry_the_timestamps_across_the_date_boundary() throws Exception {
            when(spi.create(any())).thenAnswer(call -> call.getArgument(0));

            var guardrail = aGuardrail().build();
            var created = adapter.create(guardrail);

            assertThat(created.getCreatedAt()).isEqualTo(guardrail.getCreatedAt());
            assertThat(created.getUpdatedAt()).isEqualTo(guardrail.getUpdatedAt());
        }
    }

    @Nested
    class FindByEnvironmentId {

        @Test
        void should_answer_empty_for_a_guardrail_of_another_environment() throws Exception {
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Guardrail.class);
            when(spi.create(captor.capture())).thenAnswer(call -> call.getArgument(0));
            adapter.create(aGuardrail().build());
            when(spi.findById(GUARDRAIL_ID)).thenReturn(Optional.of(captor.getValue()));

            assertThat(adapter.findByEnvironmentId("other-env", GUARDRAIL_ID)).isEmpty();
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

            var page = adapter.search(new GuardrailSearchQuery("env-1", null, 3, 20, "-updatedAt"));

            assertThat(pageable.getValue().pageNumber()).isEqualTo(2);
            assertThat(pageable.getValue().pageSize()).isEqualTo(20);
            assertThat(page.getPageNumber()).isEqualTo(3);
        }

        @Test
        void should_let_the_repository_apply_its_default_sort_when_none_is_given() throws Exception {
            when(spi.search(any(), any(), any())).thenReturn(new Page<>(List.of(), 1, 0, 0));

            adapter.search(new GuardrailSearchQuery("env-1", null, 1, 10, null));

            verify(spi).search(any(), any(), org.mockito.ArgumentMatchers.isNull());
        }
    }
}
