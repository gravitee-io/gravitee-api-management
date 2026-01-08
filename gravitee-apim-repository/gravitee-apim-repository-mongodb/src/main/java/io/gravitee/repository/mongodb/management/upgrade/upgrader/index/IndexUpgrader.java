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

import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.IndexMongoUpgrader;
import java.time.Duration;
import lombok.CustomLog;
import org.bson.BsonInt32;
import org.bson.BsonValue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public abstract class IndexUpgrader extends IndexMongoUpgrader {

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

    @Override
    public int getOrder() {
        return 0;
    }
}
