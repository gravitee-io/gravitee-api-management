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
package io.gravitee.repository.mongodb.management.internal.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.mongodb.management.internal.model.PageMongo;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

@ExtendWith(MockitoExtension.class)
class PageMongoRepositoryImplTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MongoTemplate mongoTemplate;

    @Captor
    ArgumentCaptor<Query> queryCaptor;

    @Captor
    ArgumentCaptor<UpdateDefinition> updateCaptor;

    @Test
    @SneakyThrows
    void unsetHomepage() {
        PageMongoRepositoryImpl repository = buildRepository();
        // When
        repository.unsetHomepage(List.of("id1", "id2", "id3"));
        // Then
        verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(PageMongo.class));

        JsonNode jsonQuery = MAPPER.readTree(queryCaptor.getValue().getQueryObject().toJson());
        assertThat(jsonQuery).isEqualTo(
            MAPPER.readTree(
                """
                {"_id":{"$in":["id1","id2","id3"]}}
                """
            )
        );

        JsonNode jsonUpd = MAPPER.readTree(updateCaptor.getValue().getUpdateObject().toJson());
        assertThat(jsonUpd).isEqualTo(
            MAPPER.readTree(
                """
                {"$set":{"homepage":false}}
                """
            )
        );
    }

    @Test
    @SneakyThrows
    void searchByAccessControlGroupId() {
        PageMongoRepositoryImpl repository = buildRepository();
        when(mongoTemplate.find(any(Query.class), eq(PageMongo.class))).thenReturn(Collections.emptyList());

        PageCriteria criteria = new PageCriteria.Builder().referenceType("API").accessControlGroupId("my-group").build();
        repository.search(criteria);

        verify(mongoTemplate).find(queryCaptor.capture(), eq(PageMongo.class));

        JsonNode jsonQuery = MAPPER.readTree(queryCaptor.getValue().getQueryObject().toJson());
        assertThat(jsonQuery).isEqualTo(
            MAPPER.readTree(
                """
                {"referenceType":"API","accessControls":{"$elemMatch":{"$and":[{"referenceId":"my-group"},{"referenceType":"GROUP"}]}}}
                """
            )
        );
    }

    @SneakyThrows
    private PageMongoRepositoryImpl buildRepository() {
        PageMongoRepositoryImpl repository = new PageMongoRepositoryImpl();
        Field field = PageMongoRepositoryImpl.class.getDeclaredField("mongoTemplate");
        field.setAccessible(true);
        field.set(repository, mongoTemplate);
        field.setAccessible(false);
        return repository;
    }
}
