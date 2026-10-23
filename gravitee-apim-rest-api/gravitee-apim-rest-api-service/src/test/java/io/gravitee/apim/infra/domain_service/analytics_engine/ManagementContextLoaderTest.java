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
package io.gravitee.apim.infra.domain_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ManagementContextLoaderTest {

    private static final String USER_ID = "user-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");

    private static final Api API_1 = Api.builder().id("id1").name("api1").type(ApiType.PROXY).build();
    private static final Api API_2 = Api.builder().id("id2").name("api2").type(ApiType.MESSAGE).build();
    private static final Api API_3 = Api.builder().id("id3").name("api3").type(ApiType.LLM_PROXY).build();

    @Mock
    private EnvironmentAnalyticsApisDomainService readableApis;

    @Mock
    private Authentication authentication;

    private ManagementContextLoader contextLoader;

    private final AuditInfo auditInfo = AuditInfo.builder()
        .organizationId("DEFAULT")
        .environmentId("DEFAULT")
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    @BeforeEach
    void setUp() {
        contextLoader = new ManagementContextLoader(readableApis);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void givenAuthenticatedWith(String role) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
        Collection<? extends GrantedAuthority> authorities = new ArrayList<>(List.of((GrantedAuthority) () -> role));
        doReturn(authorities).when(authentication).getAuthorities();
    }

    @Nested
    class AdminUser {

        @BeforeEach
        void setup() {
            givenAuthenticatedWith("ORGANIZATION:ADMIN");
        }

        @Test
        void should_load_context_with_the_apis_read_as_an_admin() {
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, true, ApiDataKind.AGGREGATES)).thenReturn(
                List.of(API_1, API_2, API_3)
            );

            var context = contextLoader.load(auditInfo, ApiDataKind.AGGREGATES);

            assertThat(context.authorizedApiIds()).containsExactlyInAnyOrder("id1", "id2", "id3");
            assertThat(context.apiNamesById()).isEqualTo(Map.of("id1", "api1", "id2", "api2", "id3", "api3"));
            assertThat(context.apiIdsByType()).isEqualTo(
                Map.of(ApiType.PROXY, Set.of("id1"), ApiType.MESSAGE, Set.of("id2"), ApiType.LLM_PROXY, Set.of("id3"))
            );
            assertThat(context.applicationNamesById()).isEmpty();
            assertThat(context.executionContext().getOrganizationId()).isEqualTo("DEFAULT");
            assertThat(context.executionContext().getEnvironmentId()).isEqualTo("DEFAULT");
        }

        @Test
        void should_exclude_apis_with_null_type_from_apiIdsByType() {
            var apiWithoutType = Api.builder().id("id-no-type").name("no-type-api").build();
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, true, ApiDataKind.AGGREGATES)).thenReturn(
                List.of(API_1, apiWithoutType)
            );

            var context = contextLoader.load(auditInfo, ApiDataKind.AGGREGATES);

            assertThat(context.authorizedApiIds()).containsExactlyInAnyOrder("id1", "id-no-type");
            assertThat(context.apiIdsByType()).isEqualTo(Map.of(ApiType.PROXY, Set.of("id1")));
        }

        @Test
        void should_group_multiple_apis_of_same_type() {
            var anotherProxy = Api.builder().id("id4").name("api4").type(ApiType.PROXY).build();
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, true, ApiDataKind.AGGREGATES)).thenReturn(
                List.of(API_1, anotherProxy, API_3)
            );

            var context = contextLoader.load(auditInfo, ApiDataKind.AGGREGATES);

            assertThat(context.apiIdsByType().get(ApiType.PROXY)).containsExactlyInAnyOrder("id1", "id4");
            assertThat(context.apiIdsByType().get(ApiType.LLM_PROXY)).containsExactly("id3");
        }
    }

    @Nested
    class NonAdminUser {

        @BeforeEach
        void setup() {
            givenAuthenticatedWith("ORGANIZATION:USER");
        }

        @Test
        void should_load_context_with_the_apis_whose_requests_the_user_may_read() {
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST)).thenReturn(List.of(API_1));

            var context = contextLoader.load(auditInfo, ApiDataKind.PER_REQUEST);

            assertThat(context.authorizedApiIds()).containsExactly("id1");
        }

        @Test
        void should_load_context_with_the_apis_whose_aggregates_the_user_may_read() {
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES)).thenReturn(List.of(API_2));

            var context = contextLoader.load(auditInfo, ApiDataKind.AGGREGATES);

            assertThat(context.authorizedApiIds()).containsExactly("id2");
            assertThat(context.apiNamesById()).isEqualTo(Map.of("id2", "api2"));
            assertThat(context.apiIdsByType()).isEqualTo(Map.of(ApiType.MESSAGE, Set.of("id2")));
        }

        /** Whoever may read an API in the console reads all its calls, keyless ones included. */
        @Test
        void should_not_restrict_the_calls_of_the_readable_apis_to_any_application() {
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES)).thenReturn(List.of(API_2));

            var context = contextLoader.load(auditInfo, ApiDataKind.AGGREGATES);

            assertThat(context.restrictedToApplicationIds()).isEmpty();
        }

        /** An empty scope has to reach the engine as an empty set of API ids, which it turns into a query matching nothing. */
        @Test
        void should_load_an_empty_context_when_the_user_may_read_no_api() {
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES)).thenReturn(List.of());

            var context = contextLoader.load(auditInfo, ApiDataKind.AGGREGATES);

            assertThat(context.authorizedApiIds()).isEmpty();
            assertThat(context.apiNamesById()).isEmpty();
            assertThat(context.apiIdsByType()).isEmpty();
        }

        @Test
        void should_ask_for_aggregates_and_never_for_the_requests_themselves() {
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES)).thenReturn(List.of());

            contextLoader.load(auditInfo, ApiDataKind.AGGREGATES);

            verify(readableApis).findReadableApis(eq(EXECUTION_CONTEXT), eq(USER_ID), eq(false), eq(ApiDataKind.AGGREGATES));
        }
    }
}
