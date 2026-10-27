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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.subscription;

import static io.gravitee.repository.mongodb.management.MongoSubscriptionRepository.UNIQUE_ACTIVE_KEY_FIELD;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import org.bson.Document;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Recomputes the unique active key of every subscription from the fields it derives from, the only source of truth:
 * nodes of the previous versions write subscriptions without maintaining the key (a full document replace drops it).
 * <p>
 * Subscriptions are read in batches, in {@code _id} order, and a key is only written if the subscription still holds
 * the values it has been computed from. A subscription changed in the meantime is skipped: a node of this version wrote
 * its up-to-date key along with the change, while a node of a previous version dropped it, until the next repair.
 * Running it again, or on several nodes at the same time, gives the same result.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class SubscriptionUniqueActiveKeyRepair {

    static final int BATCH_SIZE = 1000;

    private static final long BATCH_MAX_TIME_MS = TimeUnit.MINUTES.toMillis(1);

    /**
     * Fields of a subscription document the key derives from.
     */
    private static final List<String> KEY_SOURCES = List.of(
        "status",
        "referenceType",
        "referenceId",
        "api",
        "application",
        "plan",
        "type",
        "configuration"
    );

    private final MongoCollection<Document> subscriptions;

    public SubscriptionUniqueActiveKeyRepair(MongoCollection<Document> subscriptions) {
        this.subscriptions = subscriptions;
    }

    /**
     * @return how many subscriptions have been read, how many keys have been fixed, and how many could not be because
     * the key is already used by another active subscription.
     */
    public Result repair() {
        long scanned = 0;
        long fixed = 0;
        long conflicts = 0;
        Object lastId = null;
        List<Document> batch;
        do {
            batch = readBatch(lastId);
            if (batch.isEmpty()) {
                break;
            }
            BatchResult result = repairBatch(batch);
            fixed += result.fixed();
            conflicts += result.conflicts();
            scanned += batch.size();
            lastId = batch.getLast().get("_id");
        } while (batch.size() == BATCH_SIZE);

        log.info("Unique active key of subscriptions repaired: {} scanned, {} fixed, {} in conflict", scanned, fixed, conflicts);
        return new Result(scanned, fixed, conflicts);
    }

    List<Document> readBatch(Object afterId) {
        List<String> fields = new ArrayList<>(KEY_SOURCES);
        fields.add(UNIQUE_ACTIVE_KEY_FIELD);
        Bson filter = afterId == null ? new Document() : Filters.gt("_id", afterId);
        return Flux.from(
            subscriptions
                .find(filter)
                .projection(Projections.include(fields))
                .sort(Sorts.ascending("_id"))
                // Walks the _id index: no collection scan, even where they are forbidden.
                .hint(new Document("_id", 1))
                .limit(BATCH_SIZE)
                .maxTime(BATCH_MAX_TIME_MS, TimeUnit.MILLISECONDS)
        )
            .collectList()
            .block();
    }

    BatchResult repairBatch(List<Document> documents) {
        // A write error gives the position of the update, which only the repaired subscriptions have.
        List<Document> repaired = new ArrayList<>();
        List<WriteModel<Document>> updates = new ArrayList<>();
        for (Document document : documents) {
            WriteModel<Document> update = toRepair(document);
            if (update != null) {
                repaired.add(document);
                updates.add(update);
            }
        }
        if (updates.isEmpty()) {
            return new BatchResult(0, 0);
        }
        try {
            var result = Mono.from(subscriptions.bulkWrite(updates, new BulkWriteOptions().ordered(false))).block();
            return new BatchResult(result == null ? 0 : result.getModifiedCount(), 0);
        } catch (MongoBulkWriteException e) {
            // Once the unique index exists, a key shared with another active subscription cannot be written.
            e
                .getWriteErrors()
                .stream()
                .map(BulkWriteError::getIndex)
                .forEach(index ->
                    log.warn(
                        "Unique active key of subscription [{}] not repaired: another active subscription has the same key",
                        repaired.get(index).get("_id")
                    )
                );
            return new BatchResult(e.getWriteResult().getModifiedCount(), e.getWriteErrors().size());
        }
    }

    private static WriteModel<Document> toRepair(Document document) {
        String expected;
        try {
            expected = SubscriptionUniqueActiveKey.of(toSubscription(document));
        } catch (RuntimeException e) {
            // An unknown status, type or reference type: the repository cannot read this subscription either.
            log.warn("Unique active key of subscription [{}] not repaired: {}", document.get("_id"), e.getMessage());
            return null;
        }
        boolean present = document.containsKey(UNIQUE_ACTIVE_KEY_FIELD);
        if (expected == null ? !present : expected.equals(document.get(UNIQUE_ACTIVE_KEY_FIELD))) {
            return null;
        }

        List<Bson> unchanged = new ArrayList<>();
        unchanged.add(Filters.eq("_id", document.get("_id")));
        // Filters.eq(field, null) also matches a missing field.
        KEY_SOURCES.forEach(field -> unchanged.add(Filters.eq(field, document.get(field))));

        Bson update = expected == null ? Updates.unset(UNIQUE_ACTIVE_KEY_FIELD) : Updates.set(UNIQUE_ACTIVE_KEY_FIELD, expected);
        return new UpdateOneModel<>(Filters.and(unchanged), update);
    }

    private static Subscription toSubscription(Document document) {
        Subscription subscription = new Subscription();
        subscription.setId(String.valueOf(document.get("_id")));
        subscription.setStatus(valueOf(Subscription.Status.class, document.getString("status")));
        subscription.setReferenceType(valueOf(SubscriptionReferenceType.class, document.getString("referenceType")));
        subscription.setReferenceId(document.getString("referenceId"));
        subscription.setApi(document.getString("api"));
        subscription.setApplication(document.getString("application"));
        subscription.setPlan(document.getString("plan"));
        subscription.setType(valueOf(Subscription.Type.class, document.getString("type")));
        subscription.setConfiguration(document.getString("configuration"));
        return subscription;
    }

    private static <E extends Enum<E>> E valueOf(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    public record Result(long scanned, long fixed, long conflicts) {}

    record BatchResult(long fixed, long conflicts) {}
}
