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
package io.gravitee.repository.redis.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RateLimit {

    private String key;

    private long lastRequest = System.currentTimeMillis();

    private long counter;

    private long resetTime;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getLastRequest() {
        return lastRequest;
    }

    public void setLastRequest(long lastRequest) {
        this.lastRequest = lastRequest;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public long getResetTime() {
        return resetTime;
    }

    public void setResetTime(long resetTime) {
        this.resetTime = resetTime;
    }

    public long getEndOfWindow(long periodTime, TimeUnit periodTimeUnit) {
        return getEndOfPeriod(getLastRequest(), periodTime, periodTimeUnit);
    }

    public long getEndOfPeriod(long startingTime, long periodTime, TimeUnit periodTimeUnit) {
        Duration duration = null;

        switch (periodTimeUnit) {
            case SECONDS:
                duration = Duration.ofSeconds(periodTime);
                break;
            case MINUTES:
                duration = Duration.ofMinutes(periodTime);
                break;
            case HOURS:
                duration = Duration.ofHours(periodTime);
                break;
            case DAYS:
                duration = Duration.ofDays(periodTime);
                break;
        }

        return Instant.ofEpochMilli(startingTime).plus(duration).toEpochMilli();
    }
}