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
package io.gravitee.apim.infra.adapter;

import io.gravitee.repository.management.model.ApiProduct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductAdapterTest {

    @Nested
    class RepositoryToModel {

        @Test
        void should_convert_from_v4_repository_to_core_model() {
            var repository = apiProduct().build();

            var api = ApiProductAdapter.INSTANCE.toModel(repository);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(api.getId()).isEqualTo("my-id");
                soft.assertThat(api.getEnvironmentId()).isEqualTo("env-id");
                soft.assertThat(api.getDescription()).isEqualTo("api-product-description");
                soft.assertThat(api.getVersion()).isEqualTo("1.0.0");

                soft.assertThat(api.getName()).isEqualTo("api-product-name");
                soft.assertThat(api.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
            });
        }
    }

    @Nested
    class ModelToRepository {

        @Test
        void should_convert_v4_api_to_repository() {
            var model = BASE().build();

            var api = ApiProductAdapter.INSTANCE.toRepository(model);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(api.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(api.getDescription()).isEqualTo("api-product-description");
                soft.assertThat(api.getEnvironmentId()).isEqualTo("environment-id");
                soft.assertThat(api.getId()).isEqualTo("my-api-product");
                soft.assertThat(api.getName()).isEqualTo("my-api-product");
                soft.assertThat(api.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
            });
        }
    }

    private ApiProduct.ApiProductBuilder apiProduct() {
        return ApiProduct.builder()
            .id("my-id")
            .environmentId("env-id")
            .name("api-product-name")
            .description("api-product-description")
            .version("1.0.0")
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
    }

    private io.gravitee.apim.core.api_product.model.ApiProduct.ApiProductBuilder BASE() {
        return io.gravitee.apim.core.api_product.model.ApiProduct.builder()
            .id("my-api-product")
            .name("my-api-product")
            .environmentId("environment-id")
            .description("api-product-description")
            .version("1.0.0")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()));
    }
}
