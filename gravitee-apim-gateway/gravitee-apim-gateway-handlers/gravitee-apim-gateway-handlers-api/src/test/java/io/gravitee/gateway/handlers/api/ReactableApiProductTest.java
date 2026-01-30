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
package io.gravitee.gateway.handlers.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReactableApiProductTest {

    @Test
    void should_always_be_enabled() {
        final ReactableApiProduct apiProduct = new ReactableApiProduct();
        assertThat(apiProduct.enabled()).isTrue();
    }

    @Test
    void should_return_empty_dependencies() {
        final ReactableApiProduct apiProduct = new ReactableApiProduct();
        assertThat(apiProduct.dependencies(String.class)).isEmpty();
    }

    @Test
    void should_build_with_all_fields() {
        Date now = new Date();
        ReactableApiProduct apiProduct = ReactableApiProduct.builder()
            .id("product-id")
            .name("Product Name")
            .description("Product Description")
            .version("1.0.0")
            .apiIds(Set.of("api-1", "api-2"))
            .environmentId("env-id")
            .environmentHrid("env-hrid")
            .organizationId("org-id")
            .organizationHrid("org-hrid")
            .deployedAt(now)
            .build();

        assertThat(apiProduct.getId()).isEqualTo("product-id");
        assertThat(apiProduct.getName()).isEqualTo("Product Name");
        assertThat(apiProduct.getDescription()).isEqualTo("Product Description");
        assertThat(apiProduct.getVersion()).isEqualTo("1.0.0");
        assertThat(apiProduct.getApiIds()).containsExactlyInAnyOrder("api-1", "api-2");
        assertThat(apiProduct.getEnvironmentId()).isEqualTo("env-id");
        assertThat(apiProduct.getEnvironmentHrid()).isEqualTo("env-hrid");
        assertThat(apiProduct.getOrganizationId()).isEqualTo("org-id");
        assertThat(apiProduct.getOrganizationHrid()).isEqualTo("org-hrid");
        assertThat(apiProduct.getDeployedAt()).isEqualTo(now);
    }

    @Test
    void should_handle_empty_api_ids() {
        ReactableApiProduct apiProduct = ReactableApiProduct.builder().id("product-id").apiIds(Set.of()).build();

        assertThat(apiProduct.getApiIds()).isEmpty();
    }

    @Test
    void should_handle_null_optional_fields() {
        ReactableApiProduct apiProduct = ReactableApiProduct.builder().id("product-id").name("Name").version("1.0").build();

        assertThat(apiProduct.getId()).isEqualTo("product-id");
        assertThat(apiProduct.getName()).isEqualTo("Name");
        assertThat(apiProduct.getVersion()).isEqualTo("1.0");
        assertThat(apiProduct.getDescription()).isNull();
        assertThat(apiProduct.getApiIds()).isNull();
        assertThat(apiProduct.getEnvironmentId()).isNull();
    }
}
