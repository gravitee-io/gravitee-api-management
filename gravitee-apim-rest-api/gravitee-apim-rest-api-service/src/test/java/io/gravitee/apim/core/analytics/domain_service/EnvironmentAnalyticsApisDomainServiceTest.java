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
package io.gravitee.apim.core.analytics.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvironmentAnalyticsApisDomainServiceTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "environment-id";
    private static final String OTHER_ENV_ID = "other-env-id";
    private static final String USER_ID = "user-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORG_ID, ENV_ID);

    private static final Api PROXY_V4 = ApiFixtures.aProxyApiV4().toBuilder().id("proxy-v4").build();
    private static final Api MESSAGE_V4 = ApiFixtures.aMessageApiV4().toBuilder().id("message-v4").build();
    private static final Api PROXY_V2 = ApiFixtures.aProxyApiV2().toBuilder().id("proxy-v2").build();
    private static final Api FEDERATED = ApiFixtures.aFederatedApi().toBuilder().id("federated").environmentId(ENV_ID).build();
    private static final Api OTHER_ENV = ApiFixtures.aProxyApiV4().toBuilder().id("other-env").environmentId(OTHER_ENV_ID).build();

    private final ApiQueryServiceInMemory apiQueryService = spy(new ApiQueryServiceInMemory());
    private final ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService = spy(new ApiAuthorizationDomainServiceInMemory());
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);

    private EnvironmentAnalyticsApisDomainService cut;

    @BeforeEach
    void setUp() {
        apiQueryService.initWith(List.of(PROXY_V4, MESSAGE_V4, PROXY_V2, FEDERATED, OTHER_ENV));
        cut = new EnvironmentAnalyticsApisDomainService(apiQueryService, apiAuthorizationDomainService, permissionDomainService);
    }

    @AfterEach
    void tearDown() {
        apiQueryService.reset();
        apiAuthorizationDomainService.reset();
    }

    @Nested
    class PlatformReader {

        @BeforeEach
        void setUp() {
            givenUserReadsThePlatformOf(ENV_ID);
        }

        @Test
        void should_return_every_v2_and_v4_api_of_the_environment_without_any_membership() {
            var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID);

            assertThat(apis).extracting(Api::getId).containsExactlyInAnyOrder("proxy-v4", "message-v4", "proxy-v2");
        }

        @Test
        void should_not_check_the_analytics_permission_of_each_api() {
            cut.findReadableApis(EXECUTION_CONTEXT, USER_ID);

            verify(permissionDomainService, never()).hasPermission(
                any(),
                any(),
                eq(RolePermission.API_ANALYTICS),
                any(),
                eq(RolePermissionAction.READ)
            );
        }
    }

    @Nested
    class ApiMember {

        @Test
        void should_return_the_member_apis_whose_analytics_the_user_can_read() {
            givenUserIsMemberOf(PROXY_V4, MESSAGE_V4, PROXY_V2);
            givenUserCanReadAnalyticsOf(PROXY_V4, PROXY_V2);

            var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID);

            assertThat(apis).extracting(Api::getId).containsExactlyInAnyOrder("proxy-v4", "proxy-v2");
        }

        @Test
        void should_not_widen_to_the_environment_with_a_platform_permission_held_on_another_environment() {
            givenUserReadsThePlatformOf(OTHER_ENV_ID);
            givenUserIsMemberOf(PROXY_V4);
            givenUserCanReadAnalyticsOf(PROXY_V4);

            var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID);

            assertThat(apis).extracting(Api::getId).containsExactly("proxy-v4");
        }

        @Test
        void should_ignore_member_apis_of_another_environment_or_definition_version() {
            givenUserIsMemberOf(PROXY_V4, FEDERATED, OTHER_ENV);
            givenUserCanReadAnalyticsOf(PROXY_V4, FEDERATED, OTHER_ENV);

            var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID);

            assertThat(apis).extracting(Api::getId).containsExactly("proxy-v4");
        }

        @Test
        void should_return_no_api_when_the_user_cannot_read_the_analytics_of_any_member_api() {
            givenUserIsMemberOf(PROXY_V4, MESSAGE_V4);

            var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID);

            assertThat(apis).isEmpty();
        }
    }

    @Nested
    class ByKindOfData {

        @Nested
        class OrganizationAdmin {

            @Test
            void should_read_every_api_of_the_environment_of_any_type_for_both_kinds_without_any_permission_check() {
                for (var kind : ApiDataKind.values()) {
                    var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, true, kind);

                    assertThat(apis).extracting(Api::getId).containsExactlyInAnyOrder("proxy-v4", "message-v4", "proxy-v2", "federated");
                }
                verifyNoInteractions(permissionDomainService);
            }
        }

        @Nested
        class PlatformReader {

            @BeforeEach
            void setUp() {
                givenUserReadsThePlatformOf(ENV_ID);
            }

            @Test
            void should_read_the_aggregates_of_every_api_of_the_environment_whatever_its_type() {
                var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES);

                assertThat(apis).extracting(Api::getId).containsExactlyInAnyOrder("proxy-v4", "message-v4", "proxy-v2", "federated");
            }

            @Test
            void should_never_read_the_requests_of_an_api_the_user_is_not_a_member_of() {
                var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST);

                assertThat(apis).isEmpty();
                verify(apiQueryService, never()).search(any(), any(), any());
            }

            @Test
            void should_read_the_requests_of_the_member_apis_whose_log_permission_it_holds_only() {
                givenUserIsMemberOf(PROXY_V4, MESSAGE_V4);
                givenUserCanReadLogsOf(PROXY_V4);

                var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST);

                assertThat(apis).extracting(Api::getId).containsExactly("proxy-v4");
            }

            @Test
            void should_not_find_the_requests_of_a_single_api_it_does_not_hold_the_log_permission_of() {
                assertThat(cut.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST, "proxy-v4")).isEmpty();
            }

            @Test
            void should_find_the_aggregates_of_a_single_api_without_being_a_member_of_it() {
                assertThat(cut.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES, "federated"))
                    .get()
                    .extracting(Api::getId)
                    .isEqualTo("federated");
            }
        }

        @Nested
        class Member {

            @Test
            void should_read_the_aggregates_but_not_the_requests_with_the_analytics_permission_only() {
                givenUserIsMemberOf(PROXY_V4, FEDERATED);
                givenUserCanReadAnalyticsOf(PROXY_V4, FEDERATED);

                assertThat(cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES))
                    .extracting(Api::getId)
                    .containsExactlyInAnyOrder("proxy-v4", "federated");
                assertThat(cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST)).isEmpty();
            }

            @Test
            void should_read_the_requests_but_not_the_aggregates_with_the_log_permission_only() {
                givenUserIsMemberOf(PROXY_V4, FEDERATED);
                givenUserCanReadLogsOf(PROXY_V4, FEDERATED);

                assertThat(cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST))
                    .extracting(Api::getId)
                    .containsExactlyInAnyOrder("proxy-v4", "federated");
                assertThat(cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES)).isEmpty();
            }

            @Test
            void should_not_widen_the_aggregates_with_the_platform_permission_of_another_environment() {
                givenUserReadsThePlatformOf(OTHER_ENV_ID);
                givenUserIsMemberOf(PROXY_V4);
                givenUserCanReadAnalyticsOf(PROXY_V4);

                var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES);

                assertThat(apis).extracting(Api::getId).containsExactly("proxy-v4");
            }

            @Test
            void should_find_a_single_member_api_whose_permission_it_holds() {
                givenUserIsMemberOf(PROXY_V4);
                givenUserCanReadLogsOf(PROXY_V4);

                assertThat(cut.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST, "proxy-v4")).isPresent();
            }

            @Test
            void should_not_find_a_single_member_api_whose_permission_it_does_not_hold() {
                givenUserIsMemberOf(PROXY_V4);
                givenUserCanReadAnalyticsOf(PROXY_V4);

                assertThat(cut.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST, "proxy-v4")).isEmpty();
            }

            /** Checking one API must cost one permission lookup, not the enumeration of everything the user is a member of. */
            @Test
            void should_check_a_single_api_without_loading_the_membership_of_the_user() {
                givenUserCanReadLogsOf(PROXY_V4);

                assertThat(cut.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST, "proxy-v4")).isPresent();

                verify(apiAuthorizationDomainService, never()).findMemberApiIds(any(), any());
                verify(permissionDomainService, times(1)).hasPermission(any(), any(), eq(RolePermission.API_LOG), any(), any());
            }

            @Test
            void should_not_find_an_unknown_api_or_one_of_another_environment_whatever_the_user_may_read() {
                givenUserCanReadLogsOf(OTHER_ENV);

                assertThat(cut.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST, "unknown")).isEmpty();
                assertThat(cut.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST, "other-env")).isEmpty();
                verify(permissionDomainService, never()).hasPermission(any(), any(), eq(RolePermission.API_LOG), any(), any());
            }
        }

        /**
         * The cost grows linearly with the memberships: one search for all the member ids and one permission lookup per member API,
         * never one search per API nor a lookup of the whole membership for each API.
         */
        @Test
        void should_search_once_and_check_each_member_api_once_for_a_user_with_many_memberships() {
            var memberApis = IntStream.range(0, 500)
                .mapToObj(i -> ApiFixtures.aProxyApiV4().toBuilder().id("member-" + i).environmentId(ENV_ID).build())
                .toArray(Api[]::new);
            apiQueryService.initWith(List.of(memberApis));
            givenUserIsMemberOf(memberApis);
            givenUserCanReadLogsOf(memberApis);

            var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST);

            assertThat(apis).hasSize(500);
            verify(apiQueryService, times(1)).search(any(), any(), any());
            verify(permissionDomainService, times(500)).hasPermission(any(), any(), eq(RolePermission.API_LOG), any(), any());
        }

        @Test
        void should_read_nothing_without_searching_when_the_user_is_a_member_of_no_api() {
            for (var kind : ApiDataKind.values()) {
                assertThat(cut.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, kind)).isEmpty();
            }
            // The repositories drop an empty id filter, so searching with no member id would return the whole environment.
            verify(apiQueryService, never()).search(any(), any(), any());
        }
    }

    @Test
    void should_return_no_api_without_searching_when_the_user_reads_neither_the_platform_nor_any_api() {
        var apis = cut.findReadableApis(EXECUTION_CONTEXT, USER_ID);

        assertThat(apis).isEmpty();
        // The repositories drop an empty id filter, so searching with no member id would return the whole environment.
        verify(apiQueryService, never()).search(any(), any(), any());
    }

    @Test
    void should_group_the_readable_api_ids_by_definition_version() {
        givenUserReadsThePlatformOf(ENV_ID);

        var apiIds = cut.findReadableApiIdsByDefinitionVersion(EXECUTION_CONTEXT, USER_ID);

        assertThat(apiIds)
            .containsOnlyKeys(DefinitionVersion.V4, DefinitionVersion.V2)
            .containsEntry(DefinitionVersion.V2, List.of("proxy-v2"));
        assertThat(apiIds.get(DefinitionVersion.V4)).containsExactlyInAnyOrder("proxy-v4", "message-v4");
    }

    private void givenUserReadsThePlatformOf(String environmentId) {
        when(
            permissionDomainService.hasPermission(
                ORG_ID,
                USER_ID,
                RolePermission.ENVIRONMENT_PLATFORM,
                environmentId,
                RolePermissionAction.READ
            )
        ).thenReturn(true);
    }

    private void givenUserIsMemberOf(Api... apis) {
        apiAuthorizationDomainService.initWith(List.of(apis));
    }

    private void givenUserCanReadLogsOf(Api... apis) {
        for (Api api : apis) {
            when(
                permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.API_LOG, api.getId(), RolePermissionAction.READ)
            ).thenReturn(true);
        }
    }

    private void givenUserCanReadAnalyticsOf(Api... apis) {
        for (Api api : apis) {
            when(
                permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.API_ANALYTICS, api.getId(), RolePermissionAction.READ)
            ).thenReturn(true);
        }
    }
}
