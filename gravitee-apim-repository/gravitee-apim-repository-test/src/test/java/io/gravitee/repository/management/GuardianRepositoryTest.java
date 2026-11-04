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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.GuardianCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Guardian;
import io.gravitee.repository.management.model.GuardianLifecycleState;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class GuardianRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/guardian-tests/";
    }

    @BeforeEach
    public void before() throws TechnicalException {
        for (int i = 0; i < 10; i++) {
            var date = new Date(new Date(172321739410L).getTime() + i * 1000);
            final Guardian guardian = Guardian.builder()
                .id("id_search_test_" + i)
                .environmentId("environmentId")
                .name("name search_test " + i)
                .description("description " + i)
                .phase(Guardian.Phase.REQUEST)
                .definition("definition")
                .lifecycleState(i % 2 == 0 ? GuardianLifecycleState.UNDEPLOYED : GuardianLifecycleState.DEPLOYED)
                .deployedAt(date)
                .createdAt(date)
                .updatedAt(date)
                .build();
            guardianRepository.create(guardian);
        }
    }

    /**
     * The CrudRepository contract plus the per-environment deletion the other management repositories
     * expose: one Guardian at a time, by id.
     */
    @Nested
    class Crud {

        @Test
        public void should_create() throws Exception {
            var date = new Date();
            final Guardian guardian = Guardian.builder()
                .id("id_create_test")
                .environmentId("environmentId")
                .name("name")
                .description("a description")
                .phase(Guardian.Phase.REQUEST)
                .definition("definition")
                .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
                .deployedAt(date)
                .createdAt(date)
                .updatedAt(date)
                .build();

            guardianRepository.create(guardian);

            final Optional<Guardian> optional = guardianRepository.findById("id_create_test");
            assertThat(optional).as("Guardian to create has not been found").isPresent();
            assertThat(optional.get().getName()).isEqualTo("name");
            assertThat(optional.get().getDescription()).isEqualTo("a description");
            assertThat(optional.get().getPhase()).isEqualTo(Guardian.Phase.REQUEST);
            assertThat(optional.get().getDefinition()).isEqualTo("definition");
            assertThat(optional.get().getLifecycleState()).isEqualTo(GuardianLifecycleState.UNDEPLOYED);
            assertThat(optional.get().getEnvironmentId()).isEqualTo("environmentId");
            assertThat(optional.get().isDeployed()).isFalse();
        }

        @Test
        public void should_update() throws Exception {
            final Optional<Guardian> optional = guardianRepository.findById("id_update_test");
            assertThat(optional).as("Guardian to update has not been found").isPresent();

            final Guardian guardian = optional
                .get()
                .toBuilder()
                .name("updated name")
                .phase(Guardian.Phase.RESPONSE)
                .lifecycleState(GuardianLifecycleState.DEPLOYED)
                .build();

            guardianRepository.update(guardian);

            final Optional<Guardian> updated = guardianRepository.findById("id_update_test");
            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("updated name");
            assertThat(updated.get().getPhase()).isEqualTo(Guardian.Phase.RESPONSE);
            assertThat(updated.get().getLifecycleState()).isEqualTo(GuardianLifecycleState.DEPLOYED);
            assertThat(updated.get().isDeployed()).isTrue();
        }

        @Test
        public void should_find_by_id() throws Exception {
            final Optional<Guardian> optional = guardianRepository.findById("id_find-by-id_test");
            assertThat(optional).isPresent();
            assertThat(optional.get().getEnvironmentId()).isEqualTo("environmentId");
            assertThat(optional.get().getName()).isEqualTo("name");
            assertThat(optional.get().getPhase()).isEqualTo(Guardian.Phase.RESPONSE);
            assertThat(optional.get().getDefinition()).isEqualTo("definition");
            assertThat(optional.get().getLifecycleState()).isEqualTo(GuardianLifecycleState.DEPLOYED);
            assertThat(optional.get().getDeployedAt()).isNotNull();
            assertThat(optional.get().getCreatedAt()).isNotNull();
            assertThat(optional.get().getUpdatedAt()).isNotNull();
        }

        @Test
        public void should_not_findById_missing() throws Exception {
            final Optional<Guardian> optional = guardianRepository.findById("id_does_not_exist");
            assertThat(optional).isNotPresent();
        }

        @Test
        public void should_delete() throws Exception {
            Optional<Guardian> optional = guardianRepository.findById("id_delete_test");
            assertThat(optional).as("Guardian to delete has not been found").isPresent();

            guardianRepository.delete("id_delete_test");

            optional = guardianRepository.findById("id_delete_test");
            assertThat(optional).as("Guardian to delete has not been deleted").isNotPresent();
        }

        @Test
        public void should_findAll() throws Exception {
            assertThrows(IllegalStateException.class, () -> {
                guardianRepository.findAll();
                fail("An exception must be thrown");
            });
        }

        @Test
        public void should_delete_by_environment_id() throws TechnicalException {
            final var criteria = GuardianCriteria.builder().environmentId("ToBeDeleted").build();
            final var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();

            final var nbBeforeDeletion = guardianRepository.search(criteria, pageable, null).getTotalElements();
            final var deleted = guardianRepository.deleteByEnvironmentId("ToBeDeleted").size();
            final var nbAfterDeletion = guardianRepository.search(criteria, pageable, null).getTotalElements();

            assertThat(nbBeforeDeletion).isEqualTo(2);
            assertThat(deleted).isEqualTo(2);
            assertThat(nbAfterDeletion).isZero();
        }
    }

    /**
     * The paged search over a GuardianCriteria: the filters, the sort, the paging and the two ways a
     * free-text query must not be read as a pattern. Grouped apart from the CRUD because every one of
     * them leans on the ten Guardians before() seeds.
     */
    @Nested
    class Search {

        @Test
        public void should_search_with_no_criteria() throws TechnicalException {
            final var criteria = GuardianCriteria.builder().environmentId("environmentId").build();

            final var page = guardianRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(2).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(13);
        }

        @Test
        public void should_search_with_name_criteria() throws TechnicalException {
            final var criteria = GuardianCriteria.builder().environmentId("environmentId").query("search_test").build();

            final var page = guardianRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(20).build(), null);
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
            guardianRepository.create(
                Guardian.builder()
                    .id("id_regex_test")
                    .environmentId("environmentId")
                    .name("Rate (limit) guard")
                    .phase(Guardian.Phase.REQUEST)
                    .definition("definition")
                    .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
                    .createdAt(date)
                    .updatedAt(date)
                    .build()
            );

            final var found = guardianRepository.search(
                GuardianCriteria.builder().environmentId("environmentId").query("(limit)").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );

            assertThat(found.getContent()).extracting(Guardian::getId).containsExactly("id_regex_test");
        }

        @Test
        public void should_not_let_a_regex_metacharacter_act_as_a_wildcard() throws TechnicalException {
            // "." is a literal here, not "any character": left unescaped it would match every row on
            // Mongo and none on JDBC, for the same request.
            final var page = guardianRepository.search(
                GuardianCriteria.builder().environmentId("environmentId").query("nam.").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        public void should_page_without_repeating_or_skipping_when_the_sort_values_tie() throws TechnicalException {
            // A Guardian's phase takes one of two values and its deployedAt is null, so a single sort key
            // leaves equal rows in an undefined order and offset paging can serve one twice while never
            // serving another. A unique secondary key makes the order total.
            var date = new Date();
            for (int i = 0; i < 6; i++) {
                guardianRepository.create(
                    Guardian.builder()
                        .id("id_tie_%d".formatted(i))
                        .environmentId("tieEnvironment")
                        .name("same name")
                        .phase(Guardian.Phase.REQUEST)
                        .definition("definition")
                        .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
                        .createdAt(date)
                        .updatedAt(date)
                        .build()
                );
            }

            final var seen = new java.util.ArrayList<String>();
            for (int page = 0; page < 3; page++) {
                guardianRepository
                    .search(
                        GuardianCriteria.builder().environmentId("tieEnvironment").build(),
                        new PageableBuilder().pageNumber(page).pageSize(2).build(),
                        new SortableBuilder().field("name").setAsc(true).build()
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
            final var underscore = guardianRepository.search(
                GuardianCriteria.builder().environmentId("environmentId").query("nam_").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );
            assertThat(underscore.getContent()).isEmpty();

            // and '%' likewise
            final var percent = guardianRepository.search(
                GuardianCriteria.builder().environmentId("environmentId").query("nam%").build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build(),
                null
            );
            assertThat(percent.getContent()).isEmpty();
        }

        @Test
        public void should_search_page_2() throws TechnicalException {
            final var criteria = GuardianCriteria.builder().environmentId("environmentId").build();

            final var page = guardianRepository.search(criteria, new PageableBuilder().pageNumber(1).pageSize(5).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getPageNumber()).isEqualTo(1);
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.getTotalElements()).isEqualTo(13);
        }

        @Test
        public void should_search_with_no_result() throws TechnicalException {
            final var criteria = GuardianCriteria.builder().environmentId("environmentId").query("does-not-exist").build();

            final var page = guardianRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(10).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        public void should_search_with_unknown_environment() throws TechnicalException {
            final var criteria = GuardianCriteria.builder().environmentId("unknown").build();

            final var page = guardianRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(10).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        public void should_search_with_sortable() throws TechnicalException {
            final var criteria = GuardianCriteria.builder().environmentId("environmentId").query("search_test").build();

            final var page = guardianRepository.search(
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
            final var criteria = GuardianCriteria.builder()
                .environmentId("environmentId")
                .lifecycleState(GuardianLifecycleState.DEPLOYED)
                .build();

            final var page = guardianRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(20).build(), null);
            assertThat(page).isNotNull();
            assertThat(page.getTotalElements()).isEqualTo(6);
            assertThat(page.getContent()).allMatch(Guardian::isDeployed);
        }
    }
}
