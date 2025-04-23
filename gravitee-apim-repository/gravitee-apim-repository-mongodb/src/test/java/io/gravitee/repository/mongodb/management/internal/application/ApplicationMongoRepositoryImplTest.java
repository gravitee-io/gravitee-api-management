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
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApplicationStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

public class ApplicationMongoRepositoryImplTest {

    @Test
    public void searchQuery_queryAndEnvironmentIdsAndStatus() {
        ApplicationMongoRepositoryImpl repository = new ApplicationMongoRepositoryImpl();
        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .query("id1")
            .environmentIds(Set.of("env1"))
            .status(ApplicationStatus.ACTIVE)
            .build();
        Query query = repository.buildSearchCriteria(criteria);
        assertThat(query.toString())
            .isEqualTo(
                "Query: { \"$or\" : [{ \"id\" : \"id1\"}, { \"name\" : { \"$regularExpression\" : { \"pattern\" : \"id1\", " +
                "\"options\" : \"i\"}}}], \"environmentId\" : { \"$in\" : [\"env1\"]}, \"status\" : { \"$java\" : ACTIVE } }, " +
                "Fields: {}, Sort: {}"
            );
    }

    @Test
    public void searchQuery_queryAndRestrictedIdsAndNameAndEnvironmentIdsAndStatus() {
        ApplicationMongoRepositoryImpl repository = new ApplicationMongoRepositoryImpl();
        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .query("name1")
            .restrictedToIds(Set.of("id1"))
            .name("name1")
            .environmentIds(Set.of("env1"))
            .status(ApplicationStatus.ACTIVE)
            .build();
        Query query = repository.buildSearchCriteria(criteria);
        assertThat(query.toString())
            .isEqualTo(
                "Query: { \"$or\" : [{ \"id\" : \"name1\"}, { \"name\" : { \"$regularExpression\" : { \"pattern\" : \"name1\", " +
                "\"options\" : \"i\"}}}], \"id\" : { \"$in\" : [\"id1\"]}, \"name\" : { \"$regularExpression\" : " +
                "{ \"pattern\" : \"name1\", \"options\" : \"i\"}}, \"environmentId\" : { \"$in\" : [\"env1\"]}, \"status\" : " +
                "{ \"$java\" : ACTIVE } }, Fields: {}, Sort: {}"
            );
    }
}
