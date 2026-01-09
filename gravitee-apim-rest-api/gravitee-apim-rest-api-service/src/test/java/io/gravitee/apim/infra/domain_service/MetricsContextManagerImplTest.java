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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.domain_service.analytics_engine.MetricsContextManagerImpl;
import io.gravitee.apim.infra.domain_service.analytics_engine.mapper.ApiMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricsContextManagerImplTest {

    record TestCase(
        String userId,
        String role,
        List<io.gravitee.apim.core.api.model.Api> expectedApis,
        Map<String, String> expectedApiNamesById
    ) {}

    private final ApiAuthorizationService apiAuthorizationService = mock(ApiAuthorizationService.class);
    private final ApiRepository apiRepository = mock(ApiRepository.class);
    private final Authentication authentication = mock(Authentication.class);

    private final MetricsContextManagerImpl metricsContextManager = new MetricsContextManagerImpl(apiAuthorizationService, apiRepository);

    // Test data
    private static final Api api1 = Api.builder().id("id1").name("api1").build();
    private static final Api api2 = Api.builder().id("id2").name("api2").build();
    private static final Api api3 = Api.builder().id("id3").name("api3").build();

    private static final String adminUserId = UUID.randomUUID().toString();
    private static final List<Api> adminApis = List.of(api1, api2, api3);

    private static final String nonAdminUserId = UUID.randomUUID().toString();
    private static final List<Api> nonAdminApis = List.of(api2);

    @BeforeEach
    void setUp() {
        when(apiAuthorizationService.findApiIdsByUserId(any(), eq(adminUserId), any(), anyBoolean())).thenThrow(
            new RuntimeException("should not be called")
        );

        when(apiAuthorizationService.findApiIdsByUserId(any(), eq(nonAdminUserId), any(), anyBoolean())).thenReturn(
            nonAdminApis.stream().map(Api::getId).collect(Collectors.toSet())
        );

        when(apiRepository.search(any(), any())).thenAnswer(invocation -> {
            ApiCriteria criteria = invocation.getArgument(0);
            if (criteria.getIds() == null) {
                return adminApis;
            }

            return nonAdminApis;
        });
    }

    AuditInfo buildAuditInfo(String userId) {
        var actor = AuditActor.builder().userId(userId).build();
        return AuditInfo.builder().organizationId("DEFAULT").environmentId("DEFAULT").actor(actor).build();
    }

    void setUpSecurityContext(String role) {
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

    @ParameterizedTest
    @MethodSource("testCases")
    void should_return_allowed_apis(TestCase testCase) {
        var auditInfo = buildAuditInfo(testCase.userId);
        setUpSecurityContext(testCase.role);

        var contextWithFilters = metricsContextManager.loadApis(new MetricsContext(auditInfo));

        assertThat(contextWithFilters.apis())
            .isPresent()
            .hasValueSatisfying(s -> assertThat(s).containsAll(testCase.expectedApis));
        assertThat(contextWithFilters.apiNameById())
            .isPresent()
            .hasValueSatisfying(s -> assertThat(s).containsAllEntriesOf(testCase.expectedApiNamesById));
    }

    private static Stream<TestCase> testCases() {
        return Stream.of(
            new TestCase(
                adminUserId,
                "ORGANIZATION:ADMIN",
                ApiMapper.INSTANCE.map(adminApis),
                Map.of(api1.getId(), api1.getName(), api2.getId(), api2.getName(), api3.getId(), api3.getName())
            ),
            new TestCase(nonAdminUserId, "ORGANIZATION:USER", ApiMapper.INSTANCE.map(nonAdminApis), Map.of(api2.getId(), api2.getName()))
        );
    }
}
