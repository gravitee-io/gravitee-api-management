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
package io.gravitee.apim.infra.domain_service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.infra.domain_service.analytics_engine.mapper.ApiMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
class UserContextLoaderImplTest {

    @Mock
    private ApiAuthorizationService apiAuthorizationService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private Authentication authentication;

    private UserContextLoaderImpl userContextLoader;
    private AutoCloseable closeable;

    // Test data
    private static final Api API_1 = Api.builder().id("id1").name("api1").build();
    private static final Api API_2 = Api.builder().id("id2").name("api2").build();
    private static final Api API_3 = Api.builder().id("id3").name("api3").build();

    private static final String ADMIN_USER_ID = UUID.randomUUID().toString();
    private static final List<Api> ALL_APIS = List.of(API_1, API_2, API_3);

    private static final String NON_ADMIN_USER_ID = UUID.randomUUID().toString();
    private static final List<Api> NON_ADMIN_APIS = List.of(API_2);
    private static final Set<String> NON_ADMIN_API_IDS = NON_ADMIN_APIS.stream().map(Api::getId).collect(Collectors.toSet());
    private AuditInfo auditInfo;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        userContextLoader = new UserContextLoaderImpl(apiAuthorizationService, apiRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    private AuditInfo auditInfo(String userId) {
        var actor = AuditActor.builder().userId(userId).build();
        return AuditInfo.builder().organizationId("DEFAULT").environmentId("DEFAULT").actor(actor).build();
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

    @Nested
    class AdminUser {

        @BeforeEach
        void setup() {
            setUpSecurityContext("ORGANIZATION:ADMIN");
            auditInfo = auditInfo(ADMIN_USER_ID);
        }

        @Test
        void should_load_context_with_all_apis() {
            // Given
            when(apiRepository.search(any(), any())).thenReturn(ALL_APIS);

            // When
            var context = userContextLoader.loadApis(new UserContext(auditInfo));

            // Then
            assertThat(context.apis()).isPresent().hasValue(ApiMapper.INSTANCE.map(ALL_APIS));
            assertThat(context.apiNameById())
                .isPresent()
                .hasValue(Map.of(API_1.getId(), API_1.getName(), API_2.getId(), API_2.getName(), API_3.getId(), API_3.getName()));
            verify(apiAuthorizationService, never()).findApiIdsByUserId(any(), any(), any(), anyBoolean());
        }

        @Test
        void should_return_empty_list_when_repository_is_empty() {
            // Given
            when(apiRepository.search(new ApiCriteria.Builder().build(), null)).thenReturn(Collections.emptyList());

            // When
            var context = userContextLoader.loadApis(new UserContext(auditInfo));

            // Then
            assertThat(context.apis()).hasValue(Collections.emptyList());
            assertThat(context.apiNameById()).hasValue(Collections.emptyMap());
            verify(apiAuthorizationService, never()).findApiIdsByUserId(any(), any(), any(), anyBoolean());
        }
    }

    @Nested
    class NonAdminUser {

        @BeforeEach
        void setup() {
            setUpSecurityContext("ORGANIZATION:USER");
            auditInfo = auditInfo(NON_ADMIN_USER_ID);
        }

        @Test
        void should_load_context_with_authorized_apis() {
            // Given
            when(apiAuthorizationService.findApiIdsByUserId(any(), any(), any(), anyBoolean())).thenReturn(NON_ADMIN_API_IDS);
            when(apiRepository.search(any(), any())).thenReturn(NON_ADMIN_APIS);

            // When
            var context = userContextLoader.loadApis(new UserContext(auditInfo));

            // Then
            var userIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(apiAuthorizationService).findApiIdsByUserId(any(), userIdCaptor.capture(), any(), eq(true));
            assertThat(userIdCaptor.getValue()).isEqualTo(NON_ADMIN_USER_ID);

            assertThat(context.apis()).isPresent().hasValue(ApiMapper.INSTANCE.map(NON_ADMIN_APIS));
            assertThat(context.apiNameById()).isPresent().hasValue(Map.of(API_2.getId(), API_2.getName()));
        }

        @Test
        void should_return_empty_list_when_no_apis_are_authorized() {
            // Given
            when(apiAuthorizationService.findApiIdsByUserId(any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
            when(apiRepository.search(any(), any())).thenReturn(Collections.emptyList());

            // When
            var context = userContextLoader.loadApis(new UserContext(auditInfo));

            // Then
            var userIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(apiAuthorizationService).findApiIdsByUserId(any(), userIdCaptor.capture(), any(), eq(true));
            assertThat(userIdCaptor.getValue()).isEqualTo(NON_ADMIN_USER_ID);

            assertThat(context.apis()).hasValue(Collections.emptyList());
            assertThat(context.apiNameById()).hasValue(Collections.emptyMap());
        }
    }
}
