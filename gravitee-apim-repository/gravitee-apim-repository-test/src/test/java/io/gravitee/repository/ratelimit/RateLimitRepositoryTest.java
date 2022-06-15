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
package io.gravitee.repository.ratelimit;

import static org.junit.Assert.assertEquals;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.TestObserver;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
public class RateLimitRepositoryTest extends AbstractRepositoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitRepositoryTest.class);

    private static final long OPERATION_TIMEOUT_SECONDS = 5L;

    private static final Map<String, RateLimit> RATE_LIMITS = new HashMap<>();

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    RateLimitRepository<RateLimit> rateLimitRepository;

    @Override
    protected String getTestCasesPath() {
        return "/data/ratelimit-tests/";
    }

    @Override
    protected String getModelPackage() {
        return "io.gravitee.repository.ratelimit.model.";
    }

    @Override
    protected void createModel(Object object) {
        RateLimit rateLimit = (RateLimit) object;

        RateLimit updatedRateLimit = rateLimitRepository
            .incrementAndGet(rateLimit.getKey(), rateLimit.getCounter(), () -> initialize(rateLimit))
            .blockingGet();

        RATE_LIMITS.put(updatedRateLimit.getKey(), updatedRateLimit);

        LOG.debug("Created {}", updatedRateLimit);
    }

    @Test
    public void shouldIncrementAndGet_byOne() {
        final RateLimit rateLimit = RATE_LIMITS.get("rl-1");
        final TestObserver<RateLimit> observer = incrementAndObserve(rateLimit, 1L);
        final long expectedCounter = 1L;

        observer.awaitTerminalEvent(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        observer
            .assertValue(shouldNotFail(rl -> assertEquals(expectedCounter, rl.getCounter())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getSubscription(), rl.getSubscription())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getResetTime(), rl.getResetTime())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getLimit(), rl.getLimit())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getKey(), rl.getKey())));
    }

    @Test
    public void shouldIncrementAndGet_fromSuppliedCounterByTwo() {
        final RateLimit rateLimit = RATE_LIMITS.get("rl-2");
        final TestObserver<RateLimit> observer = incrementAndObserve(rateLimit, 2L);
        final long expectedCounter = 42L;

        observer.awaitTerminalEvent(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        observer
            .assertValue(shouldNotFail(rl -> assertEquals(expectedCounter, rl.getCounter())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getSubscription(), rl.getSubscription())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getResetTime(), rl.getResetTime())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getLimit(), rl.getLimit())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getKey(), rl.getKey())));
    }

    @Test
    public void shouldIncrementAndGet_withUnknownKey() {
        final RateLimit rateLimit = of("rl-3", 0, 100000, 5000, "rl-3-subscription");
        final TestObserver<RateLimit> observer = incrementAndObserve(rateLimit, 10L);
        final long expectedCounter = 10L;

        observer.awaitTerminalEvent(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        observer
            .assertValue(shouldNotFail(rl -> assertEquals(expectedCounter, rl.getCounter())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getSubscription(), rl.getSubscription())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getResetTime(), rl.getResetTime())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getLimit(), rl.getLimit())))
            .assertValue(shouldNotFail(rl -> assertEquals(rateLimit.getKey(), rl.getKey())));
    }

    private TestObserver<RateLimit> incrementAndObserve(RateLimit rateLimit, long weight) {
        return rateLimitRepository.incrementAndGet(rateLimit.getKey(), weight, () -> rateLimit).test();
    }

    /*
     * Used to get better error messages with testObserver
     * using a consumer that can throw an assertion error
     * if the assertion fails before returning true
     */
    private static Predicate<RateLimit> shouldNotFail(Consumer<RateLimit> consumer) {
        return rl -> {
            consumer.accept(rl);
            return true;
        };
    }

    /**
     *
     * @param key the rateLimit key
     * @param counter the counter value of the rateLimit
     * @param expireIn when the rateLimit should expire in milliseconds
     * @param limit the limit of the rateLimit
     * @param subscription the subscription of the rateLimit
     * @return a new rateLimit
     */
    private static RateLimit of(String key, long counter, long expireIn, long limit, String subscription) {
        final RateLimit rateLimit = new RateLimit(key);
        final Instant resetTime = Instant.now().plus(Duration.ofMillis(expireIn));
        rateLimit.setSubscription(subscription);
        rateLimit.setResetTime(resetTime.toEpochMilli());
        rateLimit.setLimit(limit);
        rateLimit.setCounter(counter);
        return rateLimit;
    }

    /**
     *
     * @param rateLimit a rateLimit to pass as a supplier when initializing our data
     * @return a copy of this rateLimit with a 0 counter
     */
    private static RateLimit initialize(RateLimit rateLimit) {
        final long counter = 0;
        return of(rateLimit.getKey(), counter, rateLimit.getResetTime(), rateLimit.getLimit(), rateLimit.getSubscription());
    }
}
