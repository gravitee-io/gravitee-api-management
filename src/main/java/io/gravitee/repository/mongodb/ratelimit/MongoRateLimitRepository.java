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
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class MongoRateLimitRepository implements RateLimitRepository<String> {

    @Autowired
    @Qualifier("rateLimitMongoTemplate")
    private MongoOperations mongoOperations;

    private final static String RATE_LIMIT_COLLECTION = "ratelimit";

    @PostConstruct
    public void ensureTTLIndex() {
        mongoOperations.indexOps(RATE_LIMIT_COLLECTION).ensureIndex(new IndexDefinition() {
            @Override
            public DBObject getIndexKeys() {
                return new BasicDBObject("reset_time", 1);
            }

            @Override
            public DBObject getIndexOptions() {
                // To expire Documents at a Specific Clock Time we have to specify an expireAfterSeconds value of 0.
                return new BasicDBObject("expireAfterSeconds", 0);
            }
        });
    }

    @Override
    public RateLimit get(RateLimit rateLimit) {
        DBObject result = mongoOperations
                .getCollection(RATE_LIMIT_COLLECTION)
                .findOne(rateLimit.getKey());

        if (result != null) {
            rateLimit.setCounter((long) result.get("counter"));
            rateLimit.setLastRequest((long) result.get("last_request"));
            rateLimit.setResetTime(((Date) result.get("reset_time")).getTime());
        }

        return rateLimit;
    }

    @Override
    public void save(RateLimit rateLimit) {
        final DBObject doc = BasicDBObjectBuilder.start()
                .add("_id", rateLimit.getKey())
                .add("counter", rateLimit.getCounter())
                .add("last_request", rateLimit.getLastRequest())
                .add("reset_time", new Date(rateLimit.getResetTime()))
                .get();

        mongoOperations
                .getCollection(RATE_LIMIT_COLLECTION)
                .save(doc);
    }
}
