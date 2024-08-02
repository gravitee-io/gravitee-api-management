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
import static org.assertj.core.api.Assertions.catchException;

import io.gravitee.repository.management.model.ApiCategoryOrder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiCategoryOrderRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/apicategoryorder-tests/";
    }

    @Test
    public void shouldFindAllByCategoryId() {
        final Set<ApiCategoryOrder> optionalCategory = apiCategoryOrderRepository.findAllByCategoryId("category-1");
        assertThat(optionalCategory).hasSize(2);
    }

    @Test
    public void shouldFindAllByApiId() {
        final Set<ApiCategoryOrder> optionalCategory = apiCategoryOrderRepository.findAllByApiId("api-1");
        assertThat(optionalCategory).hasSize(1);
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<ApiCategoryOrder> optionalCategory = apiCategoryOrderRepository.findById("api-2", "category-2");
        assertThat(optionalCategory).contains(ApiCategoryOrder.builder().apiId("api-2").categoryId("category-2").order(0).build());
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<ApiCategoryOrder> optionalCategory = apiCategoryOrderRepository.findAll();
        assertThat(optionalCategory).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    public void shouldCreate() throws Exception {
        var apiCategoryOrder = ApiCategoryOrder.builder().apiId("api-3").categoryId("category-3").order(0).build();

        int nbApiCategoryOrdersBeforeCreation = apiCategoryOrderRepository.findAll().size();
        apiCategoryOrderRepository.create(apiCategoryOrder);

        var savedEntities = apiCategoryOrderRepository.findAll();
        Assert.assertEquals(nbApiCategoryOrdersBeforeCreation + 1, savedEntities.size());

        var optional = savedEntities
            .stream()
            .filter(apiCategoryOrder1 ->
                Objects.equals(apiCategoryOrder.getApiId(), apiCategoryOrder1.getApiId()) &&
                Objects.equals(apiCategoryOrder.getCategoryId(), apiCategoryOrder1.getCategoryId())
            )
            .findFirst();
        assertThat(optional).contains(apiCategoryOrder);
    }

    @Test
    public void shouldUpdate() throws Exception {
        var savedEntities = apiCategoryOrderRepository.findAll();

        var optional = savedEntities
            .stream()
            .filter(apiCategoryOrder1 ->
                Objects.equals("api-1", apiCategoryOrder1.getApiId()) && Objects.equals("category-1", apiCategoryOrder1.getCategoryId())
            )
            .findFirst();
        assertThat(optional).isPresent();

        var apiCategoryOrder = optional.get();
        apiCategoryOrder.setOrder(99);

        apiCategoryOrderRepository.update(apiCategoryOrder);

        var updatedSavedEntities = apiCategoryOrderRepository.findAll();

        Assert.assertEquals(savedEntities.size(), updatedSavedEntities.size());

        var savedOptional = savedEntities
            .stream()
            .filter(apiCategoryOrder1 ->
                Objects.equals("api-1", apiCategoryOrder1.getApiId()) && Objects.equals("category-1", apiCategoryOrder1.getCategoryId())
            )
            .findFirst();
        assertThat(savedOptional).contains(apiCategoryOrder);
    }

    @Test
    public void shouldDelete() throws Exception {
        var apiCategoryOrder = ApiCategoryOrder.builder().apiId("api-4").categoryId("category-4").order(0).build();
        apiCategoryOrderRepository.create(apiCategoryOrder);
        int nbBeforeDeletion = apiCategoryOrderRepository.findAll().size();
        apiCategoryOrderRepository.delete("api-4", List.of("category-4"));

        assertThat(apiCategoryOrderRepository.findAll()).hasSize(nbBeforeDeletion - 1);
    }

    @Test
    public void shouldNotUpdateUnknownApiCategoryOrder() {
        var unknownCategory = ApiCategoryOrder.builder().categoryId("unknown").apiId("unknown").order(0).build();
        Exception exception = catchException(() -> apiCategoryOrderRepository.update(unknownCategory));
        assertThat(exception).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        Exception exception = catchException(() -> apiCategoryOrderRepository.update(null));
        assertThat(exception).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_delete_by_api_id() throws Exception {
        var nbBeforeDeletion = apiCategoryOrderRepository.findAllByApiId("api-2").size();
        assertThat(nbBeforeDeletion).isEqualTo(2);

        List<String> deletedCategoryIds = apiCategoryOrderRepository.deleteByApiId("api-2");

        assertThat(deletedCategoryIds).containsOnly("category-1", "category-2");
        assertThat(apiCategoryOrderRepository.findAllByApiId("api-2").size()).isEqualTo(0);
    }
}
