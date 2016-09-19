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
import java.util.Iterator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoRateLimitRepository implements RateLimitRepository {

    @Autowired
    @Qualifier("rateLimitMongoTemplate")
    private MongoOperations mongoOperations;

    private final static String RATE_LIMIT_COLLECTION = "ratelimit";

    private final static String FIELD_KEY = "_id";
    private final static String FIELD_COUNTER = "counter";
    private final static String FIELD_RESET_TIME = "reset_time";
    private final static String FIELD_LAST_REQUEST = "last_request";
    private final static String FIELD_UPDATED_AT = "updated_at";
    private final static String FIELD_CREATED_AT = "created_at";
    private final static String FIELD_ASYNC = "async";

    @PostConstruct
    public void ensureTTLIndex() {
        mongoOperations.indexOps(RATE_LIMIT_COLLECTION).ensureIndex(new IndexDefinition() {
            @Override
            public DBObject getIndexKeys() {
                return new BasicDBObject(FIELD_RESET_TIME, 1);
            }

            @Override
            public DBObject getIndexOptions() {
                // To expire Documents at a Specific Clock Time we have to specify an expireAfterSeconds value of 0.
                return new BasicDBObject("expireAfterSeconds", 0);
            }
        });
    }

    @Override
    public RateLimit get(String rateLimitKey) {
        DBObject result = mongoOperations
                .getCollection(RATE_LIMIT_COLLECTION)
                .findOne(rateLimitKey);

        RateLimit rateLimit = new RateLimit(rateLimitKey);

        if (result != null) {
            rateLimit.setCounter((long) result.get(FIELD_COUNTER));
            rateLimit.setLastRequest((long) result.get(FIELD_LAST_REQUEST));
            rateLimit.setResetTime(((Date) result.get(FIELD_RESET_TIME)).getTime());
            rateLimit.setUpdatedAt((long) result.get(FIELD_UPDATED_AT));
            rateLimit.setCreatedAt((long) result.get(FIELD_CREATED_AT));
            rateLimit.setAsync((boolean) result.get(FIELD_ASYNC));
        }

        return rateLimit;
    }

    @Override
    public void save(RateLimit rateLimit) {
        final DBObject doc = BasicDBObjectBuilder.start()
                .add(FIELD_KEY, rateLimit.getKey())
                .add(FIELD_COUNTER, rateLimit.getCounter())
                .add(FIELD_LAST_REQUEST, rateLimit.getLastRequest())
                .add(FIELD_RESET_TIME, new Date(rateLimit.getResetTime()))
                .add(FIELD_UPDATED_AT, rateLimit.getUpdatedAt())
                .add(FIELD_CREATED_AT, rateLimit.getCreatedAt())
                .add(FIELD_ASYNC, rateLimit.isAsync())
                .get();

        mongoOperations
                .getCollection(RATE_LIMIT_COLLECTION)
                .save(doc);
    }

    @Override
    public Iterator<RateLimit> findAsyncAfter(long timestamp) {
        DBObject query = BasicDBObjectBuilder
                .start(FIELD_ASYNC, true)
                .add(FIELD_UPDATED_AT, BasicDBObjectBuilder.start("$gte", timestamp).get())
                .get();

        final Iterator<DBObject> dbObjects = mongoOperations
                .getCollection(RATE_LIMIT_COLLECTION)
                .find(query).iterator();

        return new Iterator<RateLimit>() {

            @Override
            public boolean hasNext() {
                return dbObjects.hasNext();
            }

            @Override
            public RateLimit next() {
                DBObject dbObject = dbObjects.next();
                RateLimit rateLimit = new RateLimit((String) dbObject.get(FIELD_KEY));
                rateLimit.setCounter((long) dbObject.get(FIELD_COUNTER));
                rateLimit.setLastRequest((long) dbObject.get(FIELD_LAST_REQUEST));
                rateLimit.setResetTime(((Date) dbObject.get(FIELD_RESET_TIME)).getTime());
                rateLimit.setUpdatedAt((long) dbObject.get(FIELD_UPDATED_AT));
                rateLimit.setCreatedAt((long) dbObject.get(FIELD_CREATED_AT));
                rateLimit.setAsync((boolean) dbObject.get(FIELD_ASYNC));
                return rateLimit;
            }
        };
    }
}
