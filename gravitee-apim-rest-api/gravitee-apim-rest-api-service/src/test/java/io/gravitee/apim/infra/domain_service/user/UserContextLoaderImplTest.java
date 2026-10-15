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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.user.model.UserContext;
<<<<<<< HEAD
import io.gravitee.apim.infra.domain_service.analytics_engine.mapper.ApiMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
=======
import io.gravitee.rest.api.service.common.ExecutionContext;
>>>>>>> 9292cf4 (fix(observability): scope analytics and logs to readable APIs)
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
<<<<<<< HEAD
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
=======
import java.util.Optional;
>>>>>>> 9292cf4 (fix(observability): scope analytics and logs to readable APIs)
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
<<<<<<< HEAD
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
=======
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
>>>>>>> 9292cf4 (fix(observability): scope analytics and logs to readable APIs)
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
<<<<<<< HEAD
=======
@ExtendWith(MockitoExtension.class)
>>>>>>> 9292cf4 (fix(observability): scope analytics and logs to readable APIs)
class UserContextLoaderImplTest {

    private static final String USER_ID = "user-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");

    private static final Api API_1 = Api.builder().id("id1").name("api1").build();
    private static final Api API_2 = Api.builder().id("id2").name("api2").build();

    @Mock
<<<<<<< HEAD
    private ApiRepository apiRepository;
=======
    private EnvironmentAnalyticsApisDomainService readableApis;
>>>>>>> 9292cf4 (fix(observability): scope analytics and logs to readable APIs)

    @Mock
    private Authentication authentication;

    private UserContextLoaderImpl userContextLoader;
    private AutoCloseable closeable;

    private final UserContext userContext = new UserContext(
        AuditInfo.builder().organizationId("DEFAULT").environmentId("DEFAULT").actor(AuditActor.builder().userId(USER_ID).build()).build()
    );

    @BeforeEach
    void setUp() {
<<<<<<< HEAD
        closeable = MockitoAnnotations.openMocks(this);
        userContextLoader = new UserContextLoaderImpl(apiAuthorizationService, apiRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
=======
        userContextLoader = new UserContextLoaderImpl(readableApis);
>>>>>>> 9292cf4 (fix(observability): scope analytics and logs to readable APIs)
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

<<<<<<< HEAD
    private void setUpSecurityContext(String role) {
=======
    private void givenAuthenticatedWith(String role) {
>>>>>>> 9292cf4 (fix(observability): scope analytics and logs to readable APIs)
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
        Collection<? extends GrantedAuthority> authorities = new ArrayList<>(List.of((GrantedAuthority) () -> role));
        doReturn(authorities).when(authentication).getAuthorities();
    }

    @Nested
    class LoadApis {

        @Test
        void should_load_the_apis_read_as_an_admin() {
            givenAuthenticatedWith("ORGANIZATION:ADMIN");
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, true, ApiDataKind.PER_REQUEST)).thenReturn(
                List.of(API_1, API_2)
            );

            var context = userContextLoader.loadApis(userContext, ApiDataKind.PER_REQUEST);

            assertThat(context.apis()).hasValue(List.of(API_1, API_2));
            assertThat(context.apiNameById()).hasValue(Map.of("id1", "api1", "id2", "api2"));
        }

        @Test
        void should_load_the_apis_the_user_may_read_data_of_the_asked_kind_from() {
            givenAuthenticatedWith("ORGANIZATION:USER");
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.AGGREGATES)).thenReturn(List.of(API_2));
            when(readableApis.findReadableApis(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST)).thenReturn(List.of());

            var aggregates = userContextLoader.loadApis(userContext, ApiDataKind.AGGREGATES);
            var requests = userContextLoader.loadApis(userContext, ApiDataKind.PER_REQUEST);

            assertThat(aggregates.apis()).hasValue(List.of(API_2));
            assertThat(aggregates.apiNameById()).hasValue(Map.of("id2", "api2"));
            assertThat(requests.apis()).hasValue(List.of());
            assertThat(requests.apiNameById()).hasValue(Map.of());
        }
    }
<<<<<<< HEAD
=======

    @Nested
    class LoadOneApi {

        @Test
        void should_load_the_api_when_the_user_may_read_it() {
            givenAuthenticatedWith("ORGANIZATION:USER");
            when(readableApis.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST, "id2")).thenReturn(
                Optional.of(API_2)
            );

            var context = userContextLoader.loadApi(userContext, "id2", ApiDataKind.PER_REQUEST);

            assertThat(context.apis()).hasValue(List.of(API_2));
            assertThat(context.apiNameById()).hasValue(Map.of("id2", "api2"));
        }

        /** Indistinguishable from an API that does not exist, so a caller cannot probe for ids. */
        @Test
        void should_yield_nothing_when_the_user_may_not_read_the_api() {
            givenAuthenticatedWith("ORGANIZATION:USER");
            when(readableApis.findReadableApi(EXECUTION_CONTEXT, USER_ID, false, ApiDataKind.PER_REQUEST, "id1")).thenReturn(
                Optional.empty()
            );

            var context = userContextLoader.loadApi(userContext, "id1", ApiDataKind.PER_REQUEST);

            assertThat(context.apis()).hasValue(List.of());
            assertThat(context.apiNameById()).hasValue(Map.of());
        }

        @Test
        void should_load_any_api_as_an_admin() {
            givenAuthenticatedWith("ORGANIZATION:ADMIN");
            when(readableApis.findReadableApi(EXECUTION_CONTEXT, USER_ID, true, ApiDataKind.PER_REQUEST, "id1")).thenReturn(
                Optional.of(API_1)
            );

            var context = userContextLoader.loadApi(userContext, "id1", ApiDataKind.PER_REQUEST);

            assertThat(context.apis()).hasValue(List.of(API_1));
        }
    }
>>>>>>> 9292cf4 (fix(observability): scope analytics and logs to readable APIs)
}
