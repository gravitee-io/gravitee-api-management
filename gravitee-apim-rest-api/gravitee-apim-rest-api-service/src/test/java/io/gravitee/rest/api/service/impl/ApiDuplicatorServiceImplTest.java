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

import static java.util.Collections.singleton;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDuplicatorServiceImplTest {

    private static final String API_ID = "id-api";
    public static final String ENVIRONMENT_ID = "DEFAULT";

    @InjectMocks
    protected ApiDuplicatorServiceImpl apiDuplicatorService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private PageService pageService;

    @Mock
    private PlanService planService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Spy
    private ObjectMapper objectMapper = (new ServiceConfiguration()).objectMapper();

    /*
     * Test helper methods
     */
    @Test
    public void preprocessApiDefinitionUpdatingIds_should_regenerate_plans_id() throws JsonProcessingException {
        JsonNode jsonNode = new ObjectMapper()
        .readTree("{\"id\":\"my-api-id\",\"plans\":[{\"id\":\"my-plan-id1\"},{\"id\":\"my-plan-id2\"}]}");

        String newApiDefinition = apiDuplicatorService.preprocessApiDefinitionUpdatingIds(jsonNode, "default");

        // plans id have been regenerated in jsonNodes and stringified api definition
        assertEquals("b53f77ba-380a-34e2-8c5d-5c60847f0de4", jsonNode.get("plans").get(0).get("id").asText());
        assertEquals("1b552fa9-bb70-3bbc-b925-9e0b6dd4a35d", jsonNode.get("plans").get(1).get("id").asText());
        assertEquals(
            "{\"id\":\"my-api-id\",\"plans\":[{\"id\":\"b53f77ba-380a-34e2-8c5d-5c60847f0de4\"},{\"id\":\"1b552fa9-bb70-3bbc-b925-9e0b6dd4a35d\"}]}",
            newApiDefinition
        );
    }

    @Test
    public void shouldGetOnlyUserMembers() {
        MemberEntity userMember = new MemberEntity();
        userMember.setId("user-id");
        userMember.setType(MembershipMemberType.USER);
        userMember.setRoles(Collections.singletonList(new RoleEntity()));

        MemberEntity groupMember = new MemberEntity();
        groupMember.setId("group-id");
        groupMember.setType(MembershipMemberType.GROUP);
        groupMember.setRoles(Collections.singletonList(new RoleEntity()));

        when(membershipService.getMembersByReference(MembershipReferenceType.API, API_ID)).thenReturn(Set.of(userMember, groupMember));

        UserEntity userEntity = new UserEntity();
        userEntity.setId("user-id");
        userEntity.setSource("user-source");
        userEntity.setSourceId("user-source-id");

        when(userService.findById("user-id")).thenReturn(userEntity);

        final Set<ApiDuplicatorServiceImpl.MemberToImport> apiCurrentMembers = apiDuplicatorService.getAPICurrentMembers(API_ID);

        assertEquals(1, apiCurrentMembers.size());
        final ApiDuplicatorServiceImpl.MemberToImport memberToImport = apiCurrentMembers.iterator().next();
        assertEquals("user-source", memberToImport.getSource());
        assertEquals("user-source-id", memberToImport.getSourceId());
        assertNull(memberToImport.getRole());
        assertEquals(1, memberToImport.getRoles().size());
    }

    @Test
    public void shouldFindExistingMemberWithSameRole() {
        ApiDuplicatorServiceImpl.MemberToImport existingMember = new ApiDuplicatorServiceImpl.MemberToImport();
        existingMember.setSource("user-source");
        existingMember.setSourceId("user-sourceId");
        existingMember.setRoles(Collections.singletonList("user-role"));

        assertTrue(apiDuplicatorService.isPresentWithSameRole(Set.of(existingMember), existingMember));
    }

    @Test
    public void shouldNotFindExistingMemberWithSameRole_no_existing_member() {
        ApiDuplicatorServiceImpl.MemberToImport existingMember = new ApiDuplicatorServiceImpl.MemberToImport();
        existingMember.setSource("user-source");
        existingMember.setSourceId("user-sourceId");
        existingMember.setRoles(Collections.singletonList("user-role"));

        ApiDuplicatorServiceImpl.MemberToImport memberToImportWithNoRole = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImportWithNoRole.setSource("user-source");
        memberToImportWithNoRole.setSourceId("user-sourceId");

        ApiDuplicatorServiceImpl.MemberToImport memberToImportWithEmptyRole = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImportWithNoRole.setSource("user-source");
        memberToImportWithNoRole.setSourceId("user-sourceId");
        memberToImportWithNoRole.setRoles(Collections.emptyList());

        ApiDuplicatorServiceImpl.MemberToImport anotherMember = new ApiDuplicatorServiceImpl.MemberToImport();
        anotherMember.setSource("another-user-source");
        anotherMember.setSourceId("another-user-sourceId");
        anotherMember.setRoles(Collections.singletonList("user-role"));

        assertFalse(apiDuplicatorService.isPresentWithSameRole(Set.of(existingMember), memberToImportWithNoRole));
        assertFalse(apiDuplicatorService.isPresentWithSameRole(Set.of(existingMember), memberToImportWithEmptyRole));
        assertFalse(apiDuplicatorService.isPresentWithSameRole(Set.of(existingMember), anotherMember));
    }

    @Test
    public void shouldGetRolesToImport_fromRoles() {
        RoleEntity userRoleEntity = new RoleEntity();
        userRoleEntity.setId("user-role");

        ApiDuplicatorServiceImpl.MemberToImport memberToImport = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImport.setSource("user-source");
        memberToImport.setSourceId("user-sourceId");
        memberToImport.setRoles(Collections.singletonList(userRoleEntity.getId()));

        when(roleService.findByScopeAndName(RoleScope.API, "user-role")).thenReturn(Optional.of(userRoleEntity));

        final List<String> rolesToImport = apiDuplicatorService.getRolesToImport(memberToImport);

        assertNotNull(rolesToImport);
        assertEquals(1, rolesToImport.size());
        assertEquals("user-role", rolesToImport.get(0));
    }

    @Test
    public void shouldGetRolesToImport_fromRole() {
        RoleEntity userRoleEntity = new RoleEntity();
        userRoleEntity.setId("user-role");

        ApiDuplicatorServiceImpl.MemberToImport memberToImport = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImport.setSource("user-source");
        memberToImport.setSourceId("user-sourceId");
        memberToImport.setRole(userRoleEntity.getId());

        when(roleService.findByScopeAndName(RoleScope.API, "user-role")).thenReturn(Optional.of(userRoleEntity));

        final List<String> rolesToImport = apiDuplicatorService.getRolesToImport(memberToImport);

        assertNotNull(rolesToImport);
        assertEquals(1, rolesToImport.size());
        assertEquals("user-role", rolesToImport.get(0));
    }

    @Test
    public void shouldGetRolesToImport_fromRolesAndRole() {
        RoleEntity userRoleEntity1 = new RoleEntity();
        userRoleEntity1.setId("user-role-1");
        RoleEntity userRoleEntity2 = new RoleEntity();
        userRoleEntity2.setId("user-role-2");

        when(roleService.findByScopeAndName(RoleScope.API, "user-role-1")).thenReturn(Optional.of(userRoleEntity1));
        when(roleService.findByScopeAndName(RoleScope.API, "user-role-2")).thenReturn(Optional.of(userRoleEntity2));
        when(roleService.findByScopeAndName(RoleScope.API, "unexisting_role")).thenReturn(Optional.empty());

        ApiDuplicatorServiceImpl.MemberToImport memberToImport = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImport.setSource("user-source");
        memberToImport.setSourceId("user-sourceId");
        memberToImport.setRoles(List.of(userRoleEntity2.getId(), "unexisting_role"));
        memberToImport.setRole(userRoleEntity1.getId());

        final List<String> rolesToImport = apiDuplicatorService.getRolesToImport(memberToImport);

        assertNotNull(rolesToImport);
        assertEquals(2, rolesToImport.size());
        assertEquals("user-role-1", rolesToImport.get(0));
        assertEquals("user-role-2", rolesToImport.get(1));
    }

    /*
     * Test update methods
     */
    private static final String IMPORT_FILES_FOLDER = "io/gravitee/rest/api/management/service/import/";

    // Pages

    @Test
    public void shouldNotUpdatePagesIfNoPage_APIcreationMode() throws IOException {
        JsonNode noPagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.null.json");
        apiDuplicatorService.updatePlans(API_ID, noPagesNode, ENVIRONMENT_ID, false);

        verifyNoInteractions(pageService);
    }

    @Test
    public void shouldNotUpdatePagesIfNoPlan_APIUpdateMode() throws IOException {
        JsonNode noPagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.null.json");
        apiDuplicatorService.updatePages(API_ID, noPagesNode, ENVIRONMENT_ID, true);

        verifyNoInteractions(pageService);
    }

    @Test
    public void shouldNotUpdatePagesIfEmptyPlan_APIcreationMode() throws IOException {
        JsonNode emptyPagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.empty.json");
        apiDuplicatorService.updatePages(API_ID, emptyPagesNode, ENVIRONMENT_ID, false);

        verifyNoInteractions(pageService);
    }

    @Test
    public void shouldNotUpdatePagesIfEmptyPlan_APIupdateMode() throws IOException {
        JsonNode emptyPagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.empty.json");
        apiDuplicatorService.updatePages(API_ID, emptyPagesNode, ENVIRONMENT_ID, true);

        verifyNoInteractions(pageService);
    }

    @Test
    public void shouldUpdatePages_APIcreationMode() throws IOException {
        JsonNode pagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.default.json");
        apiDuplicatorService.updatePages(API_ID, pagesNode, ENVIRONMENT_ID, false);

        verify(pageService, never()).createOrUpdatePages(any(), eq(ENVIRONMENT_ID), eq(API_ID));
        verify(pageService, times(1)).duplicatePages(argThat(pageEntities -> pageEntities.size() == 2), eq(ENVIRONMENT_ID), eq(API_ID));
    }

    @Test
    public void shouldUpdatePages_APIUpdateMode() throws IOException {
        JsonNode pagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.default.json");
        apiDuplicatorService.updatePages(API_ID, pagesNode, ENVIRONMENT_ID, true);

        verify(pageService, times(1))
            .createOrUpdatePages(argThat(pageEntities -> pageEntities.size() == 2), eq(ENVIRONMENT_ID), eq(API_ID));
        verify(pageService, never()).duplicatePages(any(), eq(ENVIRONMENT_ID), eq(API_ID));
    }

    // Plans
    @Test
    public void shouldNotUpdatePlansIfNoPlan_APIcreationMode() throws IOException {
        JsonNode noPlansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.null.json");
        apiDuplicatorService.updatePlans(API_ID, noPlansNode, ENVIRONMENT_ID, false);

        verifyNoInteractions(planService);
    }

    @Test
    public void shouldNotUpdatePlansIfNoPlan_APIUpdateMode() throws IOException {
        JsonNode noPlansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.null.json");
        apiDuplicatorService.updatePlans(API_ID, noPlansNode, ENVIRONMENT_ID, true);

        verifyNoInteractions(planService);
    }

    @Test
    public void shouldNotUpdatePlansIfEmptyPlan_APIcreationMode() throws IOException {
        JsonNode emptyPlansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.empty.json");
        apiDuplicatorService.updatePlans(API_ID, emptyPlansNode, ENVIRONMENT_ID, false);

        verifyNoInteractions(planService);
    }

    @Test
    public void shouldDeleteAllExistingPlansIfEmptyPlan_APIupdateMode() throws IOException {
        JsonNode emptyPlansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.empty.json");
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId("existing-plan-id");
        when(planService.findByApi(API_ID)).thenReturn(singleton(planEntity));
        apiDuplicatorService.updatePlans(API_ID, emptyPlansNode, ENVIRONMENT_ID, true);

        verify(planService, times(1)).findByApi(API_ID);
        verify(planService, times(1)).delete("existing-plan-id");
        verify(planService, never()).createOrUpdatePlan(any(), any());
    }

    @Test
    public void shouldUpdatePlans_APIcreationMode() throws IOException {
        JsonNode plansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.default.json");
        apiDuplicatorService.updatePlans(API_ID, plansNode, ENVIRONMENT_ID, false);

        verify(planService, never()).findByApi(API_ID);
        verify(planService, never()).delete(any());
        verify(planService, times(2)).createOrUpdatePlan(any(PlanEntity.class), eq(ENVIRONMENT_ID));
    }

    @Test
    public void shouldUpdatePlans_APIUpdateMode() throws IOException {
        JsonNode plansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.default.json");
        PlanEntity existingPlanInImport = new PlanEntity();
        existingPlanInImport.setId("plan-id");
        PlanEntity unExistingPlanInImport = new PlanEntity();
        unExistingPlanInImport.setId("another-plan-id");
        when(planService.findByApi(API_ID)).thenReturn(Set.of(existingPlanInImport, unExistingPlanInImport));

        apiDuplicatorService.updatePlans(API_ID, plansNode, ENVIRONMENT_ID, true);

        verify(planService, times(1)).findByApi(API_ID);
        verify(planService, times(1)).delete(any());
        verify(planService, times(2)).createOrUpdatePlan(any(PlanEntity.class), eq(ENVIRONMENT_ID));
    }

    // Metadata
    @Test
    public void shouldNotUpdateMetadataIfNoMetadata() throws IOException {
        JsonNode noMetadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.null.json");
        apiDuplicatorService.updateMetadata(API_ID, noMetadataNode);

        verifyNoInteractions(apiMetadataService);
    }

    @Test
    public void shouldNotUpdateMetadataIfEmptyMetadata() throws IOException {
        JsonNode emptyMetadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.empty.json");
        apiDuplicatorService.updateMetadata(API_ID, emptyMetadataNode);

        verifyNoInteractions(apiMetadataService);
    }

    @Test
    public void shouldUpdateMetadata() throws IOException {
        JsonNode metadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.default.json");
        apiDuplicatorService.updateMetadata(API_ID, metadataNode);

        verify(apiMetadataService, times(2)).update(any(UpdateApiMetadataEntity.class));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementExceptionWithMetadata() throws IOException {
        JsonNode metadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.default.json");
        when(apiMetadataService.update(any())).thenThrow(new RuntimeException("fake exception"));

        apiDuplicatorService.updateMetadata(API_ID, metadataNode);
    }

    /*
     * Util methods
     */
    private JsonNode loadTestNode(String resourceName) throws IOException {
        URL url = Resources.getResource(resourceName);
        String toBeImport = Resources.toString(url, Charsets.UTF_8);

        return objectMapper.readTree(toBeImport);
    }
}
