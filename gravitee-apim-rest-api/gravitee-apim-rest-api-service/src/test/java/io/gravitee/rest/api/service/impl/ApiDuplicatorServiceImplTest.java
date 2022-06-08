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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.imports.ImportApiJsonNode;
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
    public void shouldGetOnlyUserMembers() {
        MemberEntity userMember = new MemberEntity();
        userMember.setId("user-id");
        userMember.setType(MembershipMemberType.USER);
        userMember.setRoles(Collections.singletonList(new RoleEntity()));

        MemberEntity groupMember = new MemberEntity();
        groupMember.setId("group-id");
        groupMember.setType(MembershipMemberType.GROUP);
        groupMember.setRoles(Collections.singletonList(new RoleEntity()));

        when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID))
            .thenReturn(Set.of(userMember, groupMember));

        UserEntity userEntity = new UserEntity();
        userEntity.setId("user-id");
        userEntity.setSource("user-source");
        userEntity.setSourceId("user-source-id");

        when(userService.findById(GraviteeContext.getExecutionContext(), "user-id")).thenReturn(userEntity);

        final Set<ApiDuplicatorServiceImpl.MemberToImport> apiCurrentMembers = apiDuplicatorService.getAPICurrentMembers(
            GraviteeContext.getExecutionContext(),
            API_ID
        );

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

        final List<String> rolesToImport = apiDuplicatorService.getRoleIdsToImport(GraviteeContext.getExecutionContext(), memberToImport);

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

        when(roleService.findByScopeAndName(RoleScope.API, "user-role", GraviteeContext.getExecutionContext().getOrganizationId()))
            .thenReturn(Optional.of(userRoleEntity));

        final List<String> rolesToImport = apiDuplicatorService.getRoleIdsToImport(GraviteeContext.getExecutionContext(), memberToImport);

        assertNotNull(rolesToImport);
        assertEquals(1, rolesToImport.size());
        assertEquals("user-role", rolesToImport.get(0));
    }

    @Test
    public void shouldGetRolesToImport_fromRolesAndRole() {
        RoleEntity userRoleEntity1 = new RoleEntity();
        userRoleEntity1.setId("user-role-1-id");
        userRoleEntity1.setName("user-role-1-name");
        RoleEntity userRoleEntity2 = new RoleEntity();
        userRoleEntity2.setId("user-role-2-id");
        userRoleEntity2.setName("user-role-2-name");

        when(roleService.findByScopeAndName(RoleScope.API, "user-role-1-name", GraviteeContext.getExecutionContext().getOrganizationId()))
            .thenReturn(Optional.of(userRoleEntity1));

        ApiDuplicatorServiceImpl.MemberToImport memberToImport = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImport.setSource("user-source");
        memberToImport.setSourceId("user-sourceId");
        memberToImport.setRoles(List.of(userRoleEntity2.getId(), "unexisting_role"));
        memberToImport.setRole(userRoleEntity1.getName());

        final List<String> rolesToImport = apiDuplicatorService.getRoleIdsToImport(GraviteeContext.getExecutionContext(), memberToImport);

        assertNotNull(rolesToImport);
        assertEquals(3, rolesToImport.size());
        assertTrue(rolesToImport.contains("user-role-1-id"));
        assertTrue(rolesToImport.contains("user-role-2-id"));
        // The check on role existence will occur when creating the membership. So this method returns every roleId available in the definition.
        assertTrue(rolesToImport.contains("unexisting_role"));
    }

    /*
     * Test update methods
     */
    private static final String IMPORT_FILES_FOLDER = "io/gravitee/rest/api/management/service/import/";

    // Pages

    @Test
    public void shouldNotUpdatePagesIfNoPage() throws IOException {
        ImportApiJsonNode noPagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.null.json");
        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), API_ID, noPagesNode);

        verifyNoInteractions(pageService);
    }

    @Test
    public void shouldNotUpdatePagesIfEmptyPlan() throws IOException {
        ImportApiJsonNode emptyPagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.empty.json");
        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), API_ID, emptyPagesNode);

        verifyNoInteractions(pageService);
    }

    @Test
    public void shouldUpdatePages() throws IOException {
        ImportApiJsonNode pagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.default.json");
        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), API_ID, pagesNode);

        verify(pageService, times(1))
            .createOrUpdatePages(eq(GraviteeContext.getExecutionContext()), argThat(pageEntities -> pageEntities.size() == 2), eq(API_ID));
    }

    // Plans
    @Test
    public void shouldNotUpdatePlansIfNoPlan() throws IOException {
        ImportApiJsonNode noPlansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.null.json");
        apiDuplicatorService.createOrUpdatePlans(GraviteeContext.getExecutionContext(), API_ID, noPlansNode);

        verifyNoInteractions(planService);
    }

    @Test
    public void shouldNotUpdatePlansIfEmptyPlan() throws IOException {
        ImportApiJsonNode emptyPlansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.empty.json");
        apiDuplicatorService.createOrUpdatePlans(GraviteeContext.getExecutionContext(), API_ID, emptyPlansNode);

        verifyNoInteractions(planService);
    }

    @Test
    public void shouldUpdatePlans() throws IOException {
        ImportApiJsonNode plansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.default.json");
        apiDuplicatorService.createOrUpdatePlans(GraviteeContext.getExecutionContext(), API_ID, plansNode);

        verify(planService, times(1)).findByApi(GraviteeContext.getExecutionContext(), API_ID);
        verify(planService, never()).delete(any(), any());
        verify(planService, times(2)).createOrUpdatePlan(eq(GraviteeContext.getExecutionContext()), any(PlanEntity.class));
    }

    // Metadata
    @Test
    public void shouldNotUpdateMetadataIfNoMetadata() throws IOException {
        ImportApiJsonNode noMetadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.null.json");
        apiDuplicatorService.createOrUpdateMetadata(GraviteeContext.getExecutionContext(), API_ID, noMetadataNode);

        verifyNoInteractions(apiMetadataService);
    }

    @Test
    public void shouldNotUpdateMetadataIfEmptyMetadata() throws IOException {
        ImportApiJsonNode emptyMetadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.empty.json");
        apiDuplicatorService.createOrUpdateMetadata(GraviteeContext.getExecutionContext(), API_ID, emptyMetadataNode);

        verifyNoInteractions(apiMetadataService);
    }

    @Test
    public void shouldUpdateMetadata() throws IOException {
        ImportApiJsonNode metadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.default.json");
        apiDuplicatorService.createOrUpdateMetadata(GraviteeContext.getExecutionContext(), API_ID, metadataNode);

        verify(apiMetadataService, times(2)).update(eq(GraviteeContext.getExecutionContext()), any(UpdateApiMetadataEntity.class));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementExceptionWithMetadata() throws IOException {
        ImportApiJsonNode metadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.default.json");
        when(apiMetadataService.update(eq(GraviteeContext.getExecutionContext()), any())).thenThrow(new RuntimeException("fake exception"));

        apiDuplicatorService.createOrUpdateMetadata(GraviteeContext.getExecutionContext(), API_ID, metadataNode);
    }

    /*
     * Util methods
     */
    private ImportApiJsonNode loadTestNode(String resourceName) throws IOException {
        URL url = Resources.getResource(resourceName);
        String toBeImport = Resources.toString(url, Charsets.UTF_8);

        return new ImportApiJsonNode(objectMapper.readTree(toBeImport));
    }
}
