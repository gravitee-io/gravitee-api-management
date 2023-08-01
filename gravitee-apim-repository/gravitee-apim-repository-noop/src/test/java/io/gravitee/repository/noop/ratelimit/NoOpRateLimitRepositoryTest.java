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

import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.rxjava3.core.Single;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.function.SingletonSupplier;

import static org.junit.Assert.*;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpRateLimitRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private RateLimitRepository<RateLimit> cut;

    @Test
    public void incrementAndGet() {
        Single<RateLimit> result = cut.incrementAndGet("test_key", 1L, () -> new RateLimit("test_key"));

        assertNull(result);
    }
}
