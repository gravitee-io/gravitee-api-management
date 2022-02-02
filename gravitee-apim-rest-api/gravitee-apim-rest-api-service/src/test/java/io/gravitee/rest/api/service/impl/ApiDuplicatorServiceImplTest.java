/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.imports.ImportApiJsonNode;
import io.gravitee.rest.api.service.imports.ImportJsonNodeWithIds;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDuplicatorServiceImplTest {

    @InjectMocks
    protected ApiDuplicatorServiceImpl apiDuplicatorService;

    @Mock
    private ApiService apiService;

    @Mock
    private PageService pageService;

    @Mock
    private PlanService planService;

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void beforeClass() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        mapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));
    }

    @Test
    public void handleApiDefinitionIds_WithNoCrossId_should_recalculate_api_id_and_plans_id() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "my-api-1");

        apiDefinition.set(
            "plans",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "my-plan-id-1"))
                .add(mapper.createObjectNode().put("id", "my-plan-id-2"))
        );

        ImportApiJsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(new ImportApiJsonNode(apiDefinition), "uat");

        assertEquals("e0a6482a-b8a7-3db4-a1b7-d36a462a9e38", newApiDefinition.getId());
        assertEquals("393ed51c-285d-3097-82eb-2bff2903dc62", newApiDefinition.getPlans().get(0).getId());
        assertEquals("bff87514-39d4-331b-a531-73c021ecf627", newApiDefinition.getPlans().get(1).getId());
    }

    @Test
    public void handleApiDefinitionIds_WithNoCrossId_AndNoMatchingApi_should_recalculate_api_id_and_plans_id() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "api-id-1").put("crossId", "api-cross-id");

        apiDefinition.set(
            "plans",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "plan-id-1").put("crossId", "plan-cross-id-1"))
                .add(mapper.createObjectNode().put("id", "plan-id-2").put("crossId", "plan-cross-id-2"))
        );

        when(apiService.findByEnvironmentIdAndCrossId("uat", "api-cross-id")).thenReturn(Optional.empty());

        ImportApiJsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(new ImportApiJsonNode(apiDefinition), "uat");

        assertEquals("6bcde800-d5ae-3215-8413-cae196f9edfc", newApiDefinition.getId());
        assertEquals("5025bd5d-b1a5-35f5-813b-65bb902aa4e7", newApiDefinition.getPlans().get(0).getId());
        assertEquals("a4c0e8d8-ea8b-341b-9f7f-3ed456e77689", newApiDefinition.getPlans().get(1).getId());
    }

    @Test
    public void handleApiDefinitionIds_WithCrossId_should_not_recalculate_api_id_and_plans_ids() {
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
        when(planService.findByApi("api-id-1")).thenReturn(Set.of(firstMatchingPlan, secondMatchingPlan));

        ImportApiJsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(new ImportApiJsonNode(apiDefinition), "dev");

        assertEquals("api-id-1", newApiDefinition.getId());
        assertEquals("plan-id-1", newApiDefinition.getPlans().get(0).getId());
        assertEquals("plan-id-2", newApiDefinition.getPlans().get(1).getId());
    }

    @Test
    public void handleApiDefinitionIds_WithCrossId_should_not_recalculate_api_id_and_page_ids_preserving_hierarchy() {
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
        when(pageService.findByApi("api-id-1")).thenReturn(List.of(rootFolder, nestedFolder, page));

        ImportApiJsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(new ImportApiJsonNode(apiDefinition), "dev");

        assertEquals("api-id-1", newApiDefinition.getId());
        assertEquals(3, newApiDefinition.getPages().size());
        List<ImportJsonNodeWithIds> pages = newApiDefinition.getPages();
        assertEquals("root-folder-id", pages.get(0).getId());
        assertEquals("nested-folder-id", pages.get(1).getId());
        assertEquals("root-folder-id", pages.get(1).getParentId());
        assertEquals("page-id", pages.get(2).getId());
        assertEquals("nested-folder-id", pages.get(2).getParentId());
    }

    @Test
    public void handleApiDefinitionIds_WithNoCrossId_should_recalculate_api_id_and_page_ids_preserving_hierarchy() {
        ObjectNode apiDefinition = mapper.createObjectNode().put("id", "my-api-1");

        apiDefinition.set(
            "pages",
            mapper
                .createArrayNode()
                .add(mapper.createObjectNode().put("id", "root-folder-id"))
                .add(mapper.createObjectNode().put("id", "nested-folder-id").put("parentId", "root-folder-id"))
                .add(mapper.createObjectNode().put("id", "page-id").put("parentId", "nested-folder-id"))
        );

        ImportApiJsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(new ImportApiJsonNode(apiDefinition), "uat");

        assertEquals("e0a6482a-b8a7-3db4-a1b7-d36a462a9e38", newApiDefinition.getId());
        assertEquals(3, newApiDefinition.getPages().size());
        List<ImportJsonNodeWithIds> pages = newApiDefinition.getPages();
        assertEquals("cbcf3a8b-ebe3-3bff-aed6-4235201f4851", pages.get(0).getId());
        assertEquals("1563e196-37f7-3500-adb4-65d2efe15feb", pages.get(1).getId());
        assertEquals("cbcf3a8b-ebe3-3bff-aed6-4235201f4851", pages.get(1).getParentId());
        assertEquals("91c32be3-e15c-392c-94f2-a509ec3ba69a", pages.get(2).getId());
        assertEquals("1563e196-37f7-3500-adb4-65d2efe15feb", pages.get(2).getParentId());
    }

    @Test
    public void handleApiDefinitionIds_WithCrossId_should_generate_empty_ids_preserving_existing_id() {
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
        when(pageService.findByApi("api-id-1")).thenReturn(List.of(matchingPage));
        when(planService.findByApi("api-id-1")).thenReturn(Set.of(matchingPlan));

        ImportApiJsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(new ImportApiJsonNode(apiDefinition), "dev");

        assertEquals("api-id-1", newApiDefinition.getId());
        assertEquals("plan-id-1", newApiDefinition.getPlans().get(0).getId());
        assertTrue(isNotEmpty(newApiDefinition.getPlans().get(1).getId()));
        assertEquals("I have an empty ID", newApiDefinition.getPlans().get(1).getJsonNode().get("name").asText());
        assertEquals("page-id-1", newApiDefinition.getPages().get(0).getId());
        assertTrue(isNotEmpty(newApiDefinition.getPages().get(1).getId()));
        assertEquals("I have no ID", newApiDefinition.getPages().get(1).getJsonNode().get("name").asText());
    }

    @Test
    public void handleApiDefinitionIds_WithNoCrossId_should_generate_empty_ids_and_recalculate_existing_ids() {
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

        ImportApiJsonNode newApiDefinition = apiDuplicatorService.handleApiDefinitionIds(new ImportApiJsonNode(apiDefinition), "default");

        assertEquals("ed5fbfe2-9cab-3306-9e51-24721d5b6e82", newApiDefinition.getId());
        assertEquals("de653244-912e-384e-a60d-9f8625f18c81", newApiDefinition.getPlans().get(0).getId());
        assertTrue(isNotEmpty(newApiDefinition.getPlans().get(1).getId()));
        assertEquals("943a8642-c5c1-336e-b6c9-44ea4ff7e1f9", newApiDefinition.getPages().get(0).getId());
        assertTrue(isNotEmpty(newApiDefinition.getPages().get(1).getId()));
        assertEquals("no-id", newApiDefinition.getPages().get(1).getJsonNode().get("name").asText());
    }
}
