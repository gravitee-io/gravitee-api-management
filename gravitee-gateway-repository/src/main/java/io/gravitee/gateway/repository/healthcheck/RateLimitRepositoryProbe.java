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

import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RateLimitRepositoryProbe implements Probe {

    @Autowired
    private Node node;

    @Autowired
    private RateLimitRepository<RateLimit> rateLimitRepository;

    @Override
    public String id() {
        return "ratelimit-repository";
    }

    @Override
    public CompletableFuture<Result> check() {
        return CompletableFuture.supplyAsync(
            new Supplier<Result>() {
                @Override
                public Result get() {
                    CompletableFuture<Result> future = new CompletableFuture<>();

                    try {
                        final String rlIdentifier = "hc-" + node.id();

                        // Search for a rate-limit value to check repository connection
                        rateLimitRepository
                            .incrementAndGet(
                                rlIdentifier,
                                1L,
                                new Supplier<RateLimit>() {
                                    @Override
                                    public RateLimit get() {
                                        RateLimit rateLimit = new RateLimit(rlIdentifier);
                                        rateLimit.setSubscription(rlIdentifier);
                                        return rateLimit;
                                    }
                                }
                            )
                            .subscribe(
                                new SingleObserver<RateLimit>() {
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

                        try {
                            return future.get();
                        } catch (Exception ex) {
                            return Result.unhealthy(ex);
                        }
                    } catch (Throwable t) {
                        return Result.unhealthy(t);
                    }
                }
            }
        );
    }
}
