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
package io.gravitee.repository.noop.ratelimit;

import static org.junit.jupiter.api.Assertions.assertNull;

import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
public class NoOpTokenBucketRateLimitRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private TokenBucketRateLimitRepository<TokenBucket> cut;

    @Test
    public void refillAndTryConsume() {
        Single<TokenBucketConsumeResult> result = cut.refillAndTryConsume("test_key", 1L, 10L, 1_000L, 100L, 1_000L, () ->
            new TokenBucket("test_key")
        );

        assertNull(result);
    }
}
