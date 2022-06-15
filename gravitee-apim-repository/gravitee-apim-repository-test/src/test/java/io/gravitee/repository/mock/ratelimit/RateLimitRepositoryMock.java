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
package io.gravitee.repository.mock.ratelimit;

import static org.mockito.Mockito.*;

import io.gravitee.repository.mock.AbstractRepositoryMock;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.Single;
import java.util.function.Supplier;
import org.mockito.invocation.InvocationOnMock;

/**
 * @author GraviteeSource Team
 */
@SuppressWarnings({ "unchecked", "rawtypes", "ReactiveStreamsUnusedPublisher" })
public class RateLimitRepositoryMock extends AbstractRepositoryMock<RateLimitRepository> {

    public RateLimitRepositoryMock() {
        super(RateLimitRepository.class);
    }

    @Override
    protected void prepare(RateLimitRepository repository) throws Exception {
        doAnswer(caller -> mockCounter(caller, 1)).when(repository).incrementAndGet(eq("rl-1"), anyLong(), any());
        doAnswer(caller -> mockCounter(caller, 42)).when(repository).incrementAndGet(eq("rl-2"), anyLong(), any());
        doAnswer(caller -> mockCounter(caller, 10)).when(repository).incrementAndGet(eq("rl-3"), anyLong(), any());
    }

    private static Single<RateLimit> mockCounter(InvocationOnMock caller, long counter) {
        Supplier<RateLimit> supplier = caller.getArgument(2);
        RateLimit rateLimit = supplier.get();
        rateLimit.setCounter(counter);
        return Single.just(rateLimit);
    }
}
