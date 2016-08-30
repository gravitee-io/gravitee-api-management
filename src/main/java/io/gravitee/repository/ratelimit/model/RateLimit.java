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
package io.gravitee.repository.ratelimit.model;

import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RateLimit {

    private final String key;

    private long lastRequest = System.currentTimeMillis();

    private long counter = 0;

    private long resetTime;

    private long createdAt;

    private long updatedAt;

    private boolean async;

    public RateLimit(String key) {
        this.key = key;
    }

    public RateLimit(final RateLimit rateLimit) {
        this(rateLimit.getKey(), rateLimit);
    }

    public RateLimit(String key, final RateLimit rateLimit) {
        this(key);
        this.setCounter(rateLimit.getCounter());
        this.setLastRequest(rateLimit.getLastRequest());
        this.setResetTime(rateLimit.getResetTime());
        this.setCreatedAt(rateLimit.getCreatedAt());
        this.setUpdatedAt(rateLimit.getUpdatedAt());
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public String getKey() {
        return key;
    }

    public long getLastRequest() {
        return lastRequest;
    }

    public void setLastRequest(long lastRequest) {
        this.lastRequest = lastRequest;
    }

    public long getResetTime() {
        return resetTime;
    }

    public void setResetTime(long resetTime) {
        this.resetTime = resetTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateLimit rateLimit = (RateLimit) o;
        return Objects.equals(key, rateLimit.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("counter=").append(counter);
        sb.append(", key='").append(key).append('\'');
        sb.append(", lastRequest=").append(lastRequest);
        sb.append(", resetTime=").append(resetTime);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append('}');
        return sb.toString();
    }
}
