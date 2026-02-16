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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.ApiProduct;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class ApiProductRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/api-product-tests/";
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        ApiProduct apiProduct = createApiProduct(uuid, date, "my-env", "my-api-product", "1.0.0", List.of("api1", "api2"));

        ApiProduct createdApiProduct = apiProductsRepository.create(apiProduct);

        assertSoftly(softly -> {
            softly.assertThat(createdApiProduct).isNotNull();
            softly.assertThat(createdApiProduct.getId()).isEqualTo(uuid);
            softly.assertThat(createdApiProduct.getEnvironmentId()).isEqualTo("my-env");
            softly.assertThat(createdApiProduct.getName()).isEqualTo("my-api-product");
            softly.assertThat(createdApiProduct.getDescription()).isEqualTo("test-description");
            softly.assertThat(createdApiProduct.getVersion()).isEqualTo("1.0.0");
            softly.assertThat(createdApiProduct.getApiIds()).containsExactly("api1", "api2");
            softly.assertThat(createdApiProduct.getCreatedAt()).isNotNull();
            softly.assertThat(createdApiProduct.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    public void shouldThrowExceptionWhenInsertSameIdApiProduct() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        ApiProduct apiProduct = createApiProduct(uuid, date, "my-env", "my-api-product", "1.0.0", List.of("api1"));

        apiProductsRepository.create(apiProduct);
        assertThatThrownBy(() -> apiProductsRepository.create(apiProduct)).isInstanceOf(Exception.class);
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";

        final Optional<ApiProduct> apiProduct = apiProductsRepository.findById(id);

        assertThat(apiProduct).isPresent();
        ApiProduct found = apiProduct.get();
        assertSoftly(softly -> {
            softly.assertThat(found.getId()).isEqualTo(id);
            softly.assertThat(found.getEnvironmentId()).isEqualTo("my-env");
            softly.assertThat(found.getName()).isEqualTo("my-api-product");
            softly.assertThat(found.getDescription()).isEqualTo("test-description");
            softly.assertThat(found.getVersion()).isEqualTo("1.0.0");
            softly.assertThat(found.getApiIds()).containsExactly("api1", "api2");
            softly.assertThat(found.getCreatedAt()).isNotNull();
            softly.assertThat(found.getCreatedAt().getTime()).isEqualTo(1_470_157_767_000L);
            softly.assertThat(found.getUpdatedAt()).isNotNull();
            softly.assertThat(found.getUpdatedAt().getTime()).isEqualTo(1_470_157_767_000L);
        });
    }

    @Test
    public void shouldReturnEmptyWhenApiProductNotFound() throws TechnicalException {
        var id = "not-existing-id";

        final Optional<ApiProduct> apiProduct = apiProductsRepository.findById(id);

        assertThat(apiProduct).isEmpty();
    }

    @Test
    public void shouldFindByEnvironmentId() throws TechnicalException {
        var apiProducts = apiProductsRepository.findByEnvironmentId("my-env");

        assertThat(apiProducts).hasSize(3).are(declaredInEnv("my-env"));
    }

    @Test
    public void shouldReturnEmptyListWhenEnvironmentIdNotFound() throws TechnicalException {
        final Set<ApiProduct> apiProducts = apiProductsRepository.findByEnvironmentId("other-env");

        assertThat(apiProducts).isEmpty();
    }

    @Test
    public void shouldFindByEnvironmentIdAndName() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";

        final Optional<ApiProduct> apiProduct = apiProductsRepository.findByEnvironmentIdAndName("my-env", "my-api-product");

        assertThat(apiProduct).isPresent();
        ApiProduct found = apiProduct.get();
        assertSoftly(softly -> {
            softly.assertThat(found.getId()).isEqualTo(id);
            softly.assertThat(found.getEnvironmentId()).isEqualTo("my-env");
            softly.assertThat(found.getName()).isEqualTo("my-api-product");
            softly.assertThat(found.getApiIds()).containsExactly("api1", "api2");
        });
    }

    @Test
    public void shouldReturnEmptyWhenEnvironmentIdAndNameNotFound() throws TechnicalException {
        final Optional<ApiProduct> apiProduct = apiProductsRepository.findByEnvironmentIdAndName("other-env", "unknown-product");

        assertThat(apiProduct).isEmpty();
    }

    @Test
    public void shouldFindByApiId() throws TechnicalException {
        var apiProducts = apiProductsRepository.findByApiId("api1");

        assertThat(apiProducts).hasSize(2).are(containingApi("api1"));
    }

    @Test
    public void shouldReturnEmptyListWhenApiIdNotFound() throws TechnicalException {
        final Set<ApiProduct> apiProducts = apiProductsRepository.findByApiId("unknown-api");

        assertThat(apiProducts).isEmpty();
    }

    @Test
    public void shouldFindByIds() throws TechnicalException {
        var existingId = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";
        Set<ApiProduct> found = apiProductsRepository.findByIds(Set.of(existingId, "non-existent-id"));

        assertThat(found).hasSize(1);
        assertThat(found.iterator().next().getId()).isEqualTo(existingId);
        assertThat(found.iterator().next().getApiIds()).containsExactly("api1", "api2");
    }

    @Test
    public void shouldReturnEmptyWhenFindByIdsWithEmptyCollection() throws TechnicalException {
        assertThat(apiProductsRepository.findByIds(Set.of())).isEmpty();
    }

    @Test
    public void shouldUpdateApiProduct() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";
        var date = new Date(1_470_157_767_000L);
        var updateDate = new Date(1_712_660_289L);
        ApiProduct apiProduct = ApiProduct.builder()
            .id(id)
            .name("my-updated-api-product")
            .description("updated-description")
            .version("2.0.0")
            .environmentId("my-env")
            .createdAt(date)
            .updatedAt(updateDate)
            .apiIds(List.of("api3", "api4"))
            .build();

        final ApiProduct updatedApiProduct = apiProductsRepository.update(apiProduct);
        assertSoftly(softly -> {
            softly.assertThat(updatedApiProduct).isNotNull();
            softly.assertThat(updatedApiProduct.getId()).isEqualTo(id);
            softly.assertThat(updatedApiProduct.getName()).isEqualTo("my-updated-api-product");
            softly.assertThat(updatedApiProduct.getDescription()).isEqualTo("updated-description");
            softly.assertThat(updatedApiProduct.getVersion()).isEqualTo("2.0.0");
            softly.assertThat(updatedApiProduct.getApiIds()).containsExactly("api3", "api4");
            softly.assertThat(updatedApiProduct.getCreatedAt()).isNotNull();
            softly.assertThat(updatedApiProduct.getCreatedAt().getTime()).isEqualTo(1_470_157_767_000L);
            softly.assertThat(updatedApiProduct.getUpdatedAt()).isNotNull();
            softly.assertThat(updatedApiProduct.getUpdatedAt().getTime()).isEqualTo(1_712_660_289L);
        });
    }

    @Test
    public void shouldThrowExceptionWhenApiProductToUpdateNotFound() {
        var id = "not-existing-id";
        var date = new Date(1_470_157_767_000L);
        ApiProduct apiProduct = createApiProduct(id, date, "my-env", "my-api-product", "1.0.0", List.of("api1"));

        assertThatThrownBy(() -> apiProductsRepository.update(apiProduct)).isInstanceOf(Exception.class);
    }

    @Test
    public void shouldDeleteApiProduct() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";

        apiProductsRepository.delete(id);

        var deletedApiProduct = apiProductsRepository.findById(id);
        assertThat(deletedApiProduct).isEmpty();
    }

    @Test
    public void shouldFindAllApiProducts() throws TechnicalException {
        Set<ApiProduct> apiProducts = apiProductsRepository.findAll();

        assertThat(apiProducts).isNotEmpty();
    }

    private static ApiProduct createApiProduct(
        String uuid,
        Date date,
        String environmentId,
        String name,
        String version,
        List<String> apiIds
    ) {
        return ApiProduct.builder()
            .id(uuid)
            .name(name)
            .description("test-description")
            .version(version)
            .environmentId(environmentId)
            .createdAt(date)
            .updatedAt(date)
            .apiIds(apiIds)
            .build();
    }

    private org.assertj.core.api.Condition<ApiProduct> declaredInEnv(String env) {
        return new org.assertj.core.api.Condition<>(e -> env.equals(e.getEnvironmentId()), "declared in env " + env);
    }

    private org.assertj.core.api.Condition<ApiProduct> containingApi(String apiId) {
        return new org.assertj.core.api.Condition<>(e -> e.getApiIds() != null && e.getApiIds().contains(apiId), "containing api " + apiId);
    }
}
