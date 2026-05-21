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
package io.gravitee.repository.mongodb.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.client.AggregateIterable;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class MongoQueriesTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Mock
    AggregateIterable<Document> aggregateIterable;

    @Captor
    ArgumentCaptor<Query> queryCaptor;

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(MongoQueries.class);

    @BeforeEach
    void attachAppender() {
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void countOrTimeout_returns_minus_one_when_count_times_out() {
        when(mongoTemplate.count(any(Query.class), eq(Object.class))).thenThrow(
            new UncategorizedMongoDbException("Wrapped", new MongoExecutionTimeoutException(50, "operation exceeded time limit"))
        );

        MongoQueries mongoQueries = new MongoQueries(2000L);

        long total = mongoQueries.countOrTimeout(mongoTemplate, new Query(), Object.class);

        assertThat(total).isEqualTo(-1L);
    }

    @Test
    void countOrTimeout_returns_count_when_query_succeeds() {
        when(mongoTemplate.count(any(Query.class), eq(Object.class))).thenReturn(42L);
        MongoQueries mongoQueries = new MongoQueries(2000L);

        long total = mongoQueries.countOrTimeout(mongoTemplate, new Query(), Object.class);

        assertThat(total).isEqualTo(42L);
    }

    @Test
    void countOrTimeout_logs_warning_with_collection_and_timeout_when_count_times_out() {
        when(mongoTemplate.count(any(Query.class), eq(Object.class))).thenThrow(
            new UncategorizedMongoDbException("Wrapped", new MongoExecutionTimeoutException(50, "operation exceeded time limit"))
        );
        MongoQueries mongoQueries = new MongoQueries(2500L);

        mongoQueries.countOrTimeout(mongoTemplate, new Query(), Object.class);

        assertThat(logAppender.list).anyMatch(
            e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("Object") && e.getFormattedMessage().contains("2500")
        );
    }

    @Test
    void countAggregationOrTimeout_returns_minus_one_when_aggregation_times_out() {
        when(aggregateIterable.maxTime(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(aggregateIterable);
        when(aggregateIterable.first()).thenThrow(new MongoExecutionTimeoutException(50, "operation exceeded time limit"));
        MongoQueries mongoQueries = new MongoQueries(2000L);

        long total = mongoQueries.countAggregationOrTimeout(aggregateIterable, "totalCount", "subscriptions");

        assertThat(total).isEqualTo(-1L);
    }

    @Test
    void countAggregationOrTimeout_returns_named_field_from_first_document_when_aggregation_succeeds() {
        Document firstDoc = new Document("totalCount", 123);
        when(aggregateIterable.maxTime(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(aggregateIterable);
        when(aggregateIterable.first()).thenReturn(firstDoc);
        MongoQueries mongoQueries = new MongoQueries(2000L);

        long total = mongoQueries.countAggregationOrTimeout(aggregateIterable, "totalCount", "subscriptions");

        assertThat(total).isEqualTo(123L);
    }

    @Test
    void countAggregationOrTimeout_returns_zero_when_first_document_is_absent() {
        when(aggregateIterable.maxTime(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(aggregateIterable);
        when(aggregateIterable.first()).thenReturn(null);
        MongoQueries mongoQueries = new MongoQueries(2000L);

        long total = mongoQueries.countAggregationOrTimeout(aggregateIterable, "totalCount", "subscriptions");

        assertThat(total).isZero();
    }

    @Test
    void countOrTimeout_applies_configured_timeout_to_query_before_count() {
        when(mongoTemplate.count(any(Query.class), eq(Object.class))).thenReturn(42L);
        MongoQueries mongoQueries = new MongoQueries(1500L);

        mongoQueries.countOrTimeout(mongoTemplate, new Query(), Object.class);

        verify(mongoTemplate).count(queryCaptor.capture(), eq(Object.class));
        assertThat(queryCaptor.getValue().getMeta().getMaxTimeMsec()).isEqualTo(1500L);
    }
}
