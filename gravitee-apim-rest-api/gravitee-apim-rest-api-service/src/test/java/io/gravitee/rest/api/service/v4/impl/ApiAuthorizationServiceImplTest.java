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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DEFINITION_VERSION;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.*;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiAuthorizationServiceImplTest {

    private static final String USER_NAME = "myUser";

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private Api api;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private CategoryService categoryService;

    private ApiAuthorizationService apiAuthorizationService;

    @Before
    public void setUp() {
        GraviteeContext.cleanContext();

        apiAuthorizationService =
            new ApiAuthorizationServiceImpl(
                apiRepository,
                categoryService,
                membershipService,
                roleService,
                applicationService,
                groupService,
                subscriptionService,
                primaryOwnerService,
                searchEngineService
            );
    }

    @Test
    public void shouldNotManageApiWithNullRole() {
        assertThat(apiAuthorizationService.canManageApi(null)).isFalse();
    }

    @Test
    public void shouldNotManageApiWithNotApiRole() {
        assertThat(apiAuthorizationService.canManageApi(new RoleEntity())).isFalse();
    }

    @Test
    public void shouldFindIdsByUser() {
        final String userRoleId = "API_USER";
        final String poRoleId = "API_PRIMARY_OWNER";

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId(userRoleId);
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        when(roleService.findById(userRoleId)).thenReturn(userRole);
        when(api.getId()).thenReturn("api-1");
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(new ApiCriteria.Builder().environmentId("DEFAULT").ids("api-1").build());

        when(apiRepository.searchIds(eq(apiCriteriaList), any(), any())).thenReturn(new Page<>(List.of("api-1"), 0, 1, 1));

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(USER_NAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(userRoleId);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        final Set<String> apis = apiAuthorizationService.findIdsByUser(GraviteeContext.getExecutionContext(), USER_NAME, true);

        assertThat(apis).hasSize(1);
    }

    @Test
    public void shouldFindIdsByUser_with_po_without_default_role() {
        final String userRoleId = "API_USER";
        final String poRoleId = "API_PRIMARY_OWNER";
        final String groupName = "GROUP_NAME";

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId(userRoleId);
        userRole.setName(userRoleId);
        userRole.setPermissions(Map.of());
        userRole.setScope(RoleScope.API);

        when(roleService.findById(userRoleId)).thenReturn(userRole);
        when(api.getId()).thenReturn("api-1");
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(new ApiCriteria.Builder().environmentId("DEFAULT").ids("api-1").build());

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(USER_NAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(userRoleId);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                USER_NAME,
                MembershipReferenceType.GROUP,
                poRoleId
            )
        )
            .thenReturn(
                Set.of(MembershipEntity.builder().id("member-id").roleId(poRoleId).memberId(USER_NAME).referenceId(groupName).build())
            );

        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(Api.builder().id("api-id").build()));

        when(primaryOwnerService.getPrimaryOwner(GraviteeContext.getCurrentOrganization(), "api-id"))
            .thenReturn(PrimaryOwnerEntity.builder().id(USER_NAME).build());

        when(groupService.findByIds(Set.of(groupName)))
            .thenReturn(Set.of(GroupEntity.builder().name(groupName).roles(Map.of()).id(groupName).build()));

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        when(apiRepository.searchIds(any(), any(), any())).thenReturn(new Page<>(List.of(), 0, 0, 0));

        final Set<String> apis = apiAuthorizationService.findIdsByUser(GraviteeContext.getExecutionContext(), USER_NAME, false);

        assertThat(apis).isEmpty();
    }

    @Test
    public void findIdsByUserId() {
        final String userId = "USER#1";
        final String poRoleId = "API_PRIMARY_OWNER";

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(userId);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(poRoleId);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setPermissions(ImmutableMap.of("MEMBER", "CRUD".toCharArray()));
        poRole.setScope(RoleScope.API);
        poRole.setName("PRIMARY_OWNER");
        when(roleService.findById(poRoleId)).thenReturn(poRole);

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole));

        final Set<String> apis = apiAuthorizationService.findApiIdsByUserId(GraviteeContext.getExecutionContext(), userId, null, true);

        assertThat(apis).hasSize(1);
    }

    @Test
    public void findIdsByUserIdShouldExcludeReadonlyRoles() {
        final String userId = "USER#1";
        final String userRoleId = "API_USER";
        final String poRoleId = "API_PRIMARY_OWNER";

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(userId);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(userRoleId);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity userRole = new RoleEntity();
        userRole.setId(userRoleId);
        userRole.setPermissions(ImmutableMap.of("MEMBER", "R".toCharArray()));
        userRole.setScope(RoleScope.API);
        userRole.setName("USER");
        when(roleService.findById(userRoleId)).thenReturn(userRole);
        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setPermissions(ImmutableMap.of("MEMBER", "CRUD".toCharArray()));
        poRole.setScope(RoleScope.API);
        poRole.setName("PRIMARY_OWNER");

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        final Set<String> apis = apiAuthorizationService.findApiIdsByUserId(GraviteeContext.getExecutionContext(), userId, null, true);

        assertThat(apis).hasSize(0);
    }

    @Test
    public void shouldFindIdsByUserWithCriteria() {
        final String userRoleId = "API_USER";
        final String poRoleId = "API_PRIMARY_OWNER";

        var allowedDefinitionVersion = new ArrayList<DefinitionVersion>();
        allowedDefinitionVersion.add(null);
        allowedDefinitionVersion.add(DefinitionVersion.V1);
        allowedDefinitionVersion.add(DefinitionVersion.V2);

        when(searchEngineService.search(any(), any())).thenReturn(new SearchResult(List.of("api-1")));

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId(userRoleId);
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        when(roleService.findById(userRoleId)).thenReturn(userRole);
        when(api.getId()).thenReturn("api-1");
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(
            new ApiCriteria.Builder().definitionVersion(allowedDefinitionVersion).environmentId("DEFAULT").ids("api-1").build()
        );

        when(apiRepository.searchIds(eq(apiCriteriaList), any(), any())).thenReturn(new Page<>(List.of("api-1"), 0, 1, 1));

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(USER_NAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(userRoleId);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        ApiQuery apiQuery = ApiQuery.builder().definitionVersions(allowedDefinitionVersion).tag("tag-1").build();
        final Set<String> apis = apiAuthorizationService.findIdsByUser(GraviteeContext.getExecutionContext(), USER_NAME, apiQuery, true);

        assertThat(apis).hasSize(1);
        verify(searchEngineService)
            .search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(query ->
                    query.getExcludedFilters().containsKey(FIELD_DEFINITION_VERSION) &&
                    query.getExcludedFilters().get(FIELD_DEFINITION_VERSION).contains(DefinitionVersion.V4.getLabel()) &&
                    query.getQuery().contains("tag\\-1")
                )
            );
    }

    @Test
    public void shouldFindIdsByEnvironmentId() {
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(new ApiCriteria.Builder().environmentId("DEFAULT").build());

        when(apiRepository.searchIds(eq(apiCriteriaList), any(), any())).thenReturn(new Page<>(List.of("api-1"), 0, 1, 1));
        final Set<String> apis = apiAuthorizationService.findIdsByEnvironment(GraviteeContext.getCurrentEnvironment());

        assertThat(apis).hasSize(1);
    }

    @Test
    public void shouldNotFindIdsByUserBecauseNotExists() {
        final String poRoleId = "API_PRIMARY_OWNER";

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole));

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.emptySet());

        final Set<String> apisId = apiAuthorizationService.findIdsByUser(GraviteeContext.getExecutionContext(), USER_NAME, true);

        assertThat(apisId).isEmpty();
    }

    @Test
    public void shouldFindPublicApisOnlyWithAnonymousUser() {
        final Set<String> apisId = apiAuthorizationService.findIdsByUser(GraviteeContext.getExecutionContext(), null, true);

        assertThat(apisId).isEmpty();

        verify(membershipService, times(0))
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, null, MembershipReferenceType.API);
        verify(membershipService, times(0))
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, null, MembershipReferenceType.GROUP);
        verify(applicationService, times(0)).findByUser(GraviteeContext.getExecutionContext(), null);
    }

    @Test
    public void shouldNotFindApisIfUserIsNotMembership() {
        final String userRoleId = "API_USER";
        final String poRoleId = "API_PRIMARY_OWNER";

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId(userRoleId);
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(emptySet());

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        final Set<String> apisId = apiAuthorizationService.findIdsByUser(GraviteeContext.getExecutionContext(), USER_NAME, true);

        assertThat(apisId).isEmpty();
    }

    @Test
    public void shouldFindAccessibleApiIdsForUser() {
        final String userRoleId = "API_USER";
        final String poRoleId = "API_PRIMARY_OWNER";

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId(userRoleId);
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        when(roleService.findById(userRoleId)).thenReturn(userRole);
        when(api.getId()).thenReturn("api-1");
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(
            new ApiCriteria.Builder()
                .environmentId("DEFAULT")
                .lifecycleStates(List.of(ApiLifecycleState.PUBLISHED))
                .visibility(Visibility.PUBLIC)
                .build()
        );
        apiCriteriaList.add(
            new ApiCriteria.Builder().environmentId("DEFAULT").lifecycleStates(List.of(ApiLifecycleState.PUBLISHED)).ids("api-1").build()
        );
        when(apiRepository.searchIds(eq(apiCriteriaList), any(), any())).thenReturn(new Page<>(List.of("api-1"), 0, 1, 1));

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(USER_NAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(userRoleId);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        final Set<String> apis = apiAuthorizationService.findAccessibleApiIdsForUser(GraviteeContext.getExecutionContext(), USER_NAME);

        assertThat(apis).hasSize(1);
    }

    @Test
    public void shouldFindAccessibleApiIdsForUserWithQuery() {
        final String userRoleId = "API_USER";
        final String poRoleId = "API_PRIMARY_OWNER";

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId(userRoleId);
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        when(roleService.findById(userRoleId)).thenReturn(userRole);
        when(api.getId()).thenReturn("api-1");
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(
            new ApiCriteria.Builder()
                .environmentId("DEFAULT")
                .lifecycleStates(List.of(ApiLifecycleState.PUBLISHED))
                .visibility(Visibility.PUBLIC)
                .build()
        );
        apiCriteriaList.add(
            new ApiCriteria.Builder().environmentId("DEFAULT").lifecycleStates(List.of(ApiLifecycleState.PUBLISHED)).ids("api-1").build()
        );
        when(apiRepository.searchIds(eq(apiCriteriaList), any(), any())).thenReturn(new Page<>(List.of("api-1"), 0, 1, 1));

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(USER_NAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(userRoleId);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        final Set<String> apis = apiAuthorizationService.findAccessibleApiIdsForUser(
            GraviteeContext.getExecutionContext(),
            USER_NAME,
            new ApiQuery()
        );

        assertThat(apis).hasSize(1);
    }

    @Test
    public void shouldFindAccessibleApiIdsForUserWithApi() {
        final String userRoleId = "API_USER";
        final String poRoleId = "API_PRIMARY_OWNER";

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId(userRoleId);
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        when(roleService.findById(userRoleId)).thenReturn(userRole);
        when(api.getId()).thenReturn("api-1");
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(
            new ApiCriteria.Builder()
                .environmentId("DEFAULT")
                .lifecycleStates(List.of(ApiLifecycleState.PUBLISHED))
                .ids("api-1")
                .visibility(Visibility.PUBLIC)
                .build()
        );
        apiCriteriaList.add(
            new ApiCriteria.Builder().environmentId("DEFAULT").lifecycleStates(List.of(ApiLifecycleState.PUBLISHED)).ids("api-1").build()
        );
        when(apiRepository.searchIds(eq(apiCriteriaList), any(), any())).thenReturn(new Page<>(List.of("api-1"), 0, 1, 1));

        MembershipEntity membership = new MembershipEntity();
        membership.setId("id");
        membership.setMemberId(USER_NAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceId(api.getId());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoleId(userRoleId);

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));

        RoleEntity poRole = new RoleEntity();
        poRole.setId(poRoleId);
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        final Set<String> apis = apiAuthorizationService.findAccessibleApiIdsForUser(
            GraviteeContext.getExecutionContext(),
            USER_NAME,
            Set.of("api-1")
        );

        assertThat(apis).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnAllVisibleApiWhenManagedOnlyIsFalse() {
        when(searchEngineService.search(any(), any())).thenReturn(new SearchResult(List.of("api-2")));

        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(new ApiCriteria.Builder().environmentId("DEFAULT").ids(List.of("api-2")).visibility(Visibility.PUBLIC).build());
        when(apiRepository.searchIds(eq(apiCriteriaList), any(), any())).thenReturn(new Page<>(List.of("api-2"), 0, 1, 1));

        final Set<String> apisId = apiAuthorizationService.findIdsByUser(
            GraviteeContext.getExecutionContext(),
            null,
            ApiQuery.builder().tag("a-tag").build(),
            false
        );
        assertThat(apisId).contains("api-2");

        var searchEngineQueryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(searchEngineService).search(any(), searchEngineQueryCaptor.capture());
        SoftAssertions.assertSoftly(soft -> {
            var query = searchEngineQueryCaptor.getValue();
            soft.assertThat(query.getQuery()).isEqualTo("tag:a\\-tag");
            soft.assertThat(query.getExcludedFilters()).isEmpty();
        });
    }

    @Test
    public void shouldSearchSubscriptionWithExcludedApis() {
        String apiId = "apiId";
        String applicationId = "applicationId";
        ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Set.of(apiId));

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId("USER_ROLE_ID");
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        RoleEntity poRole = new RoleEntity();
        poRole.setId("PO_ROLE_ID");
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        when(applicationService.findUserApplicationsIds(GraviteeContext.getExecutionContext(), USER_NAME, ApplicationStatus.ACTIVE))
            .thenReturn(Set.of(applicationId));

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId("subscriptionId");
        subscriptionEntity.setApi("anotherApi");
        when(subscriptionService.search(any(), any())).thenReturn(List.of(subscriptionEntity));

        List<ApiCriteria> result = apiAuthorizationService.computeApiCriteriaForUser(
            GraviteeContext.getExecutionContext(),
            USER_NAME,
            apiQuery,
            false
        );

        assertThat(result).hasSize(2);
        verify(subscriptionService).search(any(), argThat(argument -> argument.getExcludedApis().contains(apiId)));
    }

    @Test
    public void shouldAddCategoryCriteriaWhenCategoryIsNotBlank() {
        String apiId = "apiId";
        String applicationId = "applicationId";
        ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Set.of(apiId));
        String categoryKey = "category-Key";
        apiQuery.setCategory(categoryKey);

        var categoryEntity = new CategoryEntity();
        categoryEntity.setKey(categoryKey);

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId("USER_ROLE_ID");
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        RoleEntity poRole = new RoleEntity();
        poRole.setId("PO_ROLE_ID");
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(categoryService.findById(categoryKey, GraviteeContext.getExecutionContext().getEnvironmentId())).thenReturn(categoryEntity);

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        when(applicationService.findUserApplicationsIds(GraviteeContext.getExecutionContext(), USER_NAME, ApplicationStatus.ACTIVE))
            .thenReturn(Set.of(applicationId));

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId("subscriptionId");
        subscriptionEntity.setApi("anotherApi");
        when(subscriptionService.search(any(), any())).thenReturn(List.of(subscriptionEntity));

        List<ApiCriteria> result = apiAuthorizationService.computeApiCriteriaForUser(
            GraviteeContext.getExecutionContext(),
            USER_NAME,
            apiQuery,
            false
        );

        verify(subscriptionService).search(any(), argThat(argument -> argument.getExcludedApis().contains(apiId)));
        assertThat(result).hasSize(2);
        result
            .stream()
            .filter(i -> i.getCategory() != null)
            .filter(i -> i.getIds().contains(categoryKey))
            .forEach(i -> assertThat(i.getCategory()).isEqualTo(categoryKey));
    }

    @Test
    public void shouldAddCategoryCriteriaWhenCategoryIsBlank() {
        String apiId = "apiId";
        String applicationId = "applicationId";
        ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Set.of(apiId));
        String categoryKey = "";
        apiQuery.setCategory(categoryKey);

        var categoryEntity = new CategoryEntity();
        categoryEntity.setKey(categoryKey);

        Map<String, char[]> userPermissions = ImmutableMap.of("MEMBER", "CRUD".toCharArray());
        RoleEntity userRole = new RoleEntity();
        userRole.setId("USER_ROLE_ID");
        userRole.setPermissions(userPermissions);
        userRole.setScope(RoleScope.API);

        RoleEntity poRole = new RoleEntity();
        poRole.setId("PO_ROLE_ID");
        poRole.setScope(RoleScope.API);
        poRole.setName(SystemRole.PRIMARY_OWNER.name());

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization())).thenReturn(List.of(poRole, userRole));

        when(applicationService.findUserApplicationsIds(GraviteeContext.getExecutionContext(), USER_NAME, ApplicationStatus.ACTIVE))
            .thenReturn(Set.of(applicationId));

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId("subscriptionId");
        subscriptionEntity.setApi("anotherApi");
        when(subscriptionService.search(any(), any())).thenReturn(List.of(subscriptionEntity));

        List<ApiCriteria> result = apiAuthorizationService.computeApiCriteriaForUser(
            GraviteeContext.getExecutionContext(),
            USER_NAME,
            apiQuery,
            false
        );

        verify(subscriptionService).search(any(), argThat(argument -> argument.getExcludedApis().contains(apiId)));
        assertThat(result).hasSize(2);
        var nonNullCategoryResult = result.stream().filter(i -> i.getCategory() != null).count();
        assertThat(nonNullCategoryResult).isZero();
    }

    @Test
    public void shouldBeAbleToConsumePublicApi() {
        GenericApiEntity api = mock(GenericApiEntity.class);
        when(api.getVisibility()).thenReturn(io.gravitee.rest.api.model.Visibility.PUBLIC);
        assertThat(apiAuthorizationService.canConsumeApi(GraviteeContext.getExecutionContext(), USER_NAME, api)).isTrue();
    }

    @Test
    public void shouldBeAbleToConsumeApiIfDirectMember() {
        GenericApiEntity api = mock(GenericApiEntity.class);
        when(api.getId()).thenReturn("api-id");
        when(api.getVisibility()).thenReturn(io.gravitee.rest.api.model.Visibility.PRIVATE);
        MembershipEntity membership = mock(MembershipEntity.class);
        when(membership.getReferenceId()).thenReturn("api-id");
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.API))
            .thenReturn(Collections.singleton(membership));
        assertThat(apiAuthorizationService.canConsumeApi(GraviteeContext.getExecutionContext(), USER_NAME, api)).isTrue();
    }

    @Test
    public void shouldBeAbleToConsumeApiIfMemberFromGroup() {
        GenericApiEntity api = mock(GenericApiEntity.class);
        when(api.getVisibility()).thenReturn(io.gravitee.rest.api.model.Visibility.PRIVATE);
        when(api.getGroups()).thenReturn(Collections.singleton("group-id"));
        MembershipEntity membership = mock(MembershipEntity.class);
        when(membership.getReferenceId()).thenReturn("group-id");
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USER_NAME, MembershipReferenceType.GROUP))
            .thenReturn(Collections.singleton(membership));
        assertThat(apiAuthorizationService.canConsumeApi(GraviteeContext.getExecutionContext(), USER_NAME, api)).isTrue();
    }

    @Test
    public void shouldBeAbleToConsumeApiIfDirectMemberOfSubscribedApplication() {
        GenericApiEntity api = mock(GenericApiEntity.class);
        when(api.getId()).thenReturn("api-id");
        when(api.getVisibility()).thenReturn(io.gravitee.rest.api.model.Visibility.PRIVATE);
        when(api.getVisibility()).thenReturn(io.gravitee.rest.api.model.Visibility.PRIVATE);

        when(applicationService.findUserApplicationsIds(GraviteeContext.getExecutionContext(), USER_NAME, ApplicationStatus.ACTIVE))
            .thenReturn(Collections.singleton("application-id"));

        final SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(Collections.singleton("application-id"));
        query.setApi(api.getId());
        query.setStatuses(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.RESUMED));

        when(subscriptionService.search(any(ExecutionContext.class), eq(query)))
            .thenReturn(Collections.singletonList(new SubscriptionEntity()));

        assertThat(apiAuthorizationService.canConsumeApi(GraviteeContext.getExecutionContext(), USER_NAME, api)).isTrue();
    }
}
