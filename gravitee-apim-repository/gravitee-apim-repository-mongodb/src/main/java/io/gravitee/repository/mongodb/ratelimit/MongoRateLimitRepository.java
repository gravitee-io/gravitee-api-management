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

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.Single;
import java.util.Date;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Mono;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoRateLimitRepository implements RateLimitRepository<RateLimit> {

    @Autowired
    @Qualifier("rateLimitMongoTemplate")
    private ReactiveMongoOperations mongoOperations;

    @Value("${ratelimit.mongodb.prefix:}")
    private String prefix;

    private static final String RATE_LIMIT_COLLECTION = "ratelimit";

    private static final String FIELD_KEY = "_id";
    private static final String FIELD_COUNTER = "counter";
    private static final String FIELD_RESET_TIME = "reset_time";
    private static final String FIELD_LIMIT = "limit";
    private static final String FIELD_SUBSCRIPTION = "subscription";

    private final FindAndModifyOptions INC_AND_GET_OPTIONS = new FindAndModifyOptions().returnNew(true).upsert(true);

    @PostConstruct
    public void ensureTTLIndex() {
        mongoOperations
            .indexOps(getRateLimitCollectionName())
            .ensureIndex(new Index(FIELD_RESET_TIME, Sort.Direction.ASC).expire(0L))
            .subscribe();
    }

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        final Date now = new Date();
        RateLimit rateLimit = supplier.get();
        return RxJava2Adapter.monoToSingle(
            Mono
                .just(rateLimit)
                .flatMap(
                    new Function<RateLimit, Mono<Document>>() {
                        @Override
                        public Mono<Document> apply(RateLimit rateLimit) {
                            return mongoOperations.findAndModify(
                                new Query(Criteria.where(FIELD_KEY).is(key)),
                                new Update()
                                    .inc(FIELD_COUNTER, weight)
                                    .setOnInsert(FIELD_RESET_TIME, new Date(rateLimit.getResetTime()))
                                    .setOnInsert(FIELD_LIMIT, rateLimit.getLimit())
                                    .setOnInsert(FIELD_SUBSCRIPTION, rateLimit.getSubscription()),
                                INC_AND_GET_OPTIONS,
                                Document.class,
                                getRateLimitCollectionName()
                            );
                        }
                    }
                )
                .flatMap(
                    new Function<Document, Mono<Document>>() {
                        @Override
                        public Mono<Document> apply(Document document) {
                            if (document.getDate(FIELD_RESET_TIME).before(now)) {
                                return mongoOperations.findAndModify(
                                    new Query(Criteria.where(FIELD_KEY).is(key)),
                                    new Update()
                                        .set(FIELD_COUNTER, weight)
                                        .set(FIELD_RESET_TIME, new Date(rateLimit.getResetTime()))
                                        .set(FIELD_LIMIT, rateLimit.getLimit())
                                        .set(FIELD_SUBSCRIPTION, rateLimit.getSubscription()),
                                    INC_AND_GET_OPTIONS,
                                    Document.class,
                                    getRateLimitCollectionName()
                                );
                            } else {
                                return Mono.just(document);
                            }
                        }
                    }
                )
                .map(this::convert)
        );
    }

    private String getRateLimitCollectionName() {
        return prefix + RATE_LIMIT_COLLECTION;
    }

    private RateLimit convert(Document document) {
        if (document == null) {
            return null;
        }

        RateLimit rateLimit = new RateLimit(document.getString(FIELD_KEY));
        rateLimit.setCounter(document.getLong(FIELD_COUNTER));
        rateLimit.setLimit(document.getLong(FIELD_LIMIT));
        rateLimit.setResetTime(document.getDate(FIELD_RESET_TIME).getTime());
        rateLimit.setSubscription(document.getString(FIELD_SUBSCRIPTION));

        return rateLimit;
    }
}
