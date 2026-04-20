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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.ApplicationService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalContextLoaderTest {

    @Mock
    private PortalNavigationApiVisibilityDomainService visibilityDomainService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private Authentication authentication;

    private PortalContextLoader contextLoader;
    private AutoCloseable closeable;

    private static final String ENV_ID = "DEFAULT";
    private static final String ORG_ID = "DEFAULT";

    private static final Api REPO_API_1 = Api.builder().id("id1").name("api1").type(ApiType.PROXY).build();
    private static final Api REPO_API_2 = Api.builder().id("id2").name("api2").type(ApiType.MESSAGE).build();
    private static final Api REPO_API_3 = Api.builder().id("id3").name("api3").type(ApiType.LLM_PROXY).build();

    private static final String ADMIN_USER_ID = UUID.randomUUID().toString();
    private static final String NON_ADMIN_USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        contextLoader = new PortalContextLoader(visibilityDomainService, apiRepository, applicationService);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        SecurityContextHolder.clearContext();
    }

    private AuditInfo auditInfo(String userId) {
        var actor = AuditActor.builder().userId(userId).build();
        return AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).actor(actor).build();
    }

    private void setUpSecurityContext(String role) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
        var grantedAuthority = new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return role;
            }
        };
        Collection<? extends GrantedAuthority> authorities = new ArrayList<>(List.of(grantedAuthority));
        doReturn(authorities).when(authentication).getAuthorities();
    }

    private PortalNavigationApi navApi(String apiId) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(apiId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private ApplicationListItem appItem(String id, String name) {
        var app = new ApplicationListItem();
        app.setId(id);
        app.setName(name);
        return app;
    }

    @Nested
    class AdminUser {

        @BeforeEach
        void setup() {
            setUpSecurityContext("ENVIRONMENT:ADMIN");
        }

        @Test
        void should_load_context_with_all_apis_and_empty_applications() {
            when(apiRepository.search(any(), any())).thenReturn(List.of(REPO_API_1, REPO_API_2, REPO_API_3));

            var context = contextLoader.load(auditInfo(ADMIN_USER_ID));

            assertThat(context.authorizedApiIds()).containsExactlyInAnyOrder("id1", "id2", "id3");
            assertThat(context.apiNamesById()).isEqualTo(
                Map.of(
                    REPO_API_1.getId(),
                    REPO_API_1.getName(),
                    REPO_API_2.getId(),
                    REPO_API_2.getName(),
                    REPO_API_3.getId(),
                    REPO_API_3.getName()
                )
            );
            assertThat(context.apiIdsByType()).isEqualTo(
                Map.of(ApiType.PROXY, Set.of("id1"), ApiType.MESSAGE, Set.of("id2"), ApiType.LLM_PROXY, Set.of("id3"))
            );
            assertThat(context.applicationNamesById()).isEmpty();
            assertThat(context.executionContext().getOrganizationId()).isEqualTo(ORG_ID);
            assertThat(context.executionContext().getEnvironmentId()).isEqualTo(ENV_ID);
            verify(visibilityDomainService, never()).resolveVisibleItems(any(), any());
            verify(visibilityDomainService, never()).resolveVisibleItems(any());
            verify(applicationService, never()).findByUser(any(), any());
        }

        @Test
        void should_return_empty_when_repository_is_empty() {
            when(apiRepository.search(any(), any())).thenReturn(Collections.emptyList());

            var context = contextLoader.load(auditInfo(ADMIN_USER_ID));

            assertThat(context.authorizedApiIds()).isEmpty();
            assertThat(context.apiNamesById()).isEmpty();
            assertThat(context.apiIdsByType()).isEmpty();
            assertThat(context.applicationNamesById()).isEmpty();
        }

        @Test
        void should_exclude_apis_with_null_type_from_apiIdsByType() {
            var apiWithoutType = Api.builder().id("id-no-type").name("no-type-api").build();
            when(apiRepository.search(any(), any())).thenReturn(List.of(REPO_API_1, apiWithoutType));

            var context = contextLoader.load(auditInfo(ADMIN_USER_ID));

            assertThat(context.authorizedApiIds()).containsExactlyInAnyOrder("id1", "id-no-type");
            assertThat(context.apiIdsByType()).isEqualTo(Map.of(ApiType.PROXY, Set.of("id1")));
        }
    }

    @Nested
    class NonAdminAuthenticatedUser {

        @BeforeEach
        void setup() {
            setUpSecurityContext("ENVIRONMENT:USER");
        }

        @Test
        void should_load_context_scoped_to_visible_portal_apis_and_user_applications() {
            when(visibilityDomainService.resolveVisibleItems(ENV_ID, NON_ADMIN_USER_ID)).thenReturn(List.of(navApi("id2"), navApi("id3")));
            when(apiRepository.search(any(), any())).thenReturn(List.of(REPO_API_2, REPO_API_3));
            when(applicationService.findByUser(any(), eq(NON_ADMIN_USER_ID))).thenReturn(
                Set.of(appItem("app1", "My App"), appItem("app2", "Another App"))
            );

            var context = contextLoader.load(auditInfo(NON_ADMIN_USER_ID));

            assertThat(context.authorizedApiIds()).containsExactlyInAnyOrder("id2", "id3");
            assertThat(context.apiNamesById()).isEqualTo(Map.of("id2", "api2", "id3", "api3"));
            assertThat(context.applicationNamesById()).isEqualTo(Map.of("app1", "My App", "app2", "Another App"));
            verify(visibilityDomainService).resolveVisibleItems(ENV_ID, NON_ADMIN_USER_ID);
            verify(applicationService).findByUser(any(), eq(NON_ADMIN_USER_ID));
        }

        @Test
        void should_return_empty_apis_when_no_visible_portal_apis() {
            when(visibilityDomainService.resolveVisibleItems(ENV_ID, NON_ADMIN_USER_ID)).thenReturn(Collections.emptyList());
            when(applicationService.findByUser(any(), eq(NON_ADMIN_USER_ID))).thenReturn(Collections.emptySet());

            var context = contextLoader.load(auditInfo(NON_ADMIN_USER_ID));

            assertThat(context.authorizedApiIds()).isEmpty();
            assertThat(context.apiNamesById()).isEmpty();
            assertThat(context.apiIdsByType()).isEmpty();
            assertThat(context.applicationNamesById()).isEmpty();
            verify(apiRepository, never()).search(any(), any());
            verify(applicationService).findByUser(any(), eq(NON_ADMIN_USER_ID));
        }

        @Test
        void should_only_return_public_apis_and_no_applications_when_userId_is_null() {
            when(visibilityDomainService.resolveVisibleItems(ENV_ID)).thenReturn(List.of(navApi("id1")));
            when(apiRepository.search(any(), any())).thenReturn(List.of(REPO_API_1));

            var context = contextLoader.load(auditInfo(null));

            assertThat(context.authorizedApiIds()).containsExactly("id1");
            assertThat(context.applicationNamesById()).isEmpty();
            verify(visibilityDomainService).resolveVisibleItems(ENV_ID);
            verify(visibilityDomainService, never()).resolveVisibleItems(any(), any());
            verify(applicationService, never()).findByUser(any(), any());
        }

        @Test
        void should_only_return_public_apis_and_no_applications_when_userId_is_blank() {
            when(visibilityDomainService.resolveVisibleItems(ENV_ID)).thenReturn(List.of(navApi("id1")));
            when(apiRepository.search(any(), any())).thenReturn(List.of(REPO_API_1));

            var context = contextLoader.load(auditInfo("  "));

            assertThat(context.authorizedApiIds()).containsExactly("id1");
            assertThat(context.applicationNamesById()).isEmpty();
            verify(visibilityDomainService).resolveVisibleItems(ENV_ID);
            verify(visibilityDomainService, never()).resolveVisibleItems(any(), any());
            verify(applicationService, never()).findByUser(any(), any());
        }
    }

    @Nested
    class OrganizationAdminUser {

        @BeforeEach
        void setup() {
            setUpSecurityContext("ORGANIZATION:ADMIN");
        }

        @Test
        void should_be_treated_as_admin_and_load_all_apis_in_environment() {
            when(apiRepository.search(any(), any())).thenReturn(List.of(REPO_API_1, REPO_API_2));

            var context = contextLoader.load(auditInfo(ADMIN_USER_ID));

            assertThat(context.authorizedApiIds()).containsExactlyInAnyOrder("id1", "id2");
            assertThat(context.applicationNamesById()).isEmpty();
            verify(visibilityDomainService, never()).resolveVisibleItems(any(), any());
            verify(visibilityDomainService, never()).resolveVisibleItems(any());
            verify(applicationService, never()).findByUser(any(), any());
        }
    }

    @Nested
    class AnonymousUserWithoutSecurityContext {

        // Intentionally does NOT call setUpSecurityContext — verifies isAdmin() is null-safe when the
        // SecurityContextHolder has no Authentication (true anonymous access to the portal).

        @Test
        void should_not_throw_and_return_public_apis_only_when_authentication_is_null() {
            when(visibilityDomainService.resolveVisibleItems(ENV_ID)).thenReturn(List.of(navApi("id1")));
            when(apiRepository.search(any(), any())).thenReturn(List.of(REPO_API_1));

            var context = contextLoader.load(auditInfo(null));

            assertThat(context.authorizedApiIds()).containsExactly("id1");
            assertThat(context.applicationNamesById()).isEmpty();
            verify(visibilityDomainService).resolveVisibleItems(ENV_ID);
            verify(visibilityDomainService, never()).resolveVisibleItems(any(), any());
            verify(applicationService, never()).findByUser(any(), any());
        }
    }
}
