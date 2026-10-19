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
package io.gravitee.apim.infra.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.infra.domain_service.analytics_engine.ManagementContextLoader;
import io.gravitee.apim.infra.domain_service.permission.PermissionDomainServiceLegacyWrapper;
import io.gravitee.apim.infra.domain_service.user.UserContextLoaderImpl;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition;
import io.gravitee.rest.api.service.impl.PermissionServiceImpl;
import io.gravitee.rest.api.service.impl.RoleServiceImpl;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * What the platform and the environment-logs loaders let a user observe, decided by the real permission evaluation over the
 * real default roles rather than by stubbed answers: stubbing ENVIRONMENT_API[READ] as "reads every API" is how a plain
 * environment user came to see every API's analytics and logs.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObservabilityApiScopeByRoleTest {

    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final String OTHER_ENVIRONMENT_ID = "other-env";
    private static final String USER_ID = "plain-user";

    private static final Api API_1 = ApiFixtures.aProxyApiV4().toBuilder().id("api1").environmentId(ENVIRONMENT_ID).build();
    private static final Api API_2 = ApiFixtures.aMessageApiV4().toBuilder().id("api2").environmentId(ENVIRONMENT_ID).build();
    private static final Api API_3 = ApiFixtures.aFederatedApi().toBuilder().id("api3").environmentId(ENVIRONMENT_ID).build();

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Spy
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @InjectMocks
    private PermissionServiceImpl permissionService = new PermissionServiceImpl();

    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ApiAuthorizationDomainServiceInMemory memberships = new ApiAuthorizationDomainServiceInMemory();

    private final AuditInfo auditInfo = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    private ManagementContextLoader platformLoader;
    private UserContextLoaderImpl environmentLogsLoader;

    @BeforeEach
    void setUp() {
        apiQueryService.initWith(List.of(API_1, API_2, API_3));
        var readableApis = new EnvironmentAnalyticsApisDomainService(
            apiQueryService,
            memberships,
            new PermissionDomainServiceLegacyWrapper(membershipService, permissionService)
        );
        platformLoader = new ManagementContextLoader(readableApis);
        environmentLogsLoader = new UserContextLoaderImpl(readableApis);

        givenAuthenticatedAs("ORGANIZATION:USER");
        givenEnvironmentRole(ENVIRONMENT_ID, DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER.getPermissions());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class DefaultEnvironmentUserMemberOfNoApi {

        @Test
        void should_see_no_api_in_the_analytics_engine() {
            assertThat(platformLoader.load(auditInfo, ApiDataKind.AGGREGATES).authorizedApiIds()).isEmpty();
        }

        @Test
        void should_see_no_api_in_the_environment_logs() {
            assertThat(requestsApis()).isEmpty();
        }

        @Test
        void should_see_no_single_api_in_the_environment_logs() {
            assertThat(
                environmentLogsLoader.loadApi(new UserContext(auditInfo), "api1", ApiDataKind.PER_REQUEST).apis().orElseThrow()
            ).isEmpty();
        }
    }

    @Nested
    class ApiOwner {

        @BeforeEach
        void setUp() {
            givenMemberOf(API_1, DefaultRoleEntityDefinition.ROLE_API_OWNER.getPermissions());
        }

        @Test
        void should_see_only_the_owned_api_in_the_analytics_engine() {
            assertThat(platformLoader.load(auditInfo, ApiDataKind.AGGREGATES).authorizedApiIds()).containsExactly("api1");
        }

        @Test
        void should_see_only_the_owned_api_in_the_environment_logs() {
            assertThat(requestsApis()).containsExactly("api1");
        }
    }

    @Nested
    class MemberWithTheDefaultApiUserRole {

        @Test
        void should_see_neither_the_analytics_nor_the_logs_of_the_api() {
            givenMemberOf(API_1, DefaultRoleEntityDefinition.DEFAULT_ROLE_API_USER.getPermissions());

            assertThat(platformLoader.load(auditInfo, ApiDataKind.AGGREGATES).authorizedApiIds()).isEmpty();
            assertThat(requestsApis()).isEmpty();
        }
    }

    @Nested
    class MemberWithAReadOnlyRole {

        @Test
        void should_see_the_analytics_but_not_the_logs_with_the_analytics_permission_only() {
            givenMemberOf(API_1, Map.of(ApiPermission.ANALYTICS.getName(), new char[] { RolePermissionAction.READ.getId() }));

            assertThat(platformLoader.load(auditInfo, ApiDataKind.AGGREGATES).authorizedApiIds()).containsExactly("api1");
            assertThat(requestsApis()).isEmpty();
        }

        @Test
        void should_see_the_logs_but_not_the_analytics_with_the_log_permission_only() {
            givenMemberOf(API_1, Map.of(ApiPermission.LOG.getName(), new char[] { RolePermissionAction.READ.getId() }));

            assertThat(platformLoader.load(auditInfo, ApiDataKind.AGGREGATES).authorizedApiIds()).isEmpty();
            assertThat(requestsApis()).containsExactly("api1");
        }
    }

    @Nested
    class HolderOfAnEnvironmentRoleReadingThePlatform {

        @BeforeEach
        void setUp() {
            givenEnvironmentRole(ENVIRONMENT_ID, DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER.getPermissions());
        }

        @Test
        void should_see_the_analytics_of_every_api_of_the_environment_whatever_its_type() {
            assertThat(platformLoader.load(auditInfo, ApiDataKind.AGGREGATES).authorizedApiIds()).containsExactlyInAnyOrder(
                "api1",
                "api2",
                "api3"
            );
        }

        @Test
        void should_see_the_logs_of_no_api_it_is_not_a_member_of() {
            assertThat(requestsApis()).isEmpty();
        }

        @Test
        void should_see_the_logs_of_the_member_apis_only() {
            givenMemberOf(API_2, DefaultRoleEntityDefinition.ROLE_API_OWNER.getPermissions());

            assertThat(requestsApis()).containsExactly("api2");
        }

        /** A field identifying a caller (client IP, subject, Kafka client id) is per-request data. */
        @Test
        void should_see_the_analytics_of_the_member_apis_only_when_a_field_identifies_a_caller() {
            givenMemberOf(API_2, DefaultRoleEntityDefinition.ROLE_API_OWNER.getPermissions());

            assertThat(platformLoader.load(auditInfo, ApiDataKind.PER_REQUEST).authorizedApiIds()).containsExactly("api2");
        }
    }

    @Nested
    class HolderOfThePlatformPermissionOnAnotherEnvironment {

        @Test
        void should_not_see_the_analytics_of_the_apis_of_this_environment() {
            givenEnvironmentRole(OTHER_ENVIRONMENT_ID, DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER.getPermissions());

            assertThat(platformLoader.load(auditInfo, ApiDataKind.AGGREGATES).authorizedApiIds()).isEmpty();
        }
    }

    @Nested
    class OrganizationAdmin {

        @BeforeEach
        void setUp() {
            givenAuthenticatedAs("ORGANIZATION:ADMIN");
        }

        @Test
        void should_see_every_api_in_the_analytics_engine_and_in_the_environment_logs() {
            assertThat(platformLoader.load(auditInfo, ApiDataKind.AGGREGATES).authorizedApiIds()).containsExactlyInAnyOrder(
                "api1",
                "api2",
                "api3"
            );
            assertThat(requestsApis()).containsExactlyInAnyOrder("api1", "api2", "api3");
        }
    }

    private Set<String> requestsApis() {
        return environmentLogsLoader
            .loadApis(new UserContext(auditInfo), ApiDataKind.PER_REQUEST)
            .apis()
            .orElseThrow()
            .stream()
            .map(Api::getId)
            .collect(Collectors.toSet());
    }

    private void givenAuthenticatedAs(String organizationRole) {
        var principal = new UserDetails(USER_ID, "", List.of(new SimpleGrantedAuthority(organizationRole)));
        SecurityContextHolder.setContext(
            new SecurityContextImpl(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()))
        );
    }

    private void givenEnvironmentRole(String environmentId, Map<String, char[]> permissions) {
        when(
            membershipService.getUserMemberPermissions(any(), eq(MembershipReferenceType.ENVIRONMENT), eq(environmentId), eq(USER_ID))
        ).thenReturn(permissions);
    }

    private void givenMemberOf(Api api, Map<String, char[]> permissions) {
        memberships.initWith(Stream.concat(memberships.storage().stream(), Stream.of(api)).toList());
        when(membershipService.getUserMemberPermissions(any(), eq(MembershipReferenceType.API), eq(api.getId()), eq(USER_ID))).thenReturn(
            permissions
        );
    }
}
