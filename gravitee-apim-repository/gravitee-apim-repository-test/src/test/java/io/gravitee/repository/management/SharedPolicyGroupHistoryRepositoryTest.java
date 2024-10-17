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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.SharedPolicyGroupHistoryCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.SharedPolicyGroup;
import io.gravitee.repository.management.model.SharedPolicyGroupLifecycleState;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class SharedPolicyGroupHistoryRepositoryTest extends AbstractManagementRepositoryTest {

    private static final String ENV_ID_TO_BE_DELETED = "env_to_be_deleted";

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Before
    public void before() throws TechnicalException {
        for (int i = 0; i < 10; i++) {
            var date = new Date(new Date(172321739410L).getTime() + i * 1000);
            // Create shared policy group history with different name
            var sharedPolicyGroupBuilder = getDefaultSharedPolicyGroupBuilder("sharedPolicyGroupId_1");
            sharedPolicyGroupBuilder.name("Name changed " + i + " times");
            sharedPolicyGroupBuilder.lifecycleState(
                i % 2 == 0 ? SharedPolicyGroupLifecycleState.UNDEPLOYED : SharedPolicyGroupLifecycleState.DEPLOYED
            );
            sharedPolicyGroupBuilder.createdAt(date);
            sharedPolicyGroupBuilder.updatedAt(date);
            sharedPolicyGroupBuilder.deployedAt(date);
            sharedPolicyGroupHistoryRepository.create(sharedPolicyGroupBuilder.build());
        }
        // Create another shared policy group history with different name
        final SharedPolicyGroup sharedPolicyGroup2 = getDefaultSharedPolicyGroupBuilder("sharedPolicyGroupId_2")
            .name("Yet another SPG 2")
            .build();
        sharedPolicyGroupHistoryRepository.create(sharedPolicyGroup2);
        final SharedPolicyGroup sharedPolicyGroup3 = getDefaultSharedPolicyGroupBuilder("sharedPolicyGroupId_3")
            .name("Yet another SPG 3")
            .build();
        sharedPolicyGroupHistoryRepository.create(sharedPolicyGroup3);
        sharedPolicyGroupHistoryRepository.create(
            getDefaultSharedPolicyGroupBuilder("id_to_be_deleted_1")
                .updatedAt(new Date(new Date(172323739410L).getTime()))
                .environmentId(ENV_ID_TO_BE_DELETED)
                .build()
        );
        sharedPolicyGroupHistoryRepository.create(
            getDefaultSharedPolicyGroupBuilder("id_to_be_deleted_1")
                .updatedAt(new Date(new Date(172324839410L).getTime()))
                .environmentId(ENV_ID_TO_BE_DELETED)
                .build()
        );
    }

    @Test
    public void should_create() throws Exception {
        final SharedPolicyGroup sharedPolicyGroupV1 = getDefaultSharedPolicyGroupBuilder("id_create_test")
            .name("Name v1")
            .updatedAt(new Date(172321739410L))
            .build();
        sharedPolicyGroupHistoryRepository.create(sharedPolicyGroupV1);

        final SharedPolicyGroup sharedPolicyGroupV2 = getDefaultSharedPolicyGroupBuilder("id_create_test").name("Name v2").build();
        final SharedPolicyGroup create = sharedPolicyGroupHistoryRepository.create(sharedPolicyGroupV2);

        assertThat(create.getName()).isEqualTo(sharedPolicyGroupV2.getName());
        assertThat(create.getVersion()).isEqualTo(sharedPolicyGroupV2.getVersion());
        assertThat(create.getDescription()).isEqualTo(sharedPolicyGroupV2.getDescription());
        assertThat(create.getPrerequisiteMessage()).isEqualTo(sharedPolicyGroupV2.getPrerequisiteMessage());
        assertThat(create.getCrossId()).isEqualTo(sharedPolicyGroupV2.getCrossId());
        assertThat(create.getApiType()).isEqualTo(sharedPolicyGroupV2.getApiType());
        assertThat(create.getPhase()).isEqualTo(sharedPolicyGroupV2.getPhase());
        assertThat(create.getDefinition()).isEqualTo(sharedPolicyGroupV2.getDefinition());
        assertThat(create.getLifecycleState()).isEqualTo(sharedPolicyGroupV2.getLifecycleState());
        assertThat(create.getEnvironmentId()).isEqualTo(sharedPolicyGroupV2.getEnvironmentId());
        assertThat(create.getOrganizationId()).isEqualTo(sharedPolicyGroupV2.getOrganizationId());
        assertThat(create.getDeployedAt()).isEqualTo(sharedPolicyGroupV2.getDeployedAt());
        assertThat(create.getCreatedAt()).isEqualTo(create.getCreatedAt());
        assertThat(create.getUpdatedAt()).isEqualTo(create.getUpdatedAt());
    }

    @Test
    public void should_search_with_no_criteria() throws TechnicalException {
        final var criteria = SharedPolicyGroupHistoryCriteria.builder().environmentId("environmentId").build();

        final var page = sharedPolicyGroupHistoryRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(2).build(), null);
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(12);
    }

    @Test
    public void should_search_with_SharedPolicyGroupId_criteria() throws TechnicalException {
        final var criteria = SharedPolicyGroupHistoryCriteria
            .builder()
            .environmentId("environmentId")
            .sharedPolicyGroupId("sharedPolicyGroupId_1")
            .build();

        final var page = sharedPolicyGroupHistoryRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(2).build(), null);
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(10);
    }

    @Test
    public void should_search_page_2() throws TechnicalException {
        final var criteria = SharedPolicyGroupHistoryCriteria
            .builder()
            .environmentId("environmentId")
            .sharedPolicyGroupId("sharedPolicyGroupId_1")
            .build();

        final var page = sharedPolicyGroupHistoryRepository.search(criteria, new PageableBuilder().pageNumber(2).pageSize(2).build(), null);
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(2);
        var aa = page.getContent().get(0).getName();
        if (aa.equals("Name changed 3 times")) {
            var tt = "Name changed 3 times";
        }
        assertThat((page.getContent().get(0)).getName()).isEqualTo("Name changed 4 times");
        assertThat((page.getContent().get(1)).getName()).isEqualTo("Name changed 5 times");
        assertThat(page.getTotalElements()).isEqualTo(10);
    }

    @Test
    public void should_search_with_no_result() throws TechnicalException {
        final var criteria = SharedPolicyGroupHistoryCriteria
            .builder()
            .environmentId("environmentId")
            .sharedPolicyGroupId("unknown")
            .build();

        final var page = sharedPolicyGroupHistoryRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(2).build(), null);
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void should_search_with_no_result_with_unknown_environment() throws TechnicalException {
        final var criteria = SharedPolicyGroupHistoryCriteria
            .builder()
            .environmentId("unknown")
            .sharedPolicyGroupId("sharedPolicyGroupId_1")
            .build();

        final var page = sharedPolicyGroupHistoryRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(2).build(), null);
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void should_search_with_sortable() throws TechnicalException {
        final var criteria = SharedPolicyGroupHistoryCriteria
            .builder()
            .environmentId("environmentId")
            .sharedPolicyGroupId("sharedPolicyGroupId_1")
            .build();

        final var page = sharedPolicyGroupHistoryRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(2).build(), null);
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(2);
        assertThat((page.getContent().get(0)).getName()).isEqualTo("Name changed 0 times");
        assertThat((page.getContent().get(1)).getName()).isEqualTo("Name changed 1 times");
        assertThat(page.getTotalElements()).isEqualTo(10);

        final var page2 = sharedPolicyGroupHistoryRepository.search(
            criteria,
            new PageableBuilder().pageNumber(0).pageSize(2).build(),
            new SortableBuilder().field("name").setAsc(false).build()
        );
        assertThat(page2).isNotNull();
        assertThat(page2.getContent()).hasSize(2);
        assertThat((page2.getContent().get(0)).getName()).isEqualTo("Name changed 9 times");
        assertThat((page2.getContent().get(1)).getName()).isEqualTo("Name changed 8 times");
        assertThat(page2.getTotalElements()).isEqualTo(10);
    }

    @Test
    public void should_search_with_lifecycle_state_criteria() throws TechnicalException {
        final var criteria = SharedPolicyGroupHistoryCriteria
            .builder()
            .environmentId("environmentId")
            .lifecycleState(SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .build();

        final var page = sharedPolicyGroupHistoryRepository.search(criteria, new PageableBuilder().pageNumber(0).pageSize(2).build(), null);
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(7);
    }

    @Test
    public void should_searchLatestBySharedPolicyGroupId() throws TechnicalException {
        final var page1 = sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyGroupId(
            "environmentId",
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );
        assertThat(page1).isNotNull();
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(3);

        assertThat(page1.getContent().get(0).getName()).isEqualTo("Name changed 9 times");
        assertThat(page1.getContent().get(1).getName()).isEqualTo("Yet another SPG 2");

        final var page2 = sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyGroupId(
            "environmentId",
            new PageableBuilder().pageNumber(1).pageSize(2).build()
        );
        assertThat(page2).isNotNull();
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getContent().get(0).getName()).isEqualTo("Yet another SPG 3");
    }

    @Test
    public void should_searchLatestBySharedPolicyGroupId_with_no_result() throws TechnicalException {
        final var page = sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyGroupId(
            "unknown",
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void should_searchLatestBySharedPolicyGroupId_with_no_result_with_unknown_environment() throws TechnicalException {
        final var page = sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyGroupId(
            "unknown",
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void should_getLatestBySharedPolicyGroupId() throws TechnicalException {
        final var sharedPolicyGroup = sharedPolicyGroupHistoryRepository.getLatestBySharedPolicyGroupId(
            "environmentId",
            "sharedPolicyGroupId_1"
        );
        assertThat(sharedPolicyGroup).isPresent();
        assertThat(sharedPolicyGroup.get().getName()).isEqualTo("Name changed 9 times");
    }

    @Test
    public void should_getLatestBySharedPolicyGroupId_with_no_result() throws TechnicalException {
        final var sharedPolicyGroup = sharedPolicyGroupHistoryRepository.getLatestBySharedPolicyGroupId("environmentId", "unknown");
        assertThat(sharedPolicyGroup).isEmpty();
    }

    @Test
    public void should_delete() throws TechnicalException {
        final var sharedPolicyGroup = sharedPolicyGroupHistoryRepository.getLatestBySharedPolicyGroupId(
            "environmentId",
            "sharedPolicyGroupId_1"
        );
        assertThat(sharedPolicyGroup).isPresent();

        sharedPolicyGroupHistoryRepository.delete(sharedPolicyGroup.get().getId());

        final var sharedPolicyGroupAfterDelete = sharedPolicyGroupHistoryRepository.getLatestBySharedPolicyGroupId(
            "environmentId",
            "sharedPolicyGroupId_1"
        );
        assertThat(sharedPolicyGroupAfterDelete).isEmpty();
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        final var criteria = SharedPolicyGroupHistoryCriteria.builder().environmentId(ENV_ID_TO_BE_DELETED).build();
        final var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();

        final var nbBeforeDeletion = sharedPolicyGroupHistoryRepository.search(criteria, pageable, null).getTotalElements();
        final var deleted = sharedPolicyGroupHistoryRepository.deleteByEnvironmentId(ENV_ID_TO_BE_DELETED);
        final var nbAfterDeletion = sharedPolicyGroupHistoryRepository.search(criteria, pageable, null).getTotalElements();

        assertThat(nbBeforeDeletion).isEqualTo(2);
        assertThat(deleted.size()).isEqualTo(2);
        assertThat(nbAfterDeletion).isEqualTo(0);
    }

    private static SharedPolicyGroup.SharedPolicyGroupBuilder getDefaultSharedPolicyGroupBuilder(String id) {
        return SharedPolicyGroup
            .builder()
            .id(id)
            .name("name")
            .version(1)
            .description("description")
            .crossId("crossId_" + id)
            .apiType(ApiType.PROXY)
            .phase(SharedPolicyGroup.FlowPhase.REQUEST)
            .definition("definition")
            .lifecycleState(SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .environmentId("environmentId")
            .organizationId("organizationId")
            .deployedAt(new Date())
            .createdAt(new Date())
            .updatedAt(new Date());
    }
}
