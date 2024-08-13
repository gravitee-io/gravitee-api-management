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
import org.junit.Before;
import org.junit.Test;

public class SharedPolicyGroupHistoryRepositoryTest extends AbstractManagementRepositoryTest {

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
    }

    @Test
    public void should_create() throws Exception {
        var date = new Date();
        final SharedPolicyGroup sharedPolicyGroup = SharedPolicyGroup
            .builder()
            .id("id_create_test")
            .name("name")
            .version(1)
            .description("description")
            .prerequisiteMessage("prerequisiteMessage")
            .crossId("crossId")
            .apiType(ApiType.PROXY)
            .phase(SharedPolicyGroup.ExecutionPhase.REQUEST)
            .definition("definition")
            .lifecycleState(SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .environmentId("environmentId")
            .organizationId("organizationId")
            .deployedAt(date)
            .createdAt(date)
            .updatedAt(date)
            .build();

        final SharedPolicyGroup create = sharedPolicyGroupHistoryRepository.create(sharedPolicyGroup);

        assertThat(create.getName()).isEqualTo(sharedPolicyGroup.getName());
        assertThat(create.getVersion()).isEqualTo(sharedPolicyGroup.getVersion());
        assertThat(create.getDescription()).isEqualTo(sharedPolicyGroup.getDescription());
        assertThat(create.getPrerequisiteMessage()).isEqualTo(sharedPolicyGroup.getPrerequisiteMessage());
        assertThat(create.getCrossId()).isEqualTo(sharedPolicyGroup.getCrossId());
        assertThat(create.getApiType()).isEqualTo(sharedPolicyGroup.getApiType());
        assertThat(create.getPhase()).isEqualTo(sharedPolicyGroup.getPhase());
        assertThat(create.getDefinition()).isEqualTo(sharedPolicyGroup.getDefinition());
        assertThat(create.getLifecycleState()).isEqualTo(sharedPolicyGroup.getLifecycleState());
        assertThat(create.getEnvironmentId()).isEqualTo(sharedPolicyGroup.getEnvironmentId());
        assertThat(create.getOrganizationId()).isEqualTo(sharedPolicyGroup.getOrganizationId());
        assertThat(create.getDeployedAt()).isEqualTo(sharedPolicyGroup.getDeployedAt());
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
    public void should_searchLatestBySharedPolicyPolicyGroupId() throws TechnicalException {
        final var page1 = sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyPolicyGroupId(
            "environmentId",
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );
        assertThat(page1).isNotNull();
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(3);

        assertThat(page1.getContent().get(0).getName()).isEqualTo("Name changed 9 times");
        assertThat(page1.getContent().get(1).getName()).isEqualTo("Yet another SPG 2");

        final var page2 = sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyPolicyGroupId(
            "environmentId",
            new PageableBuilder().pageNumber(1).pageSize(2).build()
        );
        assertThat(page2).isNotNull();
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getContent().get(0).getName()).isEqualTo("Yet another SPG 3");
    }

    @Test
    public void should_searchLatestBySharedPolicyPolicyGroupId_with_no_result() throws TechnicalException {
        final var page = sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyPolicyGroupId(
            "unknown",
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void should_searchLatestBySharedPolicyPolicyGroupId_with_no_result_with_unknown_environment() throws TechnicalException {
        final var page = sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyPolicyGroupId(
            "unknown",
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
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
            .phase(SharedPolicyGroup.ExecutionPhase.REQUEST)
            .definition("definition")
            .lifecycleState(SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .environmentId("environmentId")
            .organizationId("organizationId")
            .deployedAt(new Date())
            .createdAt(new Date())
            .updatedAt(new Date());
    }
}
