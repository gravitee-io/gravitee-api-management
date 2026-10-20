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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationService_FindIdsByUserAndPermissionTest {

    private static final String USERNAME = "Gravitee.io APIM";
    private static final String ENV_ID = "envId";
    private static final String LOG_READER = "app-log-reader";
    private static final String APP_USER = "app-user";
    private static final String API_LOG_READER = "api-log-reader";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    private final List<MembershipEntity> userMemberships = new ArrayList<>();
    private final List<StoredApplication> storedApplications = new ArrayList<>();

    private record StoredApplication(String id, String environmentId, ApplicationStatus status, Set<String> groups) {}

    @BeforeEach
    void setUp() throws Exception {
        GraviteeContext.setCurrentEnvironment(ENV_ID);

        Map<String, RoleEntity> roles = Map.of(
            LOG_READER,
            role(LOG_READER, RoleScope.APPLICATION, Map.of(ApplicationPermission.LOG.getName(), new char[] { 'R' })),
            APP_USER,
            role(APP_USER, RoleScope.APPLICATION, Map.of(ApplicationPermission.DEFINITION.getName(), new char[] { 'R' })),
            API_LOG_READER,
            role(API_LOG_READER, RoleScope.API, Map.of(ApiPermission.LOG.getName(), new char[] { 'R' }))
        );
        RoleServiceImpl realRoleService = new RoleServiceImpl();
        lenient().when(roleService.findAllByOrganization(any())).thenReturn(List.copyOf(roles.values()));
        lenient()
            .when(roleService.findById(any()))
            .thenAnswer(invocation -> roles.get(invocation.<String>getArgument(0)));
        lenient()
            .when(roleService.hasPermission(any(), any(), any()))
            .thenAnswer(invocation ->
                realRoleService.hasPermission(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))
            );

        lenient()
            .when(membershipService.getReferenceIdsByMemberAndReferenceAndRoleIn(eq(MembershipMemberType.USER), eq(USERNAME), any(), any()))
            .thenAnswer(invocation -> {
                MembershipReferenceType referenceType = invocation.getArgument(2);
                Collection<String> roleIds = invocation.getArgument(3);
                return userMemberships
                    .stream()
                    .filter(m -> m.getReferenceType() == referenceType && roleIds.contains(m.getRoleId()))
                    .map(MembershipEntity::getReferenceId)
                    .collect(Collectors.toCollection(HashSet::new));
            });
        lenient()
            .when(membershipService.getReferenceIdsByMemberAndReference(eq(MembershipMemberType.USER), eq(USERNAME), any()))
            .thenAnswer(invocation ->
                userMemberships
                    .stream()
                    .filter(m -> m.getReferenceType() == invocation.getArgument(2))
                    .map(MembershipEntity::getReferenceId)
                    .collect(Collectors.toCollection(HashSet::new))
            );
        lenient()
            .when(membershipService.getMembershipsByMemberAndReference(eq(MembershipMemberType.USER), eq(USERNAME), any()))
            .thenAnswer(invocation ->
                userMemberships
                    .stream()
                    .filter(m -> m.getReferenceType() == invocation.getArgument(2))
                    .collect(Collectors.toSet())
            );

        lenient()
            .when(applicationRepository.searchIds(any(), any()))
            .thenAnswer(invocation -> {
                ApplicationCriteria criteria = invocation.getArgument(0);
                return storedApplications
                    .stream()
                    .filter(app -> criteria.getEnvironmentIds().contains(app.environmentId()))
                    .filter(app -> criteria.getStatus() == null || criteria.getStatus() == app.status())
                    .filter(app -> isEmpty(criteria.getRestrictedToIds()) || criteria.getRestrictedToIds().contains(app.id()))
                    .filter(app -> isEmpty(criteria.getGroups()) || app.groups().stream().anyMatch(criteria.getGroups()::contains))
                    .map(StoredApplication::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            });
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_no_application_when_user_lacks_permission_on_associated_applications() {
        givenApplication("app1");
        givenApplication("app2", "group1");
        givenMembership(MembershipReferenceType.APPLICATION, "app1", APP_USER);
        givenMembership(MembershipReferenceType.GROUP, "group1", APP_USER);

        assertThat(findIdsWithLogReadPermission()).isEmpty();
    }

    @Test
    void should_return_only_applications_whose_direct_role_grants_permission() {
        givenApplication("app1");
        givenApplication("app2");
        givenMembership(MembershipReferenceType.APPLICATION, "app1", LOG_READER);
        givenMembership(MembershipReferenceType.APPLICATION, "app2", APP_USER);

        assertThat(findIdsWithLogReadPermission()).containsExactly("app1");
    }

    @Test
    void should_return_applications_granted_through_group_when_user_has_no_direct_grant() {
        givenApplication("app1");
        givenApplication("app2", "group1");
        givenMembership(MembershipReferenceType.APPLICATION, "app1", APP_USER);
        givenMembership(MembershipReferenceType.GROUP, "group1", LOG_READER);

        assertThat(findIdsWithLogReadPermission()).containsExactly("app2");
    }

    @Test
    void should_combine_direct_and_group_grants_and_exclude_groups_without_permission() {
        givenApplication("app1");
        givenApplication("app2", "group1");
        givenApplication("app3", "group2");
        givenMembership(MembershipReferenceType.APPLICATION, "app1", LOG_READER);
        givenMembership(MembershipReferenceType.GROUP, "group1", LOG_READER);
        givenMembership(MembershipReferenceType.GROUP, "group2", APP_USER);

        assertThat(findIdsWithLogReadPermission()).containsExactlyInAnyOrder("app1", "app2");
    }

    @Test
    void should_ignore_group_role_of_another_scope_sharing_the_permission_name() {
        givenApplication("app1", "group1");
        givenMembership(MembershipReferenceType.GROUP, "group1", API_LOG_READER);

        assertThat(findIdsWithLogReadPermission()).isEmpty();
    }

    @Test
    void should_exclude_applications_outside_environment_or_not_active() {
        givenApplication("app1");
        storedApplications.add(new StoredApplication("app2", "otherEnv", ApplicationStatus.ACTIVE, Set.of()));
        storedApplications.add(new StoredApplication("app3", ENV_ID, ApplicationStatus.ARCHIVED, Set.of()));
        storedApplications.add(new StoredApplication("app4", "otherEnv", ApplicationStatus.ACTIVE, Set.of("group1")));
        givenMembership(MembershipReferenceType.APPLICATION, "app1", LOG_READER);
        givenMembership(MembershipReferenceType.APPLICATION, "app2", LOG_READER);
        givenMembership(MembershipReferenceType.APPLICATION, "app3", LOG_READER);
        givenMembership(MembershipReferenceType.GROUP, "group1", LOG_READER);

        assertThat(findIdsWithLogReadPermission()).containsExactly("app1");
    }

    private Set<String> findIdsWithLogReadPermission() {
        return applicationService.findIdsByUserAndPermission(
            GraviteeContext.getExecutionContext(),
            USERNAME,
            null,
            RolePermission.APPLICATION_LOG,
            RolePermissionAction.READ
        );
    }

    private void givenApplication(String id, String... groups) {
        storedApplications.add(new StoredApplication(id, ENV_ID, ApplicationStatus.ACTIVE, Set.of(groups)));
    }

    private void givenMembership(MembershipReferenceType referenceType, String referenceId, String roleId) {
        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId(USERNAME);
        membership.setMemberType(MembershipMemberType.USER);
        membership.setReferenceType(referenceType);
        membership.setReferenceId(referenceId);
        membership.setRoleId(roleId);
        userMemberships.add(membership);
    }

    private static RoleEntity role(String id, RoleScope scope, Map<String, char[]> permissions) {
        return RoleEntity.builder().id(id).name(id).scope(scope).permissions(permissions).build();
    }

    private static boolean isEmpty(Collection<String> values) {
        return values == null || values.isEmpty();
    }
}
