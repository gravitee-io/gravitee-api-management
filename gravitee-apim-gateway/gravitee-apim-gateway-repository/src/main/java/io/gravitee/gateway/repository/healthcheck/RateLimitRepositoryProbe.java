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
package io.gravitee.gateway.repository.healthcheck;

import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RateLimitRepositoryProbe implements Probe {

    private static final String NONE_RATE_LIMIT_TYPE = "none";

    @Autowired
    private Node node;

    @Autowired
    private RateLimitRepository<RateLimit> rateLimitRepository;

    @Value("${ratelimit.type:}")
    private String rateLimitType;

    @Override
    public String id() {
        return "ratelimit-repository";
    }

    @Override
    public CompletableFuture<Result> check() {
        return CompletableFuture.supplyAsync(() -> {
            CompletableFuture<Result> future = new CompletableFuture<>();

            try {
                final String rlIdentifier = "hc-" + node.id();

                // Search for a rate-limit value to check repository connection
                var rateLimitSingle = rateLimitRepository.incrementAndGet(
                    rlIdentifier,
                    1L,
                    () -> {
                        RateLimit rateLimit = new RateLimit(rlIdentifier);
                        rateLimit.setSubscription(rlIdentifier);
                        return rateLimit;
                    }
                );

                if (NONE_RATE_LIMIT_TYPE.equalsIgnoreCase(rateLimitType) && rateLimitSingle == null) {
                    future.complete(Result.healthy("RateLimit type is none"));
                } else {
                    rateLimitSingle.subscribe(
                        new SingleObserver<>() {
                            @Override
                            public void onSubscribe(Disposable d) {}

                            @Override
                            public void onSuccess(RateLimit rateLimit) {
                                future.complete(Result.healthy());
                            }

                            @Override
                            public void onError(Throwable t) {
                                future.complete(Result.unhealthy(t));
                            }
                        }
                    );
                }

                try {
                    return future.get();
                } catch (Exception ex) {
                    return Result.unhealthy(ex);
                }
            } catch (Throwable t) {
                return Result.unhealthy(t);
            }
        });
    }
}
