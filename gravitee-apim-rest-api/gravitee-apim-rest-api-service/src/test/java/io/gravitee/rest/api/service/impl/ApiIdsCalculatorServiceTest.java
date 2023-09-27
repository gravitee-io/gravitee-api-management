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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiIdsCalculatorService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.imports.ImportApiJsonNode;
import io.gravitee.rest.api.service.imports.ImportJsonNodeWithIds;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiIdsCalculatorServiceTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    ApiIdsCalculatorService cut;

    @Mock
    private ApiService apiService;

    @Mock
    private PageService pageService;

    @Mock
    private PlanService planService;

    @BeforeEach
    void setUp() {
        cut = new ApiIdsCalculatorServiceImpl(apiService, pageService, planService);
    }

    @Test
    public void recalculateApiDefinitionIds_WithNoCrossId_should_recalculate_api_id_and_plans_id() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "my-api-1");

        apiDefinition.set(
            "plans",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "my-plan-id-1"))
                .add(mapper.createObjectNode().put("id", "my-plan-id-2"))
        );

        ImportApiJsonNode newApiDefinition = cut.recalculateApiDefinitionIds(
            new ExecutionContext("default", "uat"),
            new ImportApiJsonNode(apiDefinition)
        );

        assertThat(newApiDefinition.getId()).isEqualTo("e0a6482a-b8a7-3db4-a1b7-d36a462a9e38");
        assertThat(newApiDefinition.getPlans().get(0).getId()).isEqualTo("3ce45391-1777-3424-b4cb-2811b1281cea");
        assertThat(newApiDefinition.getPlans().get(1).getId()).isEqualTo("ecc775e1-2793-30b6-8668-251910d63563");
    }

    @Test
    public void recalculateApiDefinitionIds_WithNoCrossId_AndNoMatchingApi_should_recalculate_api_id_and_plans_id() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "api-id-1").put("crossId", "api-cross-id");

        apiDefinition.set(
            "plans",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "plan-id-1").put("crossId", "plan-cross-id-1"))
                .add(mapper.createObjectNode().put("id", "plan-id-2").put("crossId", "plan-cross-id-2"))
        );
        when(apiService.findByEnvironmentIdAndCrossId("uat", "api-cross-id")).thenReturn(Optional.empty());

        ImportApiJsonNode newApiDefinition = cut.recalculateApiDefinitionIds(
            new ExecutionContext("default", "uat"),
            new ImportApiJsonNode(apiDefinition)
        );

        assertThat(newApiDefinition.getId()).isEqualTo("6bcde800-d5ae-3215-8413-cae196f9edfc");
        assertThat(newApiDefinition.getPlans().get(0).getId()).isEqualTo("9e0bf9bc-137b-39d3-9e6a-3de94ac3788c");
        assertThat(newApiDefinition.getPlans().get(1).getId()).isEqualTo("8484a510-1b6d-3a40-a5fe-228f54f7727c");
    }

    @Test
    public void recalculateApiDefinitionIds_WithCrossId_should_not_recalculate_api_id_and_plans_ids() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "api-id-1").put("crossId", "api-cross-id");

        apiDefinition.set(
            "plans",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "plan-id-1").put("crossId", "plan-cross-id-1"))
                .add(mapper.createObjectNode().put("id", "plan-id-2").put("crossId", "plan-cross-id-2"))
        );

        ApiEntity matchingApi = new ApiEntity();
        matchingApi.setCrossId("api-cross-id");
        matchingApi.setId("api-id-1");

        PlanEntity firstMatchingPlan = new PlanEntity();
        firstMatchingPlan.setCrossId("plan-cross-id-1");
        firstMatchingPlan.setId("plan-id-1");

        PlanEntity secondMatchingPlan = new PlanEntity();
        secondMatchingPlan.setCrossId("plan-cross-id-2");
        secondMatchingPlan.setId("plan-id-2");

        when(apiService.findByEnvironmentIdAndCrossId("dev", "api-cross-id")).thenReturn(Optional.of(matchingApi));

        ExecutionContext executionContext = new ExecutionContext("default", "dev");

        when(planService.findByApi("api-id-1")).thenReturn(Set.of(firstMatchingPlan, secondMatchingPlan));

        ImportApiJsonNode newApiDefinition = cut.recalculateApiDefinitionIds(executionContext, new ImportApiJsonNode(apiDefinition));

        assertThat(newApiDefinition.getId()).isEqualTo("api-id-1");
        assertThat(newApiDefinition.getPlans().get(0).getId()).isEqualTo("plan-id-1");
        assertThat(newApiDefinition.getPlans().get(1).getId()).isEqualTo("plan-id-2");
    }

    @Test
    public void recalculateApiDefinitionIds_WithCrossId_should_not_recalculate_api_id_and_page_ids_preserving_hierarchy() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "api-id-1").put("crossId", "api-cross-id");

        apiDefinition.set(
            "pages",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "root-folder-id").put("crossId", "root-folder-cross-id"))
                .add(
                    mapper
                        .createObjectNode()
                        .put("id", "nested-folder-id")
                        .put("crossId", "nested-folder-cross-id")
                        .put("parentId", "root-folder-id")
                )
                .add(mapper.createObjectNode().put("id", "page-id").put("crossId", "page-cross-id").put("parentId", "nested-folder-id"))
        );

        ApiEntity matchingApi = new ApiEntity();
        matchingApi.setCrossId("api-cross-id");
        matchingApi.setId("api-id-1");

        PageEntity rootFolder = new PageEntity();
        rootFolder.setId("root-folder-id");
        rootFolder.setCrossId("root-folder-cross-id");

        PageEntity nestedFolder = new PageEntity();
        nestedFolder.setId("nested-folder-id");
        nestedFolder.setCrossId("nested-folder-cross-id");

        PageEntity page = new PageEntity();
        page.setId("page-id");
        page.setCrossId("page-cross-id");

        when(apiService.findByEnvironmentIdAndCrossId("dev", "api-cross-id")).thenReturn(Optional.of(matchingApi));
        when(pageService.findByApi("dev", "api-id-1")).thenReturn(List.of(rootFolder, nestedFolder, page));

        ImportApiJsonNode newApiDefinition = cut.recalculateApiDefinitionIds(
            new ExecutionContext("default", "dev"),
            new ImportApiJsonNode(apiDefinition)
        );

        assertThat(newApiDefinition.getId()).isEqualTo("api-id-1");
        assertThat(newApiDefinition.getPages().size()).isEqualTo(3);
        List<ImportJsonNodeWithIds> pages = newApiDefinition.getPages();
        assertThat(pages.get(0).getId()).isEqualTo("root-folder-id");
        assertThat(pages.get(1).getId()).isEqualTo("nested-folder-id");
        assertThat(pages.get(1).getParentId()).isEqualTo("root-folder-id");
        assertThat(pages.get(2).getId()).isEqualTo("page-id");
        assertThat(pages.get(2).getParentId()).isEqualTo("nested-folder-id");
    }

    @Test
    public void recalculateApiDefinitionIds_WithNoCrossId_should_recalculate_api_id_and_page_ids_preserving_hierarchy() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "my-api-1");

        apiDefinition.set(
            "pages",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "root-folder-id"))
                .add(mapper.createObjectNode().put("id", "nested-folder-id").put("parentId", "root-folder-id"))
                .add(mapper.createObjectNode().put("id", "page-id").put("parentId", "nested-folder-id"))
        );

        ImportApiJsonNode newApiDefinition = cut.recalculateApiDefinitionIds(
            new ExecutionContext("default", "uat"),
            new ImportApiJsonNode(apiDefinition)
        );

        assertThat(newApiDefinition.getId()).isEqualTo("e0a6482a-b8a7-3db4-a1b7-d36a462a9e38");
        assertThat(newApiDefinition.getPages().size()).isEqualTo(3);
        List<ImportJsonNodeWithIds> pages = newApiDefinition.getPages();
        assertThat(pages.get(0).getId()).isEqualTo("dfe1ae59-6f5f-330d-8ba0-f90d82f3e104");
        assertThat(pages.get(1).getId()).isEqualTo("74d13d70-6b3c-33ba-80a2-329848a0ad5f");
        assertThat(pages.get(1).getParentId()).isEqualTo("dfe1ae59-6f5f-330d-8ba0-f90d82f3e104");
        assertThat(pages.get(2).getId()).isEqualTo("7c16f23b-74d7-3465-84ea-8f67a5032637");
        assertThat(pages.get(2).getParentId()).isEqualTo("74d13d70-6b3c-33ba-80a2-329848a0ad5f");
    }

    @Test
    public void recalculateApiDefinitionIds_WithCrossId_should_generate_empty_ids_preserving_existing_id() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "api-id-1").put("crossId", "api-cross-id");

        apiDefinition.set(
            "plans",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "plan-id-1").put("crossId", "plan-cross-id"))
                .add(mapper.createObjectNode().put("name", "I have an empty ID").put("id", ""))
        );

        apiDefinition.set(
            "pages",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "page-id-1").put("crossId", "page-cross-id"))
                .add(mapper.createObjectNode().put("name", "I have no ID"))
        );

        ApiEntity matchingApi = new ApiEntity();
        matchingApi.setId("api-id-1");
        matchingApi.setCrossId("api-cross-id");

        PlanEntity matchingPlan = new PlanEntity();
        matchingPlan.setId("plan-id-1");
        matchingPlan.setCrossId("plan-cross-id");

        PageEntity matchingPage = new PageEntity();
        matchingPage.setId("page-id-1");
        matchingPage.setCrossId("page-cross-id");
        when(apiService.findByEnvironmentIdAndCrossId("dev", "api-cross-id")).thenReturn(Optional.of(matchingApi));
        when(pageService.findByApi("dev", "api-id-1")).thenReturn(List.of(matchingPage));

        ExecutionContext executionContext = new ExecutionContext("default", "dev");

        when(planService.findByApi("api-id-1")).thenReturn(Set.of(matchingPlan));

        ImportApiJsonNode newApiDefinition = cut.recalculateApiDefinitionIds(executionContext, new ImportApiJsonNode(apiDefinition));

        assertThat(newApiDefinition.getId()).isEqualTo("api-id-1");
        assertThat(newApiDefinition.getPlans().get(0).getId()).isEqualTo("plan-id-1");
        assertThat(newApiDefinition.getPlans().get(1).getId()).isNotEmpty();
        assertThat(newApiDefinition.getPlans().get(1).getJsonNode().get("name").asText()).isEqualTo("I have an empty ID");
        assertThat(newApiDefinition.getPages().get(0).getId()).isEqualTo("page-id-1");
        assertThat(newApiDefinition.getPages().get(1).getId()).isNotEmpty();
        assertThat(newApiDefinition.getPages().get(1).getJsonNode().get("name").asText()).isEqualTo("I have no ID");
    }

    @Test
    public void recalculateApiDefinitionIds_WithNoCrossId_should_generate_empty_ids_and_recalculate_existing_ids() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "api-id-1");

        apiDefinition.set(
            "plans",
            mapper.createArrayNode().add(mapper.createObjectNode().put("id", "plan-id-1")).add(mapper.createObjectNode().put("id", ""))
        );

        apiDefinition.set(
            "pages",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "page-id-1"))
                .add(mapper.createObjectNode().put("name", "no-id"))
        );

        ImportApiJsonNode newApiDefinition = cut.recalculateApiDefinitionIds(
            new ExecutionContext("default", "default"),
            new ImportApiJsonNode(apiDefinition)
        );

        assertThat(newApiDefinition.getId()).isEqualTo("ed5fbfe2-9cab-3306-9e51-24721d5b6e82");
        assertThat(newApiDefinition.getPlans().get(0).getId()).isEqualTo("cfc4cdf3-7ba7-387f-ac18-d846ce8b141c");
        assertThat(newApiDefinition.getPlans().get(1).getId()).isNotEmpty();
        assertThat(newApiDefinition.getPages().get(0).getId()).isEqualTo("7cfb0e92-2f22-35a6-b565-e38d45ac0de5");
        assertThat(newApiDefinition.getPages().get(1).getId()).isNotEmpty();
        assertThat(newApiDefinition.getPages().get(1).getJsonNode().get("name").asText()).isEqualTo("no-id");
    }
}
