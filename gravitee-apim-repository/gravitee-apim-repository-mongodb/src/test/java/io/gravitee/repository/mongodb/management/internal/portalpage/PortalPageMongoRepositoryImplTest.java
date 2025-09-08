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
package io.gravitee.repository.mongodb.management.internal.portalpage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.model.ExpandsViewContext;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageMongo;
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

@ExtendWith(MockitoExtension.class)
class PortalPageMongoRepositoryImplTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MongoTemplate mongoTemplate;

    @Captor
    ArgumentCaptor<Query> queryCaptor;

    @Test
    @SneakyThrows
    void should_include_expand_field_when_non_blank() {
        PortalPageMongoRepositoryImpl repository = buildRepository();
        // When
        repository.findPortalPagesByIdWithExpand(List.of("id1", "id2"), Collections.singletonList(ExpandsViewContext.CONTENT));
        // Then
        verify(mongoTemplate).find(queryCaptor.capture(), eq(PortalPageMongo.class));

        JsonNode jsonQuery = MAPPER.readTree(queryCaptor.getValue().getQueryObject().toJson());
        assertThat(jsonQuery).isEqualTo(
            MAPPER.readTree(
                """
                {"_id":{"$in":["id1","id2"]}}
                """
            )
        );

        JsonNode jsonFields = MAPPER.readTree(queryCaptor.getValue().getFieldsObject().toJson());
        assertThat(jsonFields).isEqualTo(
            MAPPER.readTree(
                """
                {"_id":1,"content":1}
                """
            )
        );
    }

    @Test
    @SneakyThrows
    void should_only_include_id_when_expand_blank() {
        PortalPageMongoRepositoryImpl repository = buildRepository();
        // When
        repository.findPortalPagesByIdWithExpand(List.of("id1", "id2"), Collections.emptyList());
        // Then
        verify(mongoTemplate).find(queryCaptor.capture(), eq(PortalPageMongo.class));

        JsonNode jsonQuery = MAPPER.readTree(queryCaptor.getValue().getQueryObject().toJson());
        assertThat(jsonQuery).isEqualTo(
            MAPPER.readTree(
                """
                {"_id":{"$in":["id1","id2"]}}
                """
            )
        );

        JsonNode jsonFields = MAPPER.readTree(queryCaptor.getValue().getFieldsObject().toJson());
        assertThat(jsonFields).isEqualTo(
            MAPPER.readTree(
                """
                {"_id":1}
                """
            )
        );
    }

    @SneakyThrows
    private PortalPageMongoRepositoryImpl buildRepository() {
        PortalPageMongoRepositoryImpl repository = new PortalPageMongoRepositoryImpl();
        Field field = PortalPageMongoRepositoryImpl.class.getDeclaredField("mongoTemplate");
        field.setAccessible(true);
        field.set(repository, mongoTemplate);
        field.setAccessible(false);
        return repository;
    }
}
