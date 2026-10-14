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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Sets {@code referenceType = "API"} and {@code referenceId = <value of the api field>} on the documents (plans, subscriptions)
 * that do not have a {@code referenceType} yet.
 *
 * <p>The value of {@code referenceId} comes from another field of the same document, which is naturally written as a single
 * server-side {@code updateMany} with an aggregation pipeline ({@code [{ $set: { referenceId: "$api" } }]}). That form is
 * <b>not supported by Amazon DocumentDB</b> (rejected with {@code Pipeline based document update}, code 303, or
 * {@code Wrong type for parameter u}, code 14, even on an empty collection), which made the Management API fail at startup.
 * To stay compatible with both MongoDB and DocumentDB, the value is read client side and written with plain
 * {@code $set} operators in batched {@code bulkWrite} calls.
 *
 * <p>The operation is idempotent: only documents without a {@code referenceType} are selected, and each one is updated once.
 */
@CustomLog
public final class ApiReferenceBackfill {

    static final int BATCH_SIZE = 1000;

    static final String REFERENCE_TYPE_API = "API";

    private ApiReferenceBackfill() {}

    /**
     * @return {@code true} if every write has been acknowledged, {@code false} otherwise. Errors are not swallowed: they are
     * propagated to the caller so that a failed migration is never recorded as applied.
     */
    public static boolean apply(MongoCollection<Document> collection) {
        Bson noReferenceType = Filters.or(Filters.exists("referenceType", false), Filters.eq("referenceType", null));
        Bson filter = Filters.and(Filters.exists("api", true), Filters.ne("api", null), Filters.ne("api", ""), noReferenceType);

        var options = new BulkWriteOptions().ordered(false);
        var batch = new ArrayList<WriteModel<Document>>(BATCH_SIZE);
        long modified = 0;
        boolean acknowledged = true;

        try (
            MongoCursor<Document> cursor = collection
                .find(filter)
                .projection(new Document("_id", 1).append("api", 1))
                .batchSize(BATCH_SIZE)
                .iterator()
        ) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                batch.add(
                    new UpdateOneModel<>(
                        // Re-check "no reference yet" on the write, as the former updateMany did atomically for each document: a
                        // document that got a reference between the find and the bulkWrite (another node) is not overwritten.
                        Filters.and(Filters.eq("_id", doc.get("_id")), noReferenceType),
                        Updates.combine(Updates.set("referenceType", REFERENCE_TYPE_API), Updates.set("referenceId", doc.get("api")))
                    )
                );
                if (batch.size() >= BATCH_SIZE) {
                    long written = flush(collection, batch, options);
                    acknowledged &= written >= 0;
                    modified += Math.max(written, 0);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            long written = flush(collection, batch, options);
            acknowledged &= written >= 0;
            modified += Math.max(written, 0);
        }

        log.debug("Upgraded {} document(s) of {} with reference type and reference id", modified, collection.getNamespace());
        return acknowledged;
    }

    /**
     * @return the number of modified documents, or -1 if the write has not been acknowledged.
     */
    private static long flush(MongoCollection<Document> collection, List<WriteModel<Document>> batch, BulkWriteOptions options) {
        var result = collection.bulkWrite(batch, options);
        return result.wasAcknowledged() ? result.getModifiedCount() : -1;
    }
}
