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

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.rest.api.model.ImportPageEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageSourceEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateApiMetadataEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
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
import java.util.UUID;
import org.junit.Before;
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
    public static final String ORGANIZATION_ID = "DEFAULT";
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

    @Mock
    private ApiEntity apiEntity;

    @Spy
    private ObjectMapper objectMapper = (new ServiceConfiguration()).objectMapper();

    @Before
    public void setup() {
        when(apiEntity.getId()).thenReturn(API_ID);
    }

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
        assertNotNull(memberToImport.getRoles());
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
    public void shouldGetRolesToImport_fromRole() {
        RoleEntity userRoleEntity = new RoleEntity();
        userRoleEntity.setId("user-role");

        ApiDuplicatorServiceImpl.MemberToImport memberToImport = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImport.setSource("user-source");
        memberToImport.setSourceId("user-sourceId");
        memberToImport.setRoles(List.of(userRoleEntity.getId()));

        final List<String> rolesToImport = apiDuplicatorService.getRoleIdsToImport(GraviteeContext.getExecutionContext(), memberToImport);

        assertNotNull(rolesToImport);
        assertEquals(1, rolesToImport.size());
        assertEquals("user-role", rolesToImport.get(0));
    }

    @Test
    public void shouldGetRolesToImport_fromRoles() {
        RoleEntity userRoleEntity1 = new RoleEntity();
        userRoleEntity1.setId(UUID.randomUUID().toString());
        userRoleEntity1.setName("user-role-1-name");

        RoleEntity userRoleEntity2 = new RoleEntity();
        userRoleEntity2.setId(UUID.randomUUID().toString());
        userRoleEntity2.setName("user-role-2-name");

        ApiDuplicatorServiceImpl.MemberToImport memberToImport = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImport.setSource("user-source");
        memberToImport.setSourceId("user-sourceId");
        memberToImport.setRoles(List.of(userRoleEntity2.getId(), userRoleEntity1.getName(), "unexisting_role"));

        final List<String> rolesToImport = apiDuplicatorService.getRoleIdsToImport(GraviteeContext.getExecutionContext(), memberToImport);

        assertNotNull(rolesToImport);
        assertEquals(3, rolesToImport.size());
        assertTrue(rolesToImport.contains("user-role-1-name"));
        assertTrue(rolesToImport.contains(userRoleEntity2.getId()));
        // The check on role existence will occur when creating the membership. So this method returns every roleId available in the definition.
        assertTrue(rolesToImport.contains("unexisting_role"));
    }

    @Test
    public void shouldGetRolesToImport_fromRolesAndRole() {
        RoleEntity userRoleEntity1 = new RoleEntity();
        userRoleEntity1.setId(UUID.randomUUID().toString());
        userRoleEntity1.setName("user-role-1-name");

        RoleEntity userRoleEntity2 = new RoleEntity();
        userRoleEntity2.setId(UUID.randomUUID().toString());
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
        assertTrue(rolesToImport.contains(userRoleEntity1.getId()));
        assertTrue(rolesToImport.contains(userRoleEntity2.getId()));
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
        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), apiEntity, noPagesNode);

        verifyNoInteractions(pageService);
    }

    @Test
    public void shouldNotUpdatePagesIfEmptyPlan() throws IOException {
        ImportApiJsonNode emptyPagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.empty.json");
        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), apiEntity, emptyPagesNode);

        verifyNoInteractions(pageService);
    }

    @Test
    public void shouldUpdatePages() throws IOException {
        ImportApiJsonNode pagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.default.json");
        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), apiEntity, pagesNode);

        verify(pageService, times(1))
            .createOrUpdatePages(eq(GraviteeContext.getExecutionContext()), argThat(pageEntities -> pageEntities.size() == 2), eq(API_ID));
    }

    @Test
    public void shouldDeleteExistingPagesBeforeUpdateIfKubernetesOrigin() throws IOException {
        var pagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.kubernetes.json");
        var deletedPageId = "not-in-the-import";
        var existingPage = new PageEntity();

        existingPage.setId(deletedPageId);

        when(apiEntity.getId()).thenReturn(API_ID);

        when(pageService.findByApi("DEFAULT", API_ID)).thenReturn(List.of(existingPage));

        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), apiEntity, pagesNode);

        verify(pageService, times(1)).delete(GraviteeContext.getExecutionContext(), deletedPageId);

        verify(pageService, times(1))
            .createOrUpdatePages(eq(GraviteeContext.getExecutionContext()), argThat(pageEntities -> pageEntities.size() == 2), eq(API_ID));
    }

    @Test
    public void shouldImportRootPageIfKubernetesOrigin() throws IOException {
        var node = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages-root.kubernetes.json");

        when(apiEntity.getId()).thenReturn(API_ID);

        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), apiEntity, node);

        verify(pageService, times(1))
            .importFiles(eq(GraviteeContext.getExecutionContext()), eq(API_ID), argThat(page -> page.getSource() != null));

        verify(pageService, times(1)).createOrUpdatePages(GraviteeContext.getExecutionContext(), List.of(), API_ID);
    }

    @Test
    public void shouldDeleteAllPagesBeforeUpdateIfKubernetesOrigin() throws IOException {
        var pagesNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.pages.empty.kubernetes.json");

        when(apiEntity.getId()).thenReturn(API_ID);

        apiDuplicatorService.createOrUpdatePages(GraviteeContext.getExecutionContext(), apiEntity, pagesNode);

        verify(pageService, times(1)).deleteAllByApi(GraviteeContext.getExecutionContext(), API_ID);
    }

    // Plans
    @Test
    public void shouldNotUpdatePlansIfNoPlan() throws IOException {
        ImportApiJsonNode noPlansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.null.json");
        apiDuplicatorService.createOrUpdatePlans(GraviteeContext.getExecutionContext(), apiEntity, noPlansNode);

        verifyNoInteractions(planService);
    }

    @Test
    public void shouldDeleteAllExistingPlansIfEmptyPlan() throws IOException {
        ImportApiJsonNode emptyPlansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.empty.json");
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId("existing-plan-id");
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(singleton(planEntity));
        apiDuplicatorService.createOrUpdatePlans(GraviteeContext.getExecutionContext(), apiEntity, emptyPlansNode);

        verify(planService, times(1)).findByApi(GraviteeContext.getExecutionContext(), API_ID);
        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), "existing-plan-id");
        verify(planService, never()).createOrUpdatePlan(any(), any());
    }

    @Test
    public void shouldUpdatePlans() throws IOException {
        ImportApiJsonNode plansNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.plans.default.json");
        apiDuplicatorService.createOrUpdatePlans(GraviteeContext.getExecutionContext(), apiEntity, plansNode);

        verify(planService, times(1)).findByApi(GraviteeContext.getExecutionContext(), API_ID);
        verify(planService, never()).delete(any(), any());
        verify(planService, times(2)).createOrUpdatePlan(eq(GraviteeContext.getExecutionContext()), any(PlanEntity.class));
    }

    // Metadata
    @Test
    public void shouldNotUpdateMetadataIfNoMetadata() throws IOException {
        ImportApiJsonNode noMetadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.null.json");
        apiDuplicatorService.createOrUpdateMetadata(GraviteeContext.getExecutionContext(), apiEntity, noMetadataNode);

        verifyNoInteractions(apiMetadataService);
    }

    @Test
    public void shouldNotUpdateMetadataIfEmptyMetadata() throws IOException {
        ImportApiJsonNode emptyMetadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.empty.json");
        apiDuplicatorService.createOrUpdateMetadata(GraviteeContext.getExecutionContext(), apiEntity, emptyMetadataNode);

        verifyNoInteractions(apiMetadataService);
    }

    @Test
    public void shouldUpdateMetadata() throws IOException {
        ImportApiJsonNode metadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.default.json");
        apiDuplicatorService.createOrUpdateMetadata(GraviteeContext.getExecutionContext(), apiEntity, metadataNode);

        verify(apiMetadataService, times(2)).update(eq(GraviteeContext.getExecutionContext()), any(UpdateApiMetadataEntity.class));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementExceptionWithMetadata() throws IOException {
        ImportApiJsonNode metadataNode = loadTestNode(IMPORT_FILES_FOLDER + "import-api.metadata.default.json");
        when(apiMetadataService.update(eq(GraviteeContext.getExecutionContext()), any())).thenThrow(new RuntimeException("fake exception"));

        apiDuplicatorService.createOrUpdateMetadata(GraviteeContext.getExecutionContext(), apiEntity, metadataNode);
    }

    @Test
    public void addOrUpdateMembers_shouldAddRolesUsingMembershipService_butNotFailIfItThrowsException() {
        var currentPo = new ApiDuplicatorServiceImpl.MemberToImport();
        currentPo.setSource("source");
        currentPo.setSourceId("currentPo-sourceId");

        var memberToImport = new ApiDuplicatorServiceImpl.MemberToImport();
        memberToImport.setRoles(List.of("member-role"));
        memberToImport.setSource("source");
        memberToImport.setSourceId("memberToImport-sourceId");

        when(userService.findBySource(any(), any(), any(), eq(false))).thenReturn(new UserEntity());

        apiDuplicatorService.addOrUpdateMembers(
            GraviteeContext.getExecutionContext(),
            API_ID,
            "po-role-id",
            currentPo,
            memberToImport,
            List.of("new-role"),
            false
        );

        verify(membershipService).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any(), any(), any());
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
