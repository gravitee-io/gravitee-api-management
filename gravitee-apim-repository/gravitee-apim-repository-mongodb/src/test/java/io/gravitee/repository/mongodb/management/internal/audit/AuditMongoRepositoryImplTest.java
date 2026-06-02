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
package io.gravitee.repository.mongodb.management.internal.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.DeleteResult;
import io.gravitee.repository.mongodb.management.internal.model.AuditMongo;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class AuditMongoRepositoryImplTest {

    private MongoTemplate mongoTemplate;
    private AuditMongoRepositoryImpl repository;

    @BeforeEach
    public void setUp() throws Exception {
        mongoTemplate = mock(MongoTemplate.class);
        repository = new AuditMongoRepositoryImpl();

        var mongoTemplateField = AuditMongoRepositoryImpl.class.getDeclaredField("mongoTemplate");
        mongoTemplateField.setAccessible(true);
        mongoTemplateField.set(repository, mongoTemplate);

        var batchSizeField = AuditMongoRepositoryImpl.class.getDeclaredField("batchSize");
        batchSizeField.setAccessible(true);
        batchSizeField.setInt(repository, 2);

        when(mongoTemplate.remove(any(Query.class), eq(AuditMongo.class))).thenReturn(DeleteResult.acknowledged(Long.MAX_VALUE));
    }

    @Test
    public void deleteByEnvironmentIdAndAge_pages_through_results_until_exhausted() {
        AuditMongo a = auditWithId("a");
        AuditMongo b = auditWithId("b");
        AuditMongo c = auditWithId("c");

        when(mongoTemplate.find(any(Query.class), eq(AuditMongo.class)))
            .thenReturn(List.of(a, b))
            .thenReturn(List.of(c))
            .thenReturn(Collections.emptyList());
        when(mongoTemplate.remove(any(Query.class), eq(AuditMongo.class))).thenReturn(
            DeleteResult.acknowledged(2),
            DeleteResult.acknowledged(1)
        );

        repository.deleteByEnvironmentIdAndAge("env-1", Instant.parse("2026-01-01T00:00:00Z"));

        verify(mongoTemplate, times(3)).find(any(Query.class), eq(AuditMongo.class));
        verify(mongoTemplate, times(2)).remove(any(Query.class), eq(AuditMongo.class));
    }

    @Test
    public void deleteByEnvironmentIdAndAge_issues_no_delete_when_nothing_matches() {
        when(mongoTemplate.find(any(Query.class), eq(AuditMongo.class))).thenReturn(Collections.emptyList());

        repository.deleteByEnvironmentIdAndAge("env-1", Instant.parse("2026-01-01T00:00:00Z"));

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(AuditMongo.class));
        verifyNoMoreInteractions(mongoTemplate);
    }

    @Test
    public void deleteByEnvironmentIdAndAge_deletes_each_chunk_by_id() {
        AuditMongo a = auditWithId("a");
        AuditMongo b = auditWithId("b");

        when(mongoTemplate.find(any(Query.class), eq(AuditMongo.class))).thenReturn(List.of(a, b)).thenReturn(Collections.emptyList());
        when(mongoTemplate.remove(any(Query.class), eq(AuditMongo.class))).thenReturn(DeleteResult.acknowledged(2));

        repository.deleteByEnvironmentIdAndAge("env-1", Instant.parse("2026-01-01T00:00:00Z"));

        ArgumentCaptor<Query> deleteQueryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(deleteQueryCaptor.capture(), eq(AuditMongo.class));
        assertThat(deleteQueryCaptor.getValue().getQueryObject().toJson()).contains("\"a\"").contains("\"b\"");
    }

    @Test
    public void deleteByEnvironmentIdAndAge_aborts_when_remove_makes_no_progress() {
        AuditMongo a = auditWithId("a");
        AuditMongo b = auditWithId("b");

        when(mongoTemplate.find(any(Query.class), eq(AuditMongo.class))).thenReturn(List.of(a, b));
        when(mongoTemplate.remove(any(Query.class), eq(AuditMongo.class))).thenReturn(DeleteResult.acknowledged(0));

        repository.deleteByEnvironmentIdAndAge("env-1", Instant.parse("2026-01-01T00:00:00Z"));

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(AuditMongo.class));
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(AuditMongo.class));
    }

    private static AuditMongo auditWithId(String id) {
        AuditMongo audit = new AuditMongo();
        audit.setId(id);
        return audit;
    }
}
