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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index;

import com.mongodb.MongoCommandException;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.IndexMongoUpgrader;
import java.time.Duration;
import lombok.CustomLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.BsonInt32;
import org.bson.BsonValue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public abstract class IndexUpgrader extends IndexMongoUpgrader {

    /**
     * MongoDB error code returned when an index with the same key pattern already exists with different options
     * (for instance a different name or collation). MongoDB only raises it when the key pattern, the name and the
     * collation do not allow the two indexes to coexist; Amazon DocumentDB raises it for any second index on the same
     * key pattern, whatever the name or collation.
     */
    static final int INDEX_OPTIONS_CONFLICT_ERROR_CODE = 85;

    protected abstract Index buildIndex();

    protected static BsonValue ascending() {
        return new BsonInt32(1);
    }

    protected static BsonValue descending() {
        return new BsonInt32(-1);
    }

    @Override
    public boolean upgrade() {
        Index index = buildIndex();
        String collection = buildCollectionName(index.getCollection());
        String name = index.options().getName();

        Mono<Boolean> create = template
            .indexOps(collection)
            .ensureIndex(index.toIndexDefinition())
            .doOnSubscribe(s ->
                log.info("Starting creation of index {} on {}", name, collection)
            )
            .doOnSuccess(r ->
                log.info("Index {} has been created successfully on {}", name, collection)
            )
            .thenReturn(true)
            .onErrorResume(e -> {
                if (isIndexOptionsConflict(e)) {
                    // An index with the same key pattern already exists (typically on Amazon DocumentDB, which does not
                    // allow several indexes on the same keys even with different collations). The existing index already
                    // serves the same key pattern, so skipping the creation is safe and must not block the startup.
                    log.warn(
                        "Index {} on {} has not been created because an index with the same key pattern already exists (error {}). Skipping it.",
                        name,
                        collection,
                        INDEX_OPTIONS_CONFLICT_ERROR_CODE
                    );
                    return Mono.just(true);
                }
                log.error(
                    "Unexpected error while creating index {} on {}",
                    name,
                    collection,
                    e
                );
                return Mono.just(false);
            })
            .cache();

        Flux.interval(Duration.ofSeconds(10))
            .doOnNext(t ->
                log.info("Index {} on {} is still being created...", name, collection)
            )
            .takeUntilOther(create)
            .subscribe();

        return Boolean.TRUE.equals(create.block());
    }

    /**
     * Spring Data wraps the driver exception (for instance in a DataIntegrityViolationException), so the Mongo error is
     * looked up in the cause chain.
     */
    static boolean isIndexOptionsConflict(Throwable throwable) {
        MongoCommandException mongoError = ExceptionUtils.throwableOfType(
            throwable,
            MongoCommandException.class
        );
        return (
            mongoError != null &&
            mongoError.getErrorCode() == INDEX_OPTIONS_CONFLICT_ERROR_CODE
        );
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
