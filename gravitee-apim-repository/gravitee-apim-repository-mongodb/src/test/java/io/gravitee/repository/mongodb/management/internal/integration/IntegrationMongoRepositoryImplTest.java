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
package io.gravitee.repository.mongodb.management.internal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.mongodb.management.internal.model.IntegrationMongo;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class IntegrationMongoRepositoryImplTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MongoTemplate mongoTemplate;

    @InjectMocks
    IntegrationMongoRepositoryImpl sut;

    @Captor
    ArgumentCaptor<Query> query;

    @Test
    @SneakyThrows
    void findAllByEnvironmentIdAndGroups() {
        // Given
        String environmentId = "env-1";
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(5).build();
        Collection<String> groups = Set.of("grp-1");
        given(mongoTemplate.count(query.capture(), eq(IntegrationMongo.class))).willReturn(0L);
        given(mongoTemplate.find(query.capture(), eq(IntegrationMongo.class))).willReturn(List.of());
        // When
        Page<IntegrationMongo> result = sut.findAllByEnvironmentIdAndGroups(environmentId, pageable, Set.of(), groups);

        // Then
        assertThat(result.getContent()).isEmpty();

        JsonNode jsonQuery = MAPPER.readTree(query.getValue().getQueryObject().toJson());
        assertThat(jsonQuery)
            .isEqualTo(
                MAPPER.readTree(
                    """
                {
                  "$and": [
                    { "environmentId": "env-1" },
                    { "$or" : [
                      { "groups": "grp-1" }
                    ]}
                  ]
                }
                """
                )
            );
    }

    @Test
    @SneakyThrows
    void findAllByEnvironmentIdAndGroupsAndId() {
        // Given
        String environmentId = "env-1";
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(5).build();
        Collection<String> groups = Set.of("grp-1");
        given(mongoTemplate.count(query.capture(), eq(IntegrationMongo.class))).willReturn(0L);
        given(mongoTemplate.find(query.capture(), eq(IntegrationMongo.class))).willReturn(List.of());
        // When
        Page<IntegrationMongo> result = sut.findAllByEnvironmentIdAndGroups(environmentId, pageable, Set.of("id1"), groups);

        // Then
        assertThat(result.getContent()).isEmpty();

        JsonNode jsonQuery = MAPPER.readTree(query.getValue().getQueryObject().toJson());
        assertThat(jsonQuery)
            .isEqualTo(
                MAPPER.readTree(
                    """
                            {
                              "$and": [
                                { "environmentId": "env-1" },
                                { "$or" : [
                                  { "_id": {"$in": ["id1"]} },
                                  { "groups": "grp-1" }
                                ]}
                              ]
                            }
                            """
                )
            );
    }
}
