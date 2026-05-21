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
package io.gravitee.repository.mongodb.management.internal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mongodb.MongoExecutionTimeoutException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.utils.MongoQueries;
import java.lang.reflect.Field;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class ApiMongoRepositoryImplTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Test
    @SneakyThrows
    void search_returns_total_minus_one_and_items_when_count_times_out() {
        when(mongoTemplate.count(any(Query.class), eq(ApiMongo.class))).thenThrow(
            new UncategorizedMongoDbException("Wrapped", new MongoExecutionTimeoutException(50, "operation exceeded time limit"))
        );
        ApiMongo api1 = new ApiMongo();
        api1.setId("api-1");
        when(mongoTemplate.find(any(Query.class), eq(ApiMongo.class))).thenReturn(List.of(api1));
        ApiMongoRepositoryImpl repository = buildRepository();

        Page<ApiMongo> page = repository.search(
            new ApiCriteria.Builder().build(),
            null,
            new PageableBuilder().pageNumber(0).pageSize(10).build(),
            ApiFieldFilter.allFields()
        );

        assertThat(page.getTotalElements()).isEqualTo(-1L);
        assertThat(page.getContent()).extracting(ApiMongo::getId).containsExactly("api-1");
    }

    @SneakyThrows
    private ApiMongoRepositoryImpl buildRepository() {
        ApiMongoRepositoryImpl repository = new ApiMongoRepositoryImpl();
        setField(repository, "mongoTemplate", mongoTemplate);
        setField(repository, "mongoQueries", new MongoQueries(2000L));
        return repository;
    }

    @SneakyThrows
    private static void setField(Object target, String fieldName, Object value) {
        Field field = ApiMongoRepositoryImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
        field.setAccessible(false);
    }
}
