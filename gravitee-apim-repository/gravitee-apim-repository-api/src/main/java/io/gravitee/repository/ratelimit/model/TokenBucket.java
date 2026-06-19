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
package io.gravitee.repository.ratelimit.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Persisted state of a single token-bucket rate-limit key.
 *
 * <p>Unlike {@link RateLimit} (a fixed-window counter), this models burst behaviour: tokens refill
 * over time up to a burst capacity, and each request consumes tokens.
 *
 * <p>The available balance is stored in <em>whole tokens</em>. The refill rate is configured as a
 * whole-token count per period (e.g. 100 tokens / 60s), so all refill arithmetic is integer with no
 * floating-point drift across the heterogeneous backends.
 *
 * @author GraviteeSource Team
 */
public class TokenBucket implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;

    /** Available balance, in whole tokens. */
    private long tokens;

    /** Epoch millis of the last refill applied to {@link #tokens}. */
    private long lastRefillTime;

    private String subscription;

    private TokenBucket() {}

    public TokenBucket(String key) {
        this.key = key;
    }

    public TokenBucket(final TokenBucket other) {
        this.key = other.key;
        this.tokens = other.tokens;
        this.lastRefillTime = other.lastRefillTime;
        this.subscription = other.subscription;
    }

    public String getKey() {
        return key;
    }

    public long getTokens() {
        return tokens;
    }

    public void setTokens(long tokens) {
        this.tokens = tokens;
    }

    public long getLastRefillTime() {
        return lastRefillTime;
    }

    public void setLastRefillTime(long lastRefillTime) {
        this.lastRefillTime = lastRefillTime;
    }

    public String getSubscription() {
        return subscription;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenBucket that = (TokenBucket) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "TokenBucket{key='" + key + "', tokens=" + tokens + ", lastRefillTime=" + lastRefillTime + '}';
    }
}
