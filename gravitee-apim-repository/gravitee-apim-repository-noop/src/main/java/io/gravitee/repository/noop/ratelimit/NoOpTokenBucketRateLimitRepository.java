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

import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.reactivex.rxjava3.core.Single;
import java.util.function.Supplier;

/**
 * No-op {@link TokenBucketRateLimitRepository}: rate limiting is disabled. Mirrors
 * {@link NoOpRateLimitRepository} by returning {@code null}, which the policy treats as
 * "no enforcement" (pass-through).
 *
 * @author GraviteeSource Team
 */
public class NoOpTokenBucketRateLimitRepository implements TokenBucketRateLimitRepository<TokenBucket> {

    @Override
    public Single<TokenBucketConsumeResult> refillAndTryConsume(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        Supplier<TokenBucket> supplier
    ) {
        return null;
    }
}
