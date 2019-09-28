/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.repository.healthcheck;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RateLimitRepositoryProbe implements Probe {

    private static final String RATE_LIMIT_UNKNOWN_IDENTIFIER = "unknown-healthcheck";

    @Autowired
    private RateLimitRepository rateLimitRepository;

    @Override
    public String id() {
        return "ratelimit-repository";
    }

    @Override
    public CompletableFuture<Result> check() {
        try {
            // Search for a rate-limit value to check repository connection
            return CompletableFuture.supplyAsync(() -> {
                try {
                    rateLimitRepository.incrementAndGet(RATE_LIMIT_UNKNOWN_IDENTIFIER, 1L, new Supplier<RateLimit>() {
                        @Override
                        public RateLimit get() {
                            RateLimit rateLimit = new RateLimit(RATE_LIMIT_UNKNOWN_IDENTIFIER);
                            rateLimit.setSubscription(RATE_LIMIT_UNKNOWN_IDENTIFIER);
                            return rateLimit;
                        }
                    });
                    return Result.healthy();
                } catch (Exception ex) {
                    return Result.unhealthy(ex);
                }
            });
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(Result.unhealthy(ex));
        }
    }
}
