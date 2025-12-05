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
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.PermissionsPreprocessor;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import jakarta.inject.Inject;
import java.util.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
@ExtendWith(SpringExtension.class)
class PermissionsPreprocessorImplTest {

    @Inject
    PermissionsPreprocessor permissionsPreprocessorImpl;

    @Nested
    class GetFiltersWithAllowedApisTest {

        @Test
        void should_get_filter_with_empty_list() {
            var allowedApis = new HashMap<String, String>();
            var context = new MetricsContext(null).withApiNamesById(allowedApis);

            var updatedFilters = permissionsPreprocessorImpl.buildFilterForAllowedApis(context);

            assertThat(updatedFilters).containsExactly(new Filter(API, IN, List.of()));
        }

        @Test
        void should_get_filter_with_a_single_api() {
            var allowedApi1 = UUID.randomUUID().toString();

            var allowedApis = Map.of(allowedApi1, UUID.randomUUID().toString());

            var context = new MetricsContext(null).withApiNamesById(allowedApis);

            var updatedFilters = permissionsPreprocessorImpl.buildFilterForAllowedApis(context);

            assertThat(updatedFilters).containsExactly(new Filter(API, IN, List.of(allowedApi1)));
        }

        @Test
        void should_get_filter_with_multiple_apis() {
            var allowedApi1 = UUID.randomUUID().toString();
            var allowedApi2 = UUID.randomUUID().toString();
            var allowedApi3 = UUID.randomUUID().toString();

            var allowedApis = new HashMap<String, String>();
            allowedApis.put(allowedApi1, UUID.randomUUID().toString());
            allowedApis.put(allowedApi2, UUID.randomUUID().toString());
            allowedApis.put(allowedApi3, UUID.randomUUID().toString());

            var context = new MetricsContext(null).withApiNamesById(allowedApis);

            var updatedFilters = permissionsPreprocessorImpl.buildFilterForAllowedApis(context);

            assertThat(updatedFilters)
                .singleElement()
                .satisfies(filter -> {
                    assertThat(filter.name()).isEqualTo(API);
                    assertThat(filter.operator()).isEqualTo(IN);
                    assertThat(filter.value())
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactlyInAnyOrder(allowedApi1, allowedApi2, allowedApi3);
                });
        }
    }

    @Nested
    class FindAllowedApisTest {

        @Inject
        ApiAuthorizationService apiAuthorizationService;

        @Inject
        Authentication authentication;

        @Inject
        ApiSearchService apiSearchService;

        static final String apiId1 = "api-id-1";
        static final String apiId2 = "api-id-2";
        static final String apiId3 = "api-id-3";
        static final String apiId4 = "api-id-4";

        static List<String> environmentApiIds = List.of(apiId1, apiId2, apiId3, apiId4);

        @BeforeEach
        public void setUp() {
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

            var orgAdminRole = "ORGANIZATION:ADMIN";
            GrantedAuthority organizationAdmin = () -> orgAdminRole;
            setAuthorities(organizationAdmin);

            when(apiAuthorizationService.findIdsByEnvironment("DEFAULT")).thenReturn(Set.copyOf(environmentApiIds));
        }

        @Test
        void should_not_allow_access_to_any_api() {
            var content = new Page(List.of(), 0, 0, 0);
            when(apiSearchService.search(any(), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(content);
            when(apiSearchService.search(any(), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(content);

            var allowedApis = permissionsPreprocessorImpl.findAllowedApis();

            assertThat(allowedApis).isEmpty();
        }

        @Test
        void should_allow_access_to_all_apis() {
            GenericApiEntity api1 = newApiEntity("id1", "api1");
            GenericApiEntity api2 = newApiEntity("id2", "api2");

            var content = new Page(List.of(api1, api2), 0, 2, 2);
            when(apiSearchService.search(any(), any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(content);

            var allowedApis = permissionsPreprocessorImpl.findAllowedApis();

            var expectedApis = Map.of(api1.getId(), api1.getName(), api2.getId(), api2.getName());
            assertThat(allowedApis).isEqualTo(expectedApis);
        }

        void setAuthorities(GrantedAuthority... authorities) {
            Collection authorityList = new ArrayList<>(Arrays.stream(authorities).toList());
            when(authentication.getAuthorities()).thenReturn(authorityList);
        }

        GenericApiEntity newApiEntity(String id, String name) {
            return new ApiEntity(
                id,
                null,
                null,
                name,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                null,
                null
            );
        }
    }
}
