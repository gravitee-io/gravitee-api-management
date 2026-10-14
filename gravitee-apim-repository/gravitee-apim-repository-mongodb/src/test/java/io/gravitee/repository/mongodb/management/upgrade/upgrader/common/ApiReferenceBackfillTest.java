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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import java.util.ArrayList;
import java.util.List;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Amazon DocumentDB rejects updates with an aggregation pipeline ("Pipeline based document update", code 303), which a real
 * MongoDB (the one of the integration tests) accepts. These tests pin the way the writes are issued, so that a pipeline
 * update cannot sneak back in unnoticed.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiReferenceBackfillTest {

    private MongoCollection<Document> collection;
    private MongoCursor<Document> cursor;
    private final List<WriteModel<Document>> written = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        collection = mock(MongoCollection.class);
        cursor = mock(MongoCursor.class);
        FindIterable<Document> findIterable = mock(FindIterable.class);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.projection(any())).thenReturn(findIterable);
        when(findIterable.batchSize(Mockito.anyInt())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(cursor);
        written.clear();
        var acknowledged = bulkResult(true);
        // the helper reuses and clears its batch list, so the models are copied at call time
        when(collection.bulkWrite(anyList(), any(BulkWriteOptions.class))).thenAnswer(inv -> {
            written.addAll(inv.<List<WriteModel<Document>>>getArgument(0));
            return acknowledged;
        });
    }

    private static BulkWriteResult bulkResult(boolean acknowledged) {
        var result = mock(BulkWriteResult.class);
        when(result.wasAcknowledged()).thenReturn(acknowledged);
        when(result.getModifiedCount()).thenReturn(1);
        return result;
    }

    private void cursorReturns(int count) {
        var docs = new ArrayList<Document>();
        for (int i = 0; i < count; i++) {
            docs.add(new Document("_id", "id-" + i).append("api", "api-" + i));
        }
        var it = docs.iterator();
        when(cursor.hasNext()).thenAnswer(inv -> it.hasNext());
        when(cursor.next()).thenAnswer(inv -> it.next());
    }

    @Test
    void should_never_use_an_update_with_aggregation_pipeline() {
        cursorReturns(ApiReferenceBackfill.BATCH_SIZE + 3);

        ApiReferenceBackfill.apply(collection);

        // no pipeline update through the collection methods...
        verify(collection, never()).updateMany(any(Bson.class), anyList());
        verify(collection, never()).updateOne(any(Bson.class), anyList());
        verify(collection, never()).updateMany(any(Bson.class), any(Bson.class));
        // ...nor through the bulkWrite models, which DocumentDB rejects the same way
        assertThat(written).hasSize(ApiReferenceBackfill.BATCH_SIZE + 3);
        assertThat(written).allSatisfy(model -> {
            assertThat(model).isInstanceOf(UpdateOneModel.class);
            var updateModel = (UpdateOneModel<Document>) model;
            assertThat(updateModel.getUpdatePipeline()).as("update pipeline (not supported by DocumentDB)").isNull();
            assertThat(updateModel.getUpdate()).as("plain update document").isNotNull();
        });
    }

    @Test
    void should_write_plain_set_operators_copying_api_into_reference_id() {
        cursorReturns(1);

        ApiReferenceBackfill.apply(collection);

        assertThat(written).hasSize(1);
        var model = (UpdateOneModel<Document>) written.get(0);
        assertThat(model.getUpdatePipeline()).isNull();
        var update = model.getUpdate().toBsonDocument();
        assertThat(update.getDocument("$set").getString("referenceType").getValue()).isEqualTo("API");
        assertThat(update.getDocument("$set").getString("referenceId").getValue()).isEqualTo("api-0");
        assertThat(model.getFilter().toBsonDocument().toJson()).contains("\"_id\"", "id-0");
    }

    @Test
    void should_only_update_a_document_that_still_has_no_reference_type() {
        cursorReturns(1);

        ApiReferenceBackfill.apply(collection);

        // the write filter re-checks "no referenceType yet", so a document that got one meanwhile is left alone
        var filter = ((UpdateOneModel<Document>) written.get(0)).getFilter().toBsonDocument();
        assertThat(filter.toJson()).contains("\"referenceType\"", "$exists", "$or");
    }

    @Test
    void should_flush_in_batches() {
        cursorReturns(ApiReferenceBackfill.BATCH_SIZE * 2 + 5);

        ApiReferenceBackfill.apply(collection);

        verify(collection, Mockito.times(3)).bulkWrite(anyList(), any(BulkWriteOptions.class));
        assertThat(written).hasSize(ApiReferenceBackfill.BATCH_SIZE * 2 + 5);
    }

    @Test
    void should_not_write_anything_when_nothing_to_upgrade() {
        cursorReturns(0);

        var result = ApiReferenceBackfill.apply(collection);

        assertThat(result).isTrue();
        verify(collection, never()).bulkWrite(anyList(), any(BulkWriteOptions.class));
    }

    @Test
    void should_return_false_when_a_write_is_not_acknowledged() {
        cursorReturns(1);
        var notAcknowledged = bulkResult(false);
        when(collection.bulkWrite(anyList(), any(BulkWriteOptions.class))).thenReturn(notAcknowledged);

        assertThat(ApiReferenceBackfill.apply(collection)).isFalse();
    }

    @Test
    void should_propagate_write_errors_instead_of_swallowing_them() {
        cursorReturns(1);
        var error = new MongoWriteException(new WriteError(303, "boom", new BsonDocument()), new ServerAddress());
        when(collection.bulkWrite(anyList(), any(BulkWriteOptions.class))).thenThrow(error);

        assertThatThrownBy(() -> ApiReferenceBackfill.apply(collection)).isSameAs(error);
    }

    @Test
    void should_close_the_cursor() {
        cursorReturns(1);

        ApiReferenceBackfill.apply(collection);

        verify(cursor).close();
    }
}
