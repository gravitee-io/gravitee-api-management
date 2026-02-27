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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
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
class ManagementFilterPreProcessorTest {

    record TestCase(String userId, String role, List<String> expectedApiIds) {}

    private final ApiAuthorizationService apiAuthorizationService = mock(ApiAuthorizationService.class);
    private final ApiRepository apiRepository = mock(ApiRepository.class);
    private final Authentication authentication = mock(Authentication.class);

    private final FilterPreProcessor filterPreProcessor = new ManagementFilterPreProcessor(apiAuthorizationService, apiRepository);

    // Test data
    private static final Api api1 = Api.builder().id("id1").name("api1").build();
    private static final Api api2 = Api.builder().id("id2").name("api2").build();
    private static final Api api3 = Api.builder().id("id3").name("api3").build();
    private static final Api apiNotFound = Api.builder().id("1").name("1").build();

    private static final String adminUserId = UUID.randomUUID().toString();
    private static final List<Api> adminApis = List.of(api1, api2, api3, apiNotFound);

    private static final String nonAdminUserId = UUID.randomUUID().toString();
    private static final List<Api> nonAdminApis = List.of(api2, apiNotFound);

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

        var contextWithFilters = filterPreProcessor.buildFilters(new MetricsContext(auditInfo));

        assertThat(contextWithFilters.filters()).size().isEqualTo(1);

        var value = contextWithFilters.filters().getFirst().value();
        assertThat(value)
            .isInstanceOf(Set.class)
            .asInstanceOf(InstanceOfAssertFactories.SET)
            .containsExactlyInAnyOrderElementsOf(testCase.expectedApiIds);
    }

    private static Stream<TestCase> testCases() {
        return Stream.of(
            new TestCase(adminUserId, "ORGANIZATION:ADMIN", apiIds(adminApis)),
            new TestCase(nonAdminUserId, "ORGANIZATION:USER", apiIds(nonAdminApis))
        );
    }

    private static List<String> apiIds(List<Api> apis) {
        return apis.stream().map(Api::getId).toList();
    }
}
