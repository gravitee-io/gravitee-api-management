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
package io.gravitee.repository.mongodb.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.mongodb.management.internal.model.PortalMongo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

@ExtendWith(MockitoExtension.class)
class MongoPortalRepositoryTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MongoPortalRepository repository;

    @Test
    void should_wrap_a_name_update_failure_and_keep_the_cause() {
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(
            mongoTemplate.findAndModify(
                any(Query.class),
                any(UpdateDefinition.class),
                any(FindAndModifyOptions.class),
                eq(PortalMongo.class)
            )
        ).thenThrow(failure);

        assertThatThrownBy(() -> repository.updateName("portal1", "env1", "Renamed"))
            .isInstanceOf(TechnicalException.class)
            .hasCause(failure);
    }
}
