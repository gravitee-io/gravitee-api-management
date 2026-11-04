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

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.GuardianCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Guardian;
import io.gravitee.repository.management.model.GuardianLifecycleState;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
                .description("description")
                .version("1.0.0")
                .apiType(ApiType.LLM_PROXY)
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

    @Test
    public void should_create() throws Exception {
        var date = new Date();
        final Guardian guardian = Guardian.builder()
            .id("id_create_test")
            .environmentId("environmentId")
            .name("name")
            .description("description")
            .version("1.0.0")
            .apiType(ApiType.LLM_PROXY)
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
        assertThat(optional.get().getDescription()).isEqualTo("description");
        assertThat(optional.get().getVersion()).isEqualTo("1.0.0");
        assertThat(optional.get().getApiType()).isEqualTo(ApiType.LLM_PROXY);
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
            .description("updated description")
            .version("2.0.0")
            .phase(Guardian.Phase.RESPONSE)
            .lifecycleState(GuardianLifecycleState.DEPLOYED)
            .build();

        guardianRepository.update(guardian);

        final Optional<Guardian> updated = guardianRepository.findById("id_update_test");
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("updated name");
        assertThat(updated.get().getDescription()).isEqualTo("updated description");
        assertThat(updated.get().getVersion()).isEqualTo("2.0.0");
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
        assertThat(optional.get().getDescription()).isEqualTo("description");
        assertThat(optional.get().getVersion()).isEqualTo("1.0.0");
        assertThat(optional.get().getApiType()).isEqualTo(ApiType.LLM_PROXY);
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
