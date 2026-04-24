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

import io.gravitee.node.api.cluster.DistributedMap;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HazelcastRateLimitRepository implements RateLimitRepository<RateLimit> {

    private final DistributedMap<String, RateLimit> counters;

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        return Single.fromCallable(() -> {
            counters.lock(key);
            try {
                final long now = System.currentTimeMillis();
                RateLimit rateLimit = counters.get(key);
                if (rateLimit == null || rateLimit.getResetTime() < now) {
                    rateLimit = supplier.get();
                }
                rateLimit.setCounter(rateLimit.getCounter() + weight);
                final long ttlMs = Math.max(1L, rateLimit.getResetTime() - now);
                counters.put(rateLimit.getKey(), rateLimit, ttlMs);
                return rateLimit;
            } finally {
                counters.unlock(key);
            }
        }).subscribeOn(Schedulers.computation());
    }
}
