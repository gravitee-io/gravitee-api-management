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
package io.gravitee.repository.mongodb.ratelimit;

import io.gravitee.repository.ratelimit.api.TokenBucketCalculator;
import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.reactivex.rxjava3.core.Single;
import java.util.Date;
import java.util.function.Supplier;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.aggregation.SetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.adapter.rxjava.RxJava3Adapter;

/**
 * MongoDB-backed {@link TokenBucketRateLimitRepository}. Refill-and-consume is a single atomic
 * {@code findAndModify} using an aggregation update pipeline, so concurrent requests on a key are
 * serialised by MongoDB's document-level atomicity without a read-modify-write race.
 *
 * <p>Balances are whole tokens (see {@link TokenBucketCalculator}) and the pipeline mirrors that
 * calculator's integer math server-side. The refill clock is the caller-supplied {@code nowMillis};
 * the TTL {@code expire_at} uses real wall-clock time, since eviction is a storage concern. The TTL
 * index itself is created on the rate-limit datastore by {@link RateLimitRepositoryConfiguration}.
 */
public class MongoTokenBucketRateLimitRepository implements TokenBucketRateLimitRepository<TokenBucket> {

    static final String COLLECTION = "tokenbucket";
    static final String FIELD_EXPIRE_AT = "expire_at";

    private static final String FIELD_ID = "_id";
    private static final String FIELD_TOKENS = "tokens";
    private static final String FIELD_LAST_REFILL = "last_refill";
    private static final String FIELD_SUBSCRIPTION = "subscription";
    // Transient scratch (pre-consume balance): the pipeline sets it so `tokensAfter` can reference it and
    // toResult can read back whether the request was satisfiable. It must survive into the returned doc
    // (findAndModify returnNew), so it is not $unset; it is overwritten on every request.
    private static final String FIELD_REFILLED = "_refilled";

    private final ReactiveMongoOperations mongoOperations;
    private final String collection;

    public MongoTokenBucketRateLimitRepository(ReactiveMongoOperations mongoOperations, String prefix) {
        this.mongoOperations = mongoOperations;
        this.collection = prefix + COLLECTION;
    }

