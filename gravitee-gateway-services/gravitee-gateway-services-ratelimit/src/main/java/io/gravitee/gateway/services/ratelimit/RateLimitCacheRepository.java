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
package io.gravitee.gateway.services.ratelimit;

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
final class RateLimitCacheRepository implements RateLimitRepository {

    private final Cache cache;

    RateLimitCacheRepository(final Cache cache) {
        this.cache = cache;
    }

    @Override
    public RateLimit get(RateLimit rateLimit) {
        // Get data from cache
        Element elt = cache.get(rateLimit.getKey());

        return (elt != null) ? (RateLimit) elt.getObjectValue() : null;
    }

    @Override
    public void save(RateLimit rateLimit) {
        Element eltRateLimit = new Element(rateLimit.getKey(), rateLimit);
        eltRateLimit.setTimeToLive((int) (rateLimit.getResetTime() - System.currentTimeMillis()) / 1000);

        cache.put(eltRateLimit);
    }
}
