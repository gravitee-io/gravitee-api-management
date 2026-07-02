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
package io.gravitee.repository.hazelcast.ratelimit;

import static io.gravitee.repository.hazelcast.ratelimit.RateLimitRepositoryConfiguration.TOKEN_BUCKET_MAP;
import static org.assertj.core.api.Assertions.assertThat;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.EntryView;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.repository.ratelimit.AbstractTokenBucketRateLimitRepositoryContractTest;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Runs the shared {@link AbstractTokenBucketRateLimitRepositoryContractTest} against the
 * Hazelcast backend. A fresh embedded instance per test gives each test an isolated, empty store.
 */
class HazelcastTokenBucketRateLimitRepositoryTest extends AbstractTokenBucketRateLimitRepositoryContractTest {

    private HazelcastInstance hazelcast;

    @Override
    protected TokenBucketRateLimitRepository<TokenBucket> createRepository() {
        try {
            Config config = new FileSystemXmlConfig("src/test/resources/cluster.xml");
            config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
            config.setInstanceName("test-tokenbucket-hz-" + System.nanoTime());
            hazelcast = Hazelcast.newHazelcastInstance(config);
            return new HazelcastTokenBucketRateLimitRepository(hazelcast.getMap(TOKEN_BUCKET_MAP));
        } catch (Exception e) {
            throw new RuntimeException("Failed to start embedded Hazelcast", e);
        }
    }

    @AfterEach
    void shutdownHazelcast() {
        if (hazelcast != null) {
            hazelcast.shutdown();
        }
    }

    @Test
    void sets_entry_ttl_to_the_full_refill_window() {
        // capacity 100 at 2 tokens per 1000ms => the bucket would refill fully in 50s, so the entry is
        // given a 50s TTL: long enough that it is never evicted before it would be full, after which
        // evicting and recreating it full is equivalent to keeping it.
        repository.refillAndTryConsume("ttl", 1, 2, 1_000L, 100, System.currentTimeMillis(), () -> new TokenBucket("ttl")).blockingGet();

        EntryView<String, TokenBucket> view = hazelcast.<String, TokenBucket>getMap(TOKEN_BUCKET_MAP).getEntryView("ttl");
        assertThat(view.getTtl()).isEqualTo(50_000L);
    }
}
