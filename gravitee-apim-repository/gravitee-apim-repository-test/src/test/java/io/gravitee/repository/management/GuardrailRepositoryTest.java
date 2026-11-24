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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.GuardrailCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Guardrail;
import io.gravitee.repository.management.model.GuardrailLifecycleState;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class GuardrailRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/guardrail-tests/";
    }

    @BeforeEach
    public void before() throws TechnicalException {
        for (int i = 0; i < 10; i++) {
            var date = new Date(new Date(172321739410L).getTime() + i * 1000);
            final Guardrail guardrail = Guardrail.builder()
                .id("id_search_test_" + i)
                .environmentId("environmentId")
                .name("name search_test " + i)
                .description("description " + i)
                .phase(Guardrail.Phase.REQUEST)
                .definition("definition")
                .lifecycleState(i % 2 == 0 ? GuardrailLifecycleState.UNDEPLOYED : GuardrailLifecycleState.DEPLOYED)
                .deployedAt(date)
                .createdAt(date)
                .updatedAt(date)
                .build();
            guardrailRepository.create(guardrail);
        }
    }

    @Test
    void should_find_only_requested_guardrails_in_the_environment() throws Exception {
        var found = guardrailRepository.findByEnvironmentIdAndIds(
            "environmentId",
            Set.of("id_search_test_0", "id_search_test_2", "missing")
        );
        var foreign = guardrailRepository.findByEnvironmentIdAndIds("another-environment", Set.of("id_search_test_0"));
        var empty = guardrailRepository.findByEnvironmentIdAndIds("environmentId", Set.of());
        assertSoftly(softly -> {
            softly
                .assertThat(found)
                .as("requested Guardrails, ignoring missing ids")
                .extracting(Guardrail::getId)
                .containsExactlyInAnyOrder("id_search_test_0", "id_search_test_2");
            softly.assertThat(foreign).as("foreign environment cannot read Guardrails").isEmpty();
            softly.assertThat(empty).as("empty ids do not select every Guardrail").isEmpty();
        });
    }

    @Test
    void should_advance_updated_at_without_replacing_the_draft_or_crossing_environments() throws Exception {
        var before = guardrailRepository.findById("id_search_test_0").orElseThrow();
        var withdrawnAt = new Date(before.getUpdatedAt().getTime() + 100_000);

        guardrailRepository.updateUpdatedAt("another-environment", before.getId(), withdrawnAt);
        assertThat(guardrailRepository.findById(before.getId())).as("foreign environment cannot touch the Guardrail").contains(before);

        guardrailRepository.updateUpdatedAt(before.getEnvironmentId(), before.getId(), withdrawnAt);
        var after = guardrailRepository.findById(before.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly
                .assertThat(after)
                .as("all draft fields preserved")
                .usingRecursiveComparison()
                .ignoringFields("updatedAt")
                .isEqualTo(before);
            softly.assertThat(after.getUpdatedAt()).as("withdrawal indexed for sorting").isEqualTo(withdrawnAt);
        });

        guardrailRepository.updateUpdatedAt(before.getEnvironmentId(), before.getId(), before.getUpdatedAt());
        assertThat(guardrailRepository.findById(before.getId()).orElseThrow().getUpdatedAt())
            .as("retry cannot move a newer draft timestamp backwards")
            .isEqualTo(withdrawnAt);
    }

    /**
     * The CrudRepository contract plus the per-environment deletion the other management repositories
     * expose: one Guardrail at a time, by id.
     */
    @Nested
    class Crud {

        @Test
        public void should_create() throws Exception {
            var date = new Date();
            final Guardrail guardrail = Guardrail.builder()
                .id("id_create_test")
                .environmentId("environmentId")
                .name("name")
                .description("a description")
                .phase(Guardrail.Phase.REQUEST)
                .definition("definition")
                .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
                .deployedAt(date)
                .createdAt(date)
                .updatedAt(date)
                .build();

            guardrailRepository.create(guardrail);

            final Optional<Guardrail> optional = guardrailRepository.findById("id_create_test");
            assertThat(optional).as("Guardrail to create has not been found").isPresent();
            assertThat(optional.get().getName()).isEqualTo("name");
            assertThat(optional.get().getDescription()).isEqualTo("a description");
            assertThat(optional.get().getPhase()).isEqualTo(Guardrail.Phase.REQUEST);
            assertThat(optional.get().getDefinition()).isEqualTo("definition");
            assertThat(optional.get().getLifecycleState()).isEqualTo(GuardrailLifecycleState.UNDEPLOYED);
            assertThat(optional.get().getEnvironmentId()).isEqualTo("environmentId");
            assertThat(optional.get().isDeployed()).isFalse();
        }

        @Test
        public void should_update() throws Exception {
            final Optional<Guardrail> optional = guardrailRepository.findById("id_update_test");
            assertThat(optional).as("Guardrail to update has not been found").isPresent();

            final Guardrail guardrail = optional
                .get()
                .toBuilder()
                .name("updated name")
                .phase(Guardrail.Phase.RESPONSE)
                .lifecycleState(GuardrailLifecycleState.DEPLOYED)
                .build();

            guardrailRepository.update(guardrail);

            final Optional<Guardrail> updated = guardrailRepository.findById("id_update_test");
            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("updated name");
            assertThat(updated.get().getPhase()).isEqualTo(Guardrail.Phase.RESPONSE);
            assertThat(updated.get().getLifecycleState()).isEqualTo(GuardrailLifecycleState.DEPLOYED);
            assertThat(updated.get().isDeployed()).isTrue();
        }

        /** The counter is stored and read back like any other field: nothing here increments it. */
        @Test
        public void should_keep_the_version_it_is_given() throws Exception {
            final Guardrail guardrail = guardrailRepository.findById("id_update_test").orElseThrow().toBuilder().version(4).build();

            guardrailRepository.update(guardrail);

            assertThat(guardrailRepository.findById("id_update_test")).get().extracting(Guardrail::getVersion).isEqualTo(4);
        }

        @Test
        public void should_read_no_version_from_a_guardrail_stored_without_one() throws Exception {
            assertThat(guardrailRepository.findById("id_find-by-id_test")).get().extracting(Guardrail::getVersion).isNull();
        }

        @Test
        public void should_replace_a_guardrail_still_at_the_expected_version() throws Exception {
            final Guardrail stored = guardrailRepository.update(
                guardrailRepository.findById("id_update_test").orElseThrow().toBuilder().version(1).build()
            );

            final Optional<Guardrail> written = guardrailRepository.updateIfVersionMatches(
                stored.toBuilder().name("written at version 1").version(2).build(),
                1
            );

            assertThat(written).get().extracting(Guardrail::getName, Guardrail::getVersion).containsExactly("written at version 1", 2);
            assertThat(guardrailRepository.findById("id_update_test"))
                .get()
                .extracting(Guardrail::getName, Guardrail::getVersion)
                .containsExactly("written at version 1", 2);
        }

        @Test
        public void should_leave_a_guardrail_that_moved_past_the_expected_version() throws Exception {
            final Guardrail stored = guardrailRepository.update(
                guardrailRepository.findById("id_update_test").orElseThrow().toBuilder().version(2).build()
            );

            final Optional<Guardrail> written = guardrailRepository.updateIfVersionMatches(
                stored.toBuilder().name("written from a stale read").version(2).build(),
                1
            );

            assertThat(written).isEmpty();
            assertThat(guardrailRepository.findById("id_update_test"))
                .get()
                .extracting(Guardrail::getName, Guardrail::getVersion)
                .containsExactly("name crud update", 2);
        }

        /** No version is not version zero, nor any other: such a Guardrail cannot be guarded. */
        @Test
        public void should_leave_a_guardrail_stored_without_a_version() throws Exception {
            final Guardrail stored = guardrailRepository.findById("id_update_test").orElseThrow();

            final Optional<Guardrail> written = guardrailRepository.updateIfVersionMatches(
                stored.toBuilder().name("written over no version").version(1).build(),
                0
            );

            assertThat(written).isEmpty();
            assertThat(guardrailRepository.findById("id_update_test")).get().extracting(Guardrail::getName).isEqualTo("name crud update");
        }

        @Test
        public void should_write_nothing_for_a_guardrail_that_does_not_exist() throws Exception {
            final Guardrail unknown = guardrailRepository
                .findById("id_update_test")
                .orElseThrow()
                .toBuilder()
                .id("unknown")
                .version(2)
                .build();

            assertThat(guardrailRepository.updateIfVersionMatches(unknown, 1)).isEmpty();
            assertThat(guardrailRepository.findById("unknown")).isEmpty();
        }

        @Test
        public void should_find_by_id() throws Exception {
            final Optional<Guardrail> optional = guardrailRepository.findById("id_find-by-id_test");
            assertThat(optional).isPresent();
            assertThat(optional.get().getEnvironmentId()).isEqualTo("environmentId");
            assertThat(optional.get().getName()).isEqualTo("name crud find by id");
            assertThat(optional.get().getPhase()).isEqualTo(Guardrail.Phase.RESPONSE);
            assertThat(optional.get().getDefinition()).isEqualTo("definition");
            assertThat(optional.get().getLifecycleState()).isEqualTo(GuardrailLifecycleState.DEPLOYED);
            assertThat(optional.get().getDeployedAt()).isNotNull();
            assertThat(optional.get().getCreatedAt()).isNotNull();
            assertThat(optional.get().getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_not_findById_missing() throws Exception {
            final Optional<Guardrail> optional = guardrailRepository.findById("id_does_not_exist");
            assertThat(optional).isNotPresent();
        }

        @Test
        public void should_delete() throws Exception {
            Optional<Guardrail> optional = guardrailRepository.findById("id_delete_test");
            assertThat(optional).as("Guardrail to delete has not been found").isPresent();

            guardrailRepository.delete("id_delete_test");

            optional = guardrailRepository.findById("id_delete_test");
            assertThat(optional).as("Guardrail to delete has not been deleted").isNotPresent();
        }

        @Test
        public void should_findAll() throws Exception {
            assertThrows(IllegalStateException.class, () -> {
                guardrailRepository.findAll();
                fail("An exception must be thrown");
            });
        }

        @Test
        public void should_find_by_environment_id_and_name_ignoring_case() throws TechnicalException {
            // the domain refuses a duplicate name ignoring case, so the store has to answer the same
            // question; a case-sensitive lookup would let "credential leakage" past the check
            var date = new Date();
            guardrailRepository.create(
                Guardrail.builder()
                    .id("id_name_lookup")
                    .environmentId("environmentId")
                    .name("Credential Leakage")
                    .phase(Guardrail.Phase.REQUEST)
                    .definition("definition")
                    .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
                    .createdAt(date)
                    .updatedAt(date)
                    .build()
            );

            assertThat(guardrailRepository.findByEnvironmentIdAndNameIgnoreCase("environmentId", "Credential Leakage"))
                .extracting(Guardrail::getId)
                .containsExactly("id_name_lookup");
            assertThat(guardrailRepository.findByEnvironmentIdAndNameIgnoreCase("environmentId", "credential leakage")).hasSize(1);
            assertThat(guardrailRepository.findByEnvironmentIdAndNameIgnoreCase("environmentId", "CREDENTIAL LEAKAGE")).hasSize(1);
        }

        @Test
        public void should_not_find_a_name_belonging_to_another_environment() throws TechnicalException {
            // a Guardrail is defined per environment, so the same name is free in the next one
            var date = new Date();
            guardrailRepository.create(
                Guardrail.builder()
                    .id("id_name_other_env")
                    .environmentId("anotherEnvironment")
                    .name("Scoped name")
                    .phase(Guardrail.Phase.REQUEST)
                    .definition("definition")
                    .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
                    .createdAt(date)
                    .updatedAt(date)
                    .build()
            );

            assertThat(guardrailRepository.findByEnvironmentIdAndNameIgnoreCase("environmentId", "Scoped name")).isEmpty();
            assertThat(guardrailRepository.findByEnvironmentIdAndNameIgnoreCase("anotherEnvironment", "Scoped name")).hasSize(1);
        }

        @Test
        public void should_return_empty_when_no_guardrail_carries_the_name() throws TechnicalException {
            assertThat(guardrailRepository.findByEnvironmentIdAndNameIgnoreCase("environmentId", "nothing carries this")).isEmpty();
        }

        @Test
        public void should_delete_by_environment_id() throws TechnicalException {
            final var criteria = GuardrailCriteria.builder().environmentId("ToBeDeleted").build();
            final var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();

            final var nbBeforeDeletion = guardrailRepository.search(criteria, pageable, null).getTotalElements();
            final var deleted = guardrailRepository.deleteByEnvironmentId("ToBeDeleted").size();
            final var nbAfterDeletion = guardrailRepository.search(criteria, pageable, null).getTotalElements();

            assertThat(nbBeforeDeletion).isEqualTo(2);
            assertThat(deleted).isEqualTo(2);
            assertThat(nbAfterDeletion).isZero();
        }
    }

    /**
     * The paged search over a GuardrailCriteria: the filters, the sort, the paging and the two ways a
     * free-text query must not be read as a pattern. Grouped apart from the CRUD because every one of
     * them leans on the ten Guardrails before() seeds.
     */
    @Nested
    class Search {

        @Test
        public void should_search_with_no_criteria() throws TechnicalException {
            final var criteria = GuardrailCriteria.builder().environmentId("environmentId").build();

            final var page = guardrailRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(2).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(13);
        }

        @Test
        public void should_search_with_name_criteria() throws TechnicalException {
            final var criteria = GuardrailCriteria.builder().environmentId("environmentId").query("search_test").build();

            final var page = guardrailRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(20).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotalElements()).isEqualTo(10);
        }

        @Test
        public void should_treat_the_query_as_literal_text_on_both_backends() throws TechnicalException {
            // Mongo matches the query as a regex and JDBC as a LIKE pattern, so a name holding a regex
            // metacharacter is where the two backends would part company: unescaped, "(" makes Mongo
            // reject the query outright while JDBC finds the row.
            var date = new Date();
            guardrailRepository.create(
                Guardrail.builder()
                    .id("id_regex_test")
                    .environmentId("environmentId")
                    .name("Rate (limit) guard")
                    .phase(Guardrail.Phase.REQUEST)
                    .definition("definition")
                    .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
                    .createdAt(date)
                    .updatedAt(date)
                    .build()
            );

            final var found = guardrailRepository.search(
                GuardrailCriteria.builder().environmentId("environmentId").query("(limit)").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );

            assertThat(found.getContent()).extracting(Guardrail::getId).containsExactly("id_regex_test");
        }

        @Test
        public void should_not_let_a_regex_metacharacter_act_as_a_wildcard() throws TechnicalException {
            // "." is a literal here, not "any character": left unescaped it would match every row on
            // Mongo and none on JDBC, for the same request.
            final var page = guardrailRepository.search(
                GuardrailCriteria.builder().environmentId("environmentId").query("nam.").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        public void should_page_without_repeating_or_skipping_when_the_sort_values_tie() throws TechnicalException {
            // A Guardrail's phase takes one of two values, so a single sort key leaves equal rows in an
            // undefined order and offset paging can serve one twice while never serving another. A
            // unique secondary key makes the order total.
            //
            // The tie is on the phase rather than on the name: a name is unique within an environment,
            // so two rows of one environment can no longer tie on it.
            var date = new Date();
            for (int i = 0; i < 6; i++) {
                guardrailRepository.create(
                    Guardrail.builder()
                        .id("id_tie_%d".formatted(i))
                        .environmentId("tieEnvironment")
                        .name("tie %d".formatted(i))
                        .phase(Guardrail.Phase.REQUEST)
                        .definition("definition")
                        .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
                        .createdAt(date)
                        .updatedAt(date)
                        .build()
                );
            }

            final var seen = new java.util.ArrayList<String>();
            for (int page = 0; page < 3; page++) {
                guardrailRepository
                    .search(
                        GuardrailCriteria.builder().environmentId("tieEnvironment").build(),
                        new PageableBuilder().pageNumber(page).pageSize(2).build(),
                        new SortableBuilder().field("phase").setAsc(true).build()
                    )
                    .getContent()
                    .forEach(g -> seen.add(g.getId()));
            }

            assertThat(seen).hasSize(6).doesNotHaveDuplicates();
        }

        @Test
        public void should_not_let_a_like_wildcard_act_as_a_wildcard_either() throws TechnicalException {
            // The mirror of the regex case, on the other backend: '_' matches any single character in SQL
            // LIKE, so left unescaped q="nam_" would match "name" on JDBC while Mongo, which quotes the
            // query, matches nothing. Same request, different answer per backend.
            final var underscore = guardrailRepository.search(
                GuardrailCriteria.builder().environmentId("environmentId").query("nam_").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );
            assertThat(underscore.getContent()).isEmpty();

            // and '%' likewise
            final var percent = guardrailRepository.search(
                GuardrailCriteria.builder().environmentId("environmentId").query("nam%").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );
            assertThat(percent.getContent()).isEmpty();
        }

        @Test
        public void should_not_let_a_bracket_act_as_a_character_class() throws TechnicalException {
            // SQL Server reads [...] in a LIKE pattern as a character class, which PostgreSQL and MySQL
            // do not: left unescaped, q="[x]" would match any name containing an x there while Mongo,
            // which quotes the query, matches nothing. Same request, different answer per backend.
            var date = new Date();
            guardrailRepository.create(
                Guardrail.builder()
                    .id("id_bracket_test")
                    .environmentId("environmentId")
                    .name("plain x guard")
                    .phase(Guardrail.Phase.REQUEST)
                    .definition("definition")
                    .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
                    .createdAt(date)
                    .updatedAt(date)
                    .build()
            );

            final var page = guardrailRepository.search(
                GuardrailCriteria.builder().environmentId("environmentId").query("[x]").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        public void should_match_the_jdbc_escape_character_as_literal_text() throws TechnicalException {
            // JDBC escapes the LIKE wildcards with a character of its own, so a name holding that
            // character is where the choice could leak into the contract: read as an escape rather than
            // as text, "Alert!" would find nothing here while Mongo, which quotes the query, finds the
            // row. Whichever character the SQL side picks, this has to keep passing.
            var date = new Date();
            guardrailRepository.create(
                Guardrail.builder()
                    .id("id_escape_test")
                    .environmentId("environmentId")
                    .name("Alert! guard")
                    .phase(Guardrail.Phase.REQUEST)
                    .definition("definition")
                    .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
                    .createdAt(date)
                    .updatedAt(date)
                    .build()
            );

            final var found = guardrailRepository.search(
                GuardrailCriteria.builder().environmentId("environmentId").query("Alert!").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );

            assertThat(found.getContent()).extracting(Guardrail::getName).containsExactly("Alert! guard");
        }

        @Test
        public void should_search_page_2() throws TechnicalException {
            final var criteria = GuardrailCriteria.builder().environmentId("environmentId").build();

            final var page = guardrailRepository.search(criteria, new PageableBuilder().pageNumber(1).pageSize(5).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getPageNumber()).isEqualTo(1);
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.getTotalElements()).isEqualTo(13);
        }

        @Test
        public void should_search_with_no_result() throws TechnicalException {
            final var criteria = GuardrailCriteria.builder().environmentId("environmentId").query("does-not-exist").build();

            final var page = guardrailRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(10).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        public void should_search_with_unknown_environment() throws TechnicalException {
            final var criteria = GuardrailCriteria.builder().environmentId("unknown").build();

            final var page = guardrailRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(10).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        public void should_search_with_sortable() throws TechnicalException {
            final var criteria = GuardrailCriteria.builder().environmentId("environmentId").query("search_test").build();

            final var page = guardrailRepository.search(
                criteria,
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                new SortableBuilder().field("name").setAsc(false).build()
            );

            assertThat(page).isNotNull();
            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getContent().get(0).getName()).isEqualTo("name search_test 9");
        }

        @Test
        public void should_search_with_lifecycle_state_criteria() throws TechnicalException {
            final var criteria = GuardrailCriteria.builder()
                .environmentId("environmentId")
                .lifecycleState(GuardrailLifecycleState.DEPLOYED)
                .build();

            final var page = guardrailRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(20).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getTotalElements()).isEqualTo(6);
            assertThat(page.getContent()).allMatch(Guardrail::isDeployed);
        }
    }
}
