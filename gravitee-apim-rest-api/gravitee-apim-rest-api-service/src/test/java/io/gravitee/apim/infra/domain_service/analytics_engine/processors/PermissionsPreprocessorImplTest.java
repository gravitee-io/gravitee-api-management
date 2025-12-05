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

import io.gravitee.apim.core.analytics_engine.domain_service.PermissionsPreprocessor;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    class UpdateContextFiltersTest {

        @Test
        void should_get_filter_with_empty_list() {
            var allowedApis = new HashMap<String, String>();
            var context = new MetricsContext(null).withApiNamesById(allowedApis);

            var updatedFilters = permissionsPreprocessorImpl.getFiltersWithAllowedApisIds(context);

            assertThat(updatedFilters).containsExactly(new Filter(API, IN, List.of()));
        }

        @Test
        void should_get_filter_with_a_single_api() {
            String allowedApi1 = UUID.randomUUID().toString();

            var allowedApis = Map.of(allowedApi1, UUID.randomUUID().toString());

            var context = new MetricsContext(null).withApiNamesById(allowedApis);

            var updatedFilters = permissionsPreprocessorImpl.getFiltersWithAllowedApisIds(context);

            assertThat(updatedFilters).containsExactly(new Filter(API, IN, List.of(allowedApi1)));
        }

        @Test
        void should_get_filter_with_multiple_apis() {
            String allowedApi1 = UUID.randomUUID().toString();
            String allowedApi2 = UUID.randomUUID().toString();
            String allowedApi3 = UUID.randomUUID().toString();

            var allowedApis = new HashMap<String, String>();
            allowedApis.put(allowedApi1, UUID.randomUUID().toString());
            allowedApis.put(allowedApi2, UUID.randomUUID().toString());
            allowedApis.put(allowedApi3, UUID.randomUUID().toString());

            var context = new MetricsContext(null).withApiNamesById(allowedApis);

            var updatedFilters = permissionsPreprocessorImpl.getFiltersWithAllowedApisIds(context);

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
}
