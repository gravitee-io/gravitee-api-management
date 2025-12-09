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

import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name.API;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ManagementFilterPreProcessorTest {

    private final ApiSearchService apiSearchService = mock(ApiSearchService.class);

    private final FilterPreProcessor filterPreProcessor = new ManagementFilterPreProcessor(apiSearchService);

    private final Authentication authentication = mock(Authentication.class);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        var organizationAdmin = new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return "ORGANIZATION:ADMIN";
            }
        };

        Collection<? extends GrantedAuthority> authorities = new ArrayList<>(List.of(organizationAdmin));
        doReturn(authorities).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("test-user");

        GraviteeContext.setCurrentOrganization("DEFAULT");
        GraviteeContext.setCurrentEnvironment("DEFAULT");
    }

    @Test
    void should_not_allow_access_to_any_api() {
        var content = new Page<GenericApiEntity>(List.of(), 0, 0, 0);
        when(apiSearchService.search(any(), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(content);

        var filters = filterPreProcessor.buildFilters(new MetricsContext(AuditInfo.builder().build())).filters();

        assertThat(filters).isEqualTo(List.of(new Filter(API, IN, Set.of())));
    }

    @Test
    void should_allow_access_to_all_apis() {
        GenericApiEntity api1 = newApiEntity("id1", "api1");
        GenericApiEntity api2 = newApiEntity("id2", "api2");

        var content = new Page<>(List.of(api1, api2), 0, 2, 2);
        when(apiSearchService.search(any(), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(content);

        var expectedApis = Set.of("id1", "id2");

        var contextWithFilters = filterPreProcessor.buildFilters(new MetricsContext(AuditInfo.builder().build()));

        assertThat(contextWithFilters.filters()).isNotEmpty();

        assertThat(contextWithFilters.filters()).extracting(Filter::value).containsExactlyInAnyOrder(expectedApis);
    }

    private GenericApiEntity newApiEntity(String id, String name) {
        return ApiEntity.builder().id(id).name(name).build();
    }
}
