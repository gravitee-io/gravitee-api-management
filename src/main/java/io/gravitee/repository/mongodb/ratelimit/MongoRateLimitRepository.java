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
package io.gravitee.repository.mongodb.ratelimit;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimitResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class MongoRateLimitRepository implements RateLimitRepository<String> {

    @Autowired
    @Qualifier("rateLimitMongoTemplate")
    private MongoOperations mongoOperations;

    @PostConstruct
    public void ensureTTLIndex() {
        mongoOperations.indexOps(RateLimit.class).ensureIndex(new IndexDefinition() {
            @Override
            public DBObject getIndexKeys() {
                return new BasicDBObject("resetTime", 1);
            }

            @Override
            public DBObject getIndexOptions() {
                // To expire Documents at a Specific Clock Time we have to specify an expireAfterSeconds value of 0.
                return new BasicDBObject("expireAfterSeconds", 0);
            }
        });
    }

    @Override
    public RateLimitResult acquire(String key, int pound, long limit, long periodTime, TimeUnit periodTimeUnit) {

        RateLimit rateLimit = mongoOperations.findOne(query(where("key").is(key)), RateLimit.class);
        if (rateLimit == null) {
            rateLimit = new RateLimit();
            rateLimit.setKey(key);
        }

        RateLimitResult rateLimitResult = new RateLimitResult();

        // We prefer currentTimeMillis in place of nanoTime() because nanoTime is relatively
        // expensive call and depends on the underlying architecture.

        long now = System.currentTimeMillis();
        long endOfWindow = rateLimit.getEndOfWindow(periodTime, periodTimeUnit);

        if (now >= endOfWindow) {
            rateLimit.setCounter(0);
        }

        if (rateLimit.getCounter() >= limit) {
            rateLimitResult.setExceeded(true);
        } else {
            // Update rate limiter
            rateLimitResult.setExceeded(false);
            rateLimit.setCounter(rateLimit.getCounter() + pound);
            rateLimit.setLastRequest(now);
        }

        // Set the time at which the current rate limit window resets in UTC epoch seconds.
        long resetTimeMillis = rateLimit.getEndOfPeriod(now, periodTime, periodTimeUnit);
        rateLimitResult.setResetTime(resetTimeMillis / 1000L);
        rateLimit.setResetTime(new Date(resetTimeMillis));
        rateLimitResult.setRemains(limit - rateLimit.getCounter());

        mongoOperations.save(rateLimit);

        return rateLimitResult;
    }
}