    @Override
    public Single<TokenBucketConsumeResult> refillAndTryConsume(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        Supplier<TokenBucket> supplier
    ) {
        TokenBucketCalculator.requireValidArgs(tokensRequested, refillPeriodMillis, capacity);

        String subscription = supplier.get().getSubscription();
        Date expireAt = new Date(System.currentTimeMillis() + TokenBucketCalculator.ttlMillis(refillRate, refillPeriodMillis, capacity));
        // Cap the elapsed span (a long-idle bucket never accrues past capacity) so elapsed * refillRate
        // cannot overflow a 64-bit long for huge idle gaps. A zero rate never refills.
        long maxUsefulElapsed = refillRate > 0 ? (capacity * refillPeriodMillis) / refillRate + refillPeriodMillis : 0L;

        // The refill anchor, defaulting to now for a missing bucket (so a fresh bucket starts full).
        AggregationExpression anchor = ConditionalOperators.ifNull(FIELD_LAST_REFILL).then(nowMillis);
        // elapsed = max(0, now - anchor), expressed via (anchor - now) so the document field is the
        // operand base: when anchor >= now (out-of-order / no time passed) elapsed is 0, else now - anchor.
        AggregationExpression anchorMinusNow = ArithmeticOperators.valueOf(anchor).subtract(nowMillis);
        AggregationExpression elapsed = ConditionalOperators.when(ComparisonOperators.valueOf(anchorMinusNow).greaterThanEqualToValue(0L))
            .then(0L)
            .otherwise(ArithmeticOperators.valueOf(anchorMinusNow).multiplyBy(-1));
        // effectiveElapsed = min(elapsed, maxUsefulElapsed) to bound the multiplication below.
        AggregationExpression effectiveElapsed = ConditionalOperators.when(
            ComparisonOperators.valueOf(elapsed).greaterThanValue(maxUsefulElapsed)
        )
            .then(maxUsefulElapsed)
            .otherwise(elapsed);
        // refillAmount = floor(effectiveElapsed * refillRate / refillPeriodMillis) as a long — whole tokens.
        // A zero rate yields 0 (no refill).
        AggregationExpression refillAmount = refillRate <= 0
            ? (AggregationExpression) context -> new org.bson.Document("$literal", 0L)
            : ConvertOperators.valueOf(
                ArithmeticOperators.valueOf(
                    ArithmeticOperators.valueOf(ArithmeticOperators.valueOf(effectiveElapsed).multiplyBy(refillRate)).divideBy(
                        refillPeriodMillis
                    )
                ).floor()
            ).convertToLong();
        // refilled = min(capacity, currentTokens + refillAmount); a missing bucket defaults to full.
        AggregationExpression currentTokens = ConditionalOperators.ifNull(FIELD_TOKENS).then(capacity);
        AggregationExpression refilledRaw = ArithmeticOperators.valueOf(currentTokens).add(refillAmount);
        AggregationExpression refilled = ConditionalOperators.when(ComparisonOperators.valueOf(refilledRaw).greaterThanValue(capacity))
            .then(capacity)
            .otherwise(refilledRaw);

        // tokens after consume: subtract when the refilled balance covers the request, otherwise leave it.
        AggregationExpression tokensAfter = ConditionalOperators.when(
            ComparisonOperators.valueOf(FIELD_REFILLED).greaterThanEqualToValue(tokensRequested)
        )
            .then(ArithmeticOperators.valueOf(FIELD_REFILLED).subtract(tokensRequested))
            .otherwiseValueOf(FIELD_REFILLED);
        // Anchor forward to now only once a whole token is credited (refillAmount > 0); otherwise leave it
        // put so an out-of-order or sub-token elapsed keeps accumulating instead of being discarded or
        // re-credited (which would over-admit).
        AggregationExpression nextAnchor = ConditionalOperators.when(ComparisonOperators.valueOf(refillAmount).greaterThanValue(0L))
            .then(nowMillis)
            .otherwise(anchor);

        AggregationUpdate update = AggregationUpdate.update()
            .set(SetOperation.set(FIELD_REFILLED).toValue(refilled))
            .set(SetOperation.set(FIELD_SUBSCRIPTION).toValue(ConditionalOperators.ifNull(FIELD_SUBSCRIPTION).then(subscription)))
            .set(SetOperation.set(FIELD_TOKENS).toValue(tokensAfter))
            .set(SetOperation.set(FIELD_LAST_REFILL).toValue(nextAnchor))
            .set(SetOperation.set(FIELD_EXPIRE_AT).toValue(expireAt));

        Query query = new Query(Criteria.where(FIELD_ID).is(key));
        // A single atomic findAndModify(upsert): unlike JDBC (SELECT-then-INSERT, which retries the
        // first-insert race) there is no select/insert window here, so no E11000 retry is needed — same
        // pattern as MongoRateLimitRepository.
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);

        return RxJava3Adapter.monoToSingle(
            mongoOperations
                .findAndModify(query, update, options, Document.class, collection)
                .map(document -> toResult(document, tokensRequested, refillRate, refillPeriodMillis, nowMillis))
        );
    }

    private static TokenBucketConsumeResult toResult(
        Document document,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long nowMillis
    ) {
        long refilled = asLong(document, FIELD_REFILLED);
        long tokensAfter = asLong(document, FIELD_TOKENS);
        boolean allowed = refilled >= tokensRequested;
        return new TokenBucketConsumeResult(
            allowed,
            tokensAfter,
            TokenBucketCalculator.nextAvailableAtMillis(tokensAfter, refillRate, refillPeriodMillis, nowMillis)
        );
    }

    private static long asLong(Document document, String field) {
        Object value = document.get(field);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("token-bucket field '" + field + "' is missing or not numeric: " + value);
        }
        return number.longValue();
    }
}
