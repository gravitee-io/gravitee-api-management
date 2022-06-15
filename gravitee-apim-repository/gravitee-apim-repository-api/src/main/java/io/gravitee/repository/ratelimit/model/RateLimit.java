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

import java.io.Serializable;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RateLimit implements Serializable {

    private String key;

    private long counter = 0;

    private long resetTime;

    private long limit;

    private String subscription;

    private RateLimit() {}

    public RateLimit(String key) {
        this.key = key;
    }

    public RateLimit(final RateLimit rateLimit) {
        this(rateLimit.getKey(), rateLimit);
    }

    public RateLimit(String key, final RateLimit rateLimit) {
        this(key);
        this.setCounter(rateLimit.getCounter());
        this.setLimit(rateLimit.getLimit());
        this.setResetTime(rateLimit.getResetTime());
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

    public long getResetTime() {
        return resetTime;
    }

    public void setResetTime(long resetTime) {
        this.resetTime = resetTime;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
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
        RateLimit rateLimit = (RateLimit) o;
        return Objects.equals(key, rateLimit.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "RateLimit{" + "key='" + key + '\'' + ", counter=" + counter + ", resetTime=" + resetTime + ", limit=" + limit + '}';
    }
}
