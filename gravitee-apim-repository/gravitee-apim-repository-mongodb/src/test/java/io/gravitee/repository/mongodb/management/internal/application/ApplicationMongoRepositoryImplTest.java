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
package io.gravitee.repository.mongodb.management.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.model.ApplicationStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

public class ApplicationMongoRepositoryImplTest {

    @Test
    public void buildSearchCriteria_withIdsAndNameAndEnvironmentIdsAndStatus() throws JsonProcessingException {
        ApplicationMongoRepositoryImpl repository = new ApplicationMongoRepositoryImpl();
        ApplicationCriteria criteria = new ApplicationCriteria.Builder()
            .ids(Set.of("id1"))
            .name("name1")
            .environmentIds("DEFAULT")
            .status(ApplicationStatus.ACTIVE)
            .build();
        Query query = repository.buildSearchCriteria(criteria);
        var queryJson = new ObjectMapper().writeValueAsString(query.getQueryObject());
        assertThat(queryJson)
            .isEqualTo(
                "{\"$or\":[{\"id\":{\"$in\":[\"id1\"]}},{\"name\":\"name1\"}]," +
                "\"environmentId\":{\"$in\":[\"DEFAULT\"]},\"status\":\"ACTIVE\"}"
            );
    }
}
