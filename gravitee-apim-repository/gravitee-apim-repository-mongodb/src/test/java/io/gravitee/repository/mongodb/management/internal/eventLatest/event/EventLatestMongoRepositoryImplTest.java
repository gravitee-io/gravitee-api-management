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
package io.gravitee.repository.mongodb.management.internal.eventLatest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.mongodb.management.internal.model.EventLatestMongo;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class EventLatestMongoRepositoryImplTest {

    private static final EventCriteria CRITERIA = EventCriteria.builder().types(Set.of(EventType.PUBLISH_API)).build();
    private static final IndexInfo SEEK_INDEX = new IndexInfo(
        List.of(
            IndexField.create("type", Sort.Direction.ASC),
            IndexField.create("updatedAt", Sort.Direction.ASC),
            IndexField.create("_id", Sort.Direction.ASC)
        ),
        "t1u1i1",
        false,
        false,
        null
    );

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IndexOperations indexOperations;

    @InjectMocks
    private EventLatestMongoRepositoryImpl cut;

    @BeforeEach
    void setUp() {
        when(mongoTemplate.indexOps(EventLatestMongo.class)).thenReturn(indexOperations);
    }

    @Test
    void should_refuse_to_seek_when_the_indexes_cannot_be_listed() {
        when(indexOperations.getIndexInfo()).thenThrow(new PermissionDeniedDataAccessException("not authorized", null));

        assertThatThrownBy(() -> cut.searchAfter(CRITERIA, Event.EventProperties.API_ID, null, 2)).isInstanceOf(
            UnsupportedOperationException.class
        );
    }

    @Test
    void should_not_list_the_indexes_again_once_the_seek_index_is_found() {
        when(indexOperations.getIndexInfo()).thenReturn(List.of(SEEK_INDEX));
        when(mongoTemplate.find(any(Query.class), eq(EventLatestMongo.class))).thenReturn(List.of());

        cut.searchAfter(CRITERIA, Event.EventProperties.API_ID, null, 2);
        cut.searchAfter(CRITERIA, Event.EventProperties.API_ID, null, 2);

        verify(indexOperations, times(1)).getIndexInfo();
    }

    @Test
    void should_seek_as_soon_as_a_missing_index_exists() {
        when(indexOperations.getIndexInfo()).thenReturn(List.of()).thenReturn(List.of(SEEK_INDEX));
        when(mongoTemplate.find(any(Query.class), eq(EventLatestMongo.class))).thenReturn(List.of());

        assertThatThrownBy(() -> cut.searchAfter(CRITERIA, Event.EventProperties.API_ID, null, 2)).isInstanceOf(
            UnsupportedOperationException.class
        );

        assertThat(cut.searchAfter(CRITERIA, Event.EventProperties.API_ID, null, 2)).isEmpty();
    }
}
