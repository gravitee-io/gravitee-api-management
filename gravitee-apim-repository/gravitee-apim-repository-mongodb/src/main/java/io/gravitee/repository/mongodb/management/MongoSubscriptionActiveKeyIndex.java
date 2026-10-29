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

import com.mongodb.MongoCommandException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import lombok.CustomLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

/**
 * Whether the unique index on the hash of the unique active key of the subscriptions can protect the writes: the
 * repository writes the hash only then. A hash written while the index is missing, or still being built, could
 * duplicate the hash of another subscription, and the index could not be built anymore without repairing data.
 * <p>
 * An index being built is already listed, but does not refuse duplicates yet: the index is ready only once creating it
 * again with the same definition is a no-op. Checked by the writing thread on the first write, then at most once a
 * minute, never at startup: the gateways create the repository too, and only read subscriptions.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoSubscriptionActiveKeyIndex {

    public static final String FIELD = "activeKeyHash";
    public static final String NAME = "akh1_unique";

    static final Duration CHECK_INTERVAL = Duration.ofMinutes(1);
    private static final Duration WARNING_INTERVAL = Duration.ofHours(1);
    private static final int MAX_TIME_MS = 1000;
    private static final int MAX_TIME_EXPIRED = 50;
    private static final int UNAUTHORIZED = 13;
    private static final int INDEX_OPTIONS_CONFLICT = 85;
    private static final int INDEX_KEY_SPECS_CONFLICT = 86;

    private final MongoOperations mongoOperations;
    private final String collection;
    private final LongSupplier clock;
    private final AtomicBoolean checking = new AtomicBoolean();
    private volatile boolean checked;
    private volatile boolean ready;
    private volatile long nextCheck;
    private volatile long nextWarning;

    /**
     * Lazy: the template is not created early, before its mapping context can resolve the collection names.
     */
    @Autowired
    public MongoSubscriptionActiveKeyIndex(
        @Lazy @Qualifier("managementMongoTemplate") MongoOperations mongoOperations,
        @Value("${management.mongodb.prefix:}") String prefix
    ) {
        this(mongoOperations, prefix, System::currentTimeMillis);
    }

    MongoSubscriptionActiveKeyIndex(MongoOperations mongoOperations, String prefix, LongSupplier clock) {
        this.mongoOperations = mongoOperations;
        this.collection = prefix + "subscriptions";
        this.clock = clock;
    }

    /**
     * Definition of the index, as given to {@code createIndexes}. Only active subscriptions carry the field.
     */
    public static Document definition() {
        return new Document("key", new Document(FIELD, 1))
            .append("name", NAME)
            .append("unique", true)
            .append("partialFilterExpression", new Document(FIELD, new Document("$exists", true)));
    }

    /**
     * @return whether the hash can be written. Never fails: an index that cannot be checked is not ready.
     */
    public boolean isReady() {
        if (!checked) {
            // Concurrent first writes all wait for the first check: none of them may write a hash before it.
            synchronized (this) {
                if (!checked) {
                    refresh();
                    checked = true;
                }
            }
        } else if (clock.getAsLong() >= nextCheck && checking.compareAndSet(false, true)) {
            // The other threads keep the previous answer meanwhile.
            try {
                refresh();
            } finally {
                checking.set(false);
            }
        }
        return ready;
    }

    private void refresh() {
        String unreadiness = unreadiness();
        long now = clock.getAsLong();
        nextCheck = now + CHECK_INTERVAL.toMillis();
        if (unreadiness == null) {
            if (!ready) {
                log.info(
                    "Index {} on {} is ready: an application cannot have several active subscriptions to the same plan anymore",
                    NAME,
                    collection
                );
            }
            ready = true;
            return;
        }
        if (ready || now >= nextWarning) {
            log.warn(
                "Index {} on {} is not ready, {}: concurrent identical subscription requests may create several active subscriptions of an application to a plan",
                NAME,
                collection,
                unreadiness
            );
            nextWarning = now + WARNING_INTERVAL.toMillis();
        }
        ready = false;
    }

    /**
     * @return why the index cannot protect the writes, with what to do about it, or {@code null} when it is ready.
     */
    private String unreadiness() {
        try {
            boolean listed = mongoOperations
                .getCollection(collection)
                .listIndexes()
                .into(new ArrayList<>())
                .stream()
                .anyMatch(index -> NAME.equals(index.getString("name")));
            if (!listed) {
                return "it does not exist. It is created at the Management API startup: if it failed, run scripts/create-index.js, or remove the record of the SubscriptionsActiveKeyHashIndexUpgrader from the upgrades collection and restart";
            }
            Document result = mongoOperations.executeCommand(
                new Document("createIndexes", collection).append("indexes", List.of(definition())).append("maxTimeMS", MAX_TIME_MS)
            );
            if (result.get("numIndexesAfter", Number.class) != null && result.get("numIndexesBefore", Number.class) != null) {
                if (result.get("numIndexesAfter", Number.class).intValue() > result.get("numIndexesBefore", Number.class).intValue()) {
                    log.warn("Index {} on {} was dropped meanwhile: it has been created again", NAME, collection);
                }
            }
            return null;
        } catch (Exception e) {
            MongoCommandException mongoError = ExceptionUtils.throwableOfType(e, MongoCommandException.class);
            int code = mongoError == null ? 0 : mongoError.getErrorCode();
            return switch (code) {
                case MAX_TIME_EXPIRED -> "it is still being built";
                case UNAUTHORIZED -> "its definition cannot be checked: grant the createIndex action on the subscriptions collection to the Management API database user";
                case INDEX_OPTIONS_CONFLICT, INDEX_KEY_SPECS_CONFLICT -> "an index with another definition is in the way (" +
                code +
                "): drop it and restart the Management API";
                default -> "it cannot be checked (" + e.getMessage() + ")";
            };
        }
    }
}
