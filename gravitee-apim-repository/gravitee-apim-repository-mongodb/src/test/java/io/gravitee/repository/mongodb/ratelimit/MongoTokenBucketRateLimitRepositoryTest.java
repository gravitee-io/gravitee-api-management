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
package io.gravitee.repository.mongodb.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import io.gravitee.repository.ratelimit.AbstractTokenBucketRateLimitRepositoryContractTest;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs the shared {@link AbstractTokenBucketRateLimitRepositoryContractTest} against MongoDB. A
 * single container is started for the class; the collection is dropped before each test so every
 * test sees an empty store.
 */
class MongoTokenBucketRateLimitRepositoryTest extends AbstractTokenBucketRateLimitRepositoryContractTest {

    private static final String DATABASE = "gravitee-test";

    private static MongoDBContainer mongoContainer;
    private static MongoClient mongoClient;

    @BeforeAll
    static void startMongo() {
        mongoContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));
        mongoContainer.start();
        mongoClient = MongoClients.create(mongoContainer.getReplicaSetUrl());
    }

    @AfterAll
    static void stopMongo() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (mongoContainer != null) {
            mongoContainer.stop();
        }
    }

    private ReactiveMongoTemplate template;

    @Override
    protected TokenBucketRateLimitRepository<TokenBucket> createRepository() {
        template = new ReactiveMongoTemplate(mongoClient, DATABASE);
        template.dropCollection("tokenbucket").block();
        return new MongoTokenBucketRateLimitRepository(template, "");
    }

    @Test
    void sets_expire_at_to_the_full_refill_window() {
        long before = System.currentTimeMillis();
        // capacity 100 at 2 tokens per 1000ms => the bucket would refill fully in 50s, so the entry is
        // given an expire_at ~50s out: long enough that it is never evicted before it would be full.
        repository.refillAndTryConsume("ttl", 1, 2, 1_000L, 100, before, () -> new TokenBucket("ttl")).blockingGet();

        Document stored = template.findById("ttl", Document.class, "tokenbucket").block();
        assertThat(stored).isNotNull();
        assertThat(stored.getDate("expire_at")).isBetween(new Date(before + 49_000L), new Date(before + 60_000L));
    }
}
