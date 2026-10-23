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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.subscriptions;

import static io.gravitee.repository.mongodb.management.MongoSubscriptionRepository.UNIQUE_ACTIVE_KEY_FIELD;
import static io.gravitee.repository.mongodb.management.MongoSubscriptionRepository.UNIQUE_ACTIVE_KEY_INDEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import reactor.core.publisher.Mono;

/**
 * The unique index on active subscriptions is created at startup, before the node is ready: it must never stop the
 * node, nor give up for good when it cannot be created yet.
 *
 * @author GraviteeSource Team
 */
public class UniqueActiveKeyIndexUpgraderTest extends AbstractManagementRepositoryTest {

    @Inject
    private UniqueActiveKeyIndexUpgrader upgrader;

    @Inject
    private MongoOperations mongoOperations;

    @Inject
    @Qualifier("indexManagementReactiveMongoTemplate")
    private ReactiveMongoOperations indexManagementTemplate;

    @Inject
    private Environment environment;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(
        UniqueActiveKeyIndexUpgrader.class
    );

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Before
    public void dropIndex() {
        if (index().isPresent()) {
            subscriptions().dropIndex(UNIQUE_ACTIVE_KEY_INDEX);
        }
        logs.start();
        logger.addAppender(logs);
    }

    /**
     * The index is shared with the other tests running against the same database, it has to be put back in place.
     */
    @After
    public void restoreIndex() {
        logger.detachAppender(logs);
        subscriptions().deleteMany(Filters.in("_id", List.of("sub-1", "sub-2", "sub-3")));
        aNodeStarting().upgrade();
    }

    @Test
    public void should_create_a_unique_index_on_active_subscriptions_only() {
        assertThat(upgrader.upgrade()).isTrue();

        assertThat(index()).hasValueSatisfying(index -> {
            assertThat(index.getBoolean("unique")).isTrue();
            assertThat(index.get("partialFilterExpression", Document.class)).isEqualTo(
                new Document(UNIQUE_ACTIVE_KEY_FIELD, new Document("$exists", true))
            );
        });
    }

    @Test
    public void should_declare_the_partial_filter_in_the_index_definition() {
        // Index.options() leaves the partial filter out, only the index definition carries it.
        var options = upgrader.buildIndex().toIndexDefinition().getIndexOptions();

        assertThat(options.getString("name")).isEqualTo(UNIQUE_ACTIVE_KEY_INDEX);
        assertThat(options.getBoolean("unique")).isTrue();
        assertThat(options.get("partialFilterExpression")).isNotNull();
    }

    @Test
    public void should_repair_the_keys_before_creating_the_index() {
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-1", "plan-1"));
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-2", "plan-2"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(storedDocument("sub-1")).containsKey(UNIQUE_ACTIVE_KEY_FIELD);
        assertThat(storedDocument("sub-2")).containsKey(UNIQUE_ACTIVE_KEY_FIELD);
        assertThat(index()).isPresent();
    }

    @Test
    public void should_skip_the_index_and_report_duplicated_subscriptions_without_stopping_the_node() {
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-1", "plan-1"));
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-2", "plan-1"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(index()).isEmpty();
        assertThat(logs.list)
            .map(ILoggingEvent::getFormattedMessage)
            .anySatisfy(message ->
                assertThat(message).contains("sub-1").contains("sub-2")
            );
    }

    @Test
    public void should_try_again_at_the_next_startup_while_the_index_is_missing() {
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-1", "plan-1"));
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-2", "plan-1"));
        var firstStartup = aNodeStarting();
        firstStartup.upgrade();

        // The runner skips an upgrader whose identifier it has recorded: the next startup must not reuse it.
        var nextStartup = aNodeStarting();
        assertThat(nextStartup.identifier()).isNotEqualTo(firstStartup.identifier());

        subscriptions().deleteOne(Filters.eq("_id", "sub-2"));
        assertThat(nextStartup.upgrade()).isTrue();
        assertThat(index()).isPresent();

        // Once the index exists, every startup shares the same identifier and the runner records it once.
        assertThat(aNodeStarting().identifier())
            .isEqualTo(aNodeStarting().identifier())
            .isEqualTo(UniqueActiveKeyIndexUpgrader.class.getName());
    }

    @Test
    public void should_not_stop_the_node_when_the_database_cannot_be_reached() {
        var unreachable = mock(ReactiveMongoOperations.class);
        when(unreachable.getCollection(anyString())).thenReturn(
            Mono.error(new MongoTimeoutException("timed out"))
        );
        var upgrader = new UniqueActiveKeyIndexUpgrader();
        upgrader.setEnvironment(environment);
        upgrader.setMongoTemplate(unreachable);

        assertThat(upgrader.upgrade()).isTrue();
        // and tries again at the next startup
        assertThat(upgrader.identifier()).isNotEqualTo(
            UniqueActiveKeyIndexUpgrader.class.getName()
        );
    }

    @Test
    public void should_be_harmless_when_two_nodes_run_it_at_the_same_time()
        throws Exception {
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-1", "plan-1"));
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-2", "plan-2"));
        subscriptions().insertOne(anActiveSubscriptionWithoutKey("sub-3", "plan-3"));
        var barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = List.of(aNodeStarting(), aNodeStarting())
                .stream()
                .map(node ->
                    executor.submit(() -> {
                        barrier.await();
                        return node.upgrade();
                    })
                )
                .toList();

            for (Future<Boolean> result : results) {
                assertThat(result.get()).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(index()).isPresent();
        assertThat(List.of("sub-1", "sub-2", "sub-3")).allSatisfy(id ->
            assertThat(storedDocument(id)).containsKey(UNIQUE_ACTIVE_KEY_FIELD)
        );
    }

    private UniqueActiveKeyIndexUpgrader aNodeStarting() {
        var node = new UniqueActiveKeyIndexUpgrader();
        node.setEnvironment(environment);
        node.setMongoTemplate(indexManagementTemplate);
        return node;
    }

    private Optional<Document> index() {
        return StreamSupport.stream(subscriptions().listIndexes().spliterator(), false)
            .filter(index -> UNIQUE_ACTIVE_KEY_INDEX.equals(index.getString("name")))
            .findFirst();
    }

    private Document storedDocument(String id) {
        return subscriptions().find(Filters.eq("_id", id)).first();
    }

    private MongoCollection<Document> subscriptions() {
        return mongoOperations.getCollection(
            environment.getProperty("management.mongodb.prefix", "") + "subscriptions"
        );
    }

    /**
     * As written by a node of a previous version.
     */
    private static Document anActiveSubscriptionWithoutKey(String id, String plan) {
        return new Document("_id", id)
            .append("environmentId", "DEFAULT")
            .append("referenceType", "API")
            .append("referenceId", "api-1")
            .append("api", "api-1")
            .append("application", "app-1")
            .append("plan", plan)
            .append("status", "ACCEPTED")
            .append("type", "STANDARD");
    }
}
