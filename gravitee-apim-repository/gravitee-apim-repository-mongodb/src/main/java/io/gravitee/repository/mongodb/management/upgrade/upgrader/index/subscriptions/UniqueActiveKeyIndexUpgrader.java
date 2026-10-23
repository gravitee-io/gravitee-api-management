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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.subscriptions;

import static io.gravitee.repository.mongodb.management.MongoSubscriptionRepository.UNIQUE_ACTIVE_KEY_FIELD;
import static io.gravitee.repository.mongodb.management.MongoSubscriptionRepository.UNIQUE_ACTIVE_KEY_INDEX;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.IndexMongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.subscription.SubscriptionUniqueActiveKeyRepair;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.subscription.SubscriptionUniqueActiveKeyUpgrader;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.BsonInt32;
import org.bson.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Enforces, at database level, that an application has at most one active subscription (neither rejected nor closed)
 * to a plan, or to a channel of a PUSH plan: a unique index on the unique active key, which only active subscriptions
 * carry. It is the only protection against identical subscription requests processed at the same time, by one or
 * several Management API nodes.
 * <p>
 * Nodes of the previous versions do not maintain the key, so the key is repaired first. It runs at startup, before the
 * node is ready:
 * <ul>
 *     <li>it uses the client with long timeouts of the index upgraders, the default client gives up after one second;</li>
 *     <li>it never reports a failure, which would stop the node: without the index, the node keeps the behaviour of
 *     the previous versions;</li>
 *     <li>subscriptions already duplicated cannot be arbitrated here, both may be in use: they are reported, and the
 *     index is left out until an operator closes the extra ones;</li>
 *     <li>until the index exists, its {@link #identifier()} changes at each startup, so that the upgrader runs again
 *     instead of being recorded as done;</li>
 *     <li>running it on several nodes at the same time is harmless: the repair gives the same result, and MongoDB
 *     builds an index once.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@CustomLog
@Component("SubscriptionsUniqueActiveKeyIndexUpgrader")
public class UniqueActiveKeyIndexUpgrader extends IndexMongoUpgrader {

    private static final String COLLECTION = "subscriptions";

    private static final long DUPLICATES_MAX_TIME_MS = TimeUnit.MINUTES.toMillis(30);

    /**
     * Identifies this startup, while the index is missing.
     */
    private final String attempt = UUID.randomUUID().toString();

    private volatile String identifier;

    Index buildIndex() {
        return Index.builder()
            .collection(COLLECTION)
            .name(UNIQUE_ACTIVE_KEY_INDEX)
            .key(UNIQUE_ACTIVE_KEY_FIELD, new BsonInt32(1))
            .unique(true)
            // A null key would be indexed too: inactive subscriptions do not have the field at all. A Document, which
            // Spring Data expects for the index options.
            .partialFilterExpression(
                new Document(UNIQUE_ACTIVE_KEY_FIELD, new Document("$exists", true))
            )
            .build();
    }

    /**
     * The upgrader runner records the identifier of every upgrader that succeeded and skips it at the next startups.
     * The identifier is the same at every startup only once the index exists.
     */
    @Override
    public String identifier() {
        if (identifier == null) {
            if (indexExists()) {
                identifier = super.identifier();
            } else {
                // Called at every startup by the upgrader runner, before it decides to run the upgrader.
                log.info(
                    "Index {} is missing on subscriptions, creating it is attempted at this startup",
                    UNIQUE_ACTIVE_KEY_INDEX
                );
                identifier = super.identifier() + "#attempt-" + attempt;
            }
        }
        return identifier;
    }

    @Override
    public boolean upgrade() {
        String collection = buildCollectionName(COLLECTION);
        try {
            if (indexExists()) {
                log.info(
                    "Index {} exists on {}: an application cannot have several active subscriptions to the same plan",
                    UNIQUE_ACTIVE_KEY_INDEX,
                    collection
                );
                return true;
            }
            new SubscriptionUniqueActiveKeyRepair(subscriptions()).repair();
            log.info(
                "Starting creation of index {} on {}",
                UNIQUE_ACTIVE_KEY_INDEX,
                collection
            );
            template
                .indexOps(collection)
                .ensureIndex(buildIndex().toIndexDefinition())
                .block();
            log.info(
                "Index {} has been created successfully on {}: an application cannot have several active subscriptions to the same plan anymore",
                UNIQUE_ACTIVE_KEY_INDEX,
                collection
            );
        } catch (Exception e) {
            if (isDuplicateKey(e)) {
                reportDuplicates(collection);
            } else {
                log.error(
                    "Unexpected error while creating index {} on {}, it will be created at the next startup. Until then, concurrent subscription requests may create duplicated active subscriptions",
                    UNIQUE_ACTIVE_KEY_INDEX,
                    collection,
                    e
                );
            }
        }
        return true;
    }

    @Override
    public int getOrder() {
        return (
            SubscriptionUniqueActiveKeyUpgrader.SUBSCRIPTION_UNIQUE_ACTIVE_KEY_UPGRADER_ORDER +
            1
        );
    }

    private boolean indexExists() {
        try {
            return Boolean.TRUE.equals(
                Flux.from(subscriptions().listIndexes())
                    .any(index -> UNIQUE_ACTIVE_KEY_INDEX.equals(index.getString("name")))
                    .block()
            );
        } catch (Exception e) {
            log.warn(
                "Unable to check whether index {} exists, assuming it does not: {}",
                UNIQUE_ACTIVE_KEY_INDEX,
                e.getMessage()
            );
            return false;
        }
    }

    private void reportDuplicates(String collection) {
        try {
            List<Document> duplicates = Flux.from(
                subscriptions()
                    .aggregate(
                        List.of(
                            Aggregates.match(Filters.exists(UNIQUE_ACTIVE_KEY_FIELD)),
                            Aggregates.group(
                                "$" + UNIQUE_ACTIVE_KEY_FIELD,
                                Accumulators.first("application", "$application"),
                                Accumulators.first("plan", "$plan"),
                                Accumulators.push("subscriptions", "$_id"),
                                Accumulators.sum("count", 1)
                            ),
                            Aggregates.match(Filters.gt("count", 1))
                        )
                    )
                    .allowDiskUse(true)
                    // Walks the _id index: no collection scan, even where they are forbidden.
                    .hint(new Document("_id", 1))
                    .maxTime(DUPLICATES_MAX_TIME_MS, TimeUnit.MILLISECONDS)
            )
                .collectList()
                .block();
            log.warn(
                "Index {} has not been created on {}: {} application(s) have several active subscriptions to the same plan. It will be created at the next startup once the extra subscriptions are closed.",
                UNIQUE_ACTIVE_KEY_INDEX,
                collection,
                duplicates == null ? 0 : duplicates.size()
            );
            if (duplicates != null) {
                duplicates.forEach(duplicate ->
                    log.warn(
                        "Application [{}] has {} active subscriptions to plan [{}]: {}",
                        duplicate.getString("application"),
                        duplicate.get("count"),
                        duplicate.getString("plan"),
                        duplicate.get("subscriptions")
                    )
                );
            }
        } catch (Exception e) {
            log.warn(
                "Index {} has not been created on {}: some applications have several active subscriptions to the same plan, and they could not be listed. It will be created at the next startup once the extra subscriptions are closed.",
                UNIQUE_ACTIVE_KEY_INDEX,
                collection,
                e
            );
        }
    }

    private static boolean isDuplicateKey(Throwable throwable) {
        MongoException mongoError = ExceptionUtils.throwableOfType(
            throwable,
            MongoException.class
        );
        return (
            mongoError != null &&
            ErrorCategory.fromErrorCode(mongoError.getCode()) ==
            ErrorCategory.DUPLICATE_KEY
        );
    }

    private MongoCollection<Document> subscriptions() {
        return getCollection(COLLECTION).block();
    }
}
