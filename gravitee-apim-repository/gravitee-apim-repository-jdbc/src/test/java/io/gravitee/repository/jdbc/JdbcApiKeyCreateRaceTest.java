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
package io.gravitee.repository.jdbc;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The first shared API key of an application has an id derived from the application, so that Management API nodes subscribing
 * the application at the same time cannot all create one: every insert but the first fails. A node that lost the race links its
 * subscription to the winner's key instead, inside the single transaction of its subscribe request. So:
 * <ul>
 *     <li>the lost insert must leave that transaction usable (PostgreSQL aborts a transaction on any failed statement);</li>
 *     <li>the transaction must find the winner's key although it read the keys of the application before the winner committed
 *     (MySQL and MariaDB keep reading the snapshot of the first read in REPEATABLE READ, their default isolation);</li>
 *     <li>several transactions that lost at the same time must all commit.</li>
 * </ul>
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcApiKeyCreateRaceTest extends AbstractRepositoryTest {

    private static final String APPLICATION = "app-racing-for-its-first-shared-key";
    private static final String SHARED_KEY_ID = "first-shared-key-of-the-application";
    private static final long TIMEOUT_SECONDS = 60;

    @Inject
    private ApiKeyRepository apiKeyRepository;

    @Inject
    private PlatformTransactionManager transactionManager;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private Properties graviteeProperties;

    private final ExecutorService otherNodes = Executors.newFixedThreadPool(3);

    @After
    public void stopOtherNodes() {
        otherNodes.shutdownNow();
    }

    @Test
    public void should_commit_the_subscribe_transaction_that_lost_the_race() throws Exception {
        apiKeyRepository.create(sharedKey("sub-of-the-winner"));

        inTransaction(() -> {
            assertThatThrownBy(() -> apiKeyRepository.create(sharedKey("sub-of-the-loser"))).isInstanceOf(DuplicateKeyException.class);
            linkToEveryKeyOfTheApplication("sub-of-the-loser");
            // Any later statement of the same transaction, like the rest of the subscribe request.
            apiKeyRepository.create(apiKey("key-created-later-in-the-transaction", "another-application", "sub-other"));
        });

        assertThat(storedSubscriptions()).containsExactlyInAnyOrder("sub-of-the-winner", "sub-of-the-loser");
        assertThat(countLinks("sub-of-the-loser")).isEqualTo(1);
        assertThat(apiKeyRepository.findById("key-created-later-in-the-transaction")).isPresent();
    }

    @Test
    public void should_link_the_winning_key_from_a_transaction_that_read_the_keys_before_the_winner_committed() throws Exception {
        inTransaction(() -> {
            // The subscribe request reads the keys of the application (none yet) before it creates the first one.
            assertThat(apiKeyRepository.findByApplication(APPLICATION)).isEmpty();
            onAnotherNode(() -> apiKeyRepository.create(sharedKey("sub-of-the-winner"))).get(TIMEOUT_SECONDS, SECONDS);

            assertThatThrownBy(() -> apiKeyRepository.create(sharedKey("sub-of-the-loser"))).isInstanceOf(DuplicateKeyException.class);
            linkToEveryKeyOfTheApplication("sub-of-the-loser");
        });

        assertThat(storedSubscriptions()).containsExactlyInAnyOrder("sub-of-the-winner", "sub-of-the-loser");
        assertThat(countLinks("sub-of-the-loser")).isEqualTo(1);
    }

    @Test
    public void should_commit_every_subscribe_transaction_that_lost_the_race_at_the_same_time() throws Exception {
        CountDownLatch losersRead = new CountDownLatch(2);
        CountDownLatch winnerInserted = new CountDownLatch(1);
        CountDownLatch losersInserting = new CountDownLatch(2);
        CountDownLatch losersLost = new CountDownLatch(2);

        Future<?> winner = onAnotherNode(() -> {
            // Insert only once the losers have read: on SQL Server, a read waits for an uncommitted row.
            losersRead.await(TIMEOUT_SECONDS, SECONDS);
            apiKeyRepository.create(sharedKey("sub-of-the-winner"));
            winnerInserted.countDown();
            // Commit only once the losers are waiting for the uncommitted key.
            losersInserting.await(TIMEOUT_SECONDS, SECONDS);
            Thread.sleep(500);
        });
        List<Future<?>> losers = Stream.of("sub-of-loser-1", "sub-of-loser-2")
            .<Future<?>>map(subscription ->
                onAnotherNode(() -> {
                    assertThat(apiKeyRepository.findByApplication(APPLICATION)).isEmpty();
                    losersRead.countDown();
                    winnerInserted.await(TIMEOUT_SECONDS, SECONDS);
                    losersInserting.countDown();
                    assertThatThrownBy(() -> apiKeyRepository.create(sharedKey(subscription))).isInstanceOf(DuplicateKeyException.class);
                    // Both losers go on together, each keeping whatever its lost insert left it.
                    losersLost.countDown();
                    losersLost.await(5, SECONDS);
                    linkToEveryKeyOfTheApplication(subscription);
                })
            )
            .toList();

        winner.get(TIMEOUT_SECONDS, SECONDS);
        for (Future<?> loser : losers) {
            loser.get(TIMEOUT_SECONDS, SECONDS);
        }

        assertThat(storedSubscriptions()).containsExactlyInAnyOrder("sub-of-the-winner", "sub-of-loser-1", "sub-of-loser-2");
    }

    /** What the node that lost the race does: link its subscription to every key of the application, as found now. */
    private void linkToEveryKeyOfTheApplication(String subscriptionId) throws Exception {
        List<ApiKey> keys = apiKeyRepository.findLatestByApplication(APPLICATION);
        assertThat(keys).extracting(ApiKey::getId).containsExactly(SHARED_KEY_ID);
        for (ApiKey key : keys) {
            assertThat(apiKeyRepository.addSubscription(key.getId(), subscriptionId)).isPresent();
        }
    }

    private List<String> storedSubscriptions() throws Exception {
        return apiKeyRepository.findById(SHARED_KEY_ID).orElseThrow().getSubscriptions();
    }

    private int countLinks(String subscriptionId) {
        String keySubscriptions = graviteeProperties.getProperty("management.jdbc.prefix", "") + "key_subscriptions";
        return jdbcTemplate.queryForObject(
            "select count(*) from " + keySubscriptions + " where key_id = ? and subscription_id = ?",
            Integer.class,
            SHARED_KEY_ID,
            subscriptionId
        );
    }

    private void inTransaction(Work work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            try {
                work.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /** Runs the work in its own transaction, on another connection, like another Management API node. */
    private Future<?> onAnotherNode(Work work) {
        return otherNodes.submit(() -> inTransaction(work));
    }

    /** Each node generates its own random key value; only the id of the first shared key is the same. */
    private static ApiKey sharedKey(String subscriptionId) {
        return apiKey(SHARED_KEY_ID, APPLICATION, subscriptionId);
    }

    private static ApiKey apiKey(String id, String application, String subscriptionId) {
        return ApiKey.builder()
            .id(id)
            .key(UUID.randomUUID().toString())
            .environmentId("DEFAULT")
            .application(application)
            .subscriptions(new ArrayList<>(List.of(subscriptionId)))
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
    }

    @FunctionalInterface
    private interface Work {
        void run() throws Exception;
    }

    @Override
    protected String getTestCasesPath() {
        return "";
    }

    @Override
    protected String getModelPackage() {
        return "";
    }

    @Override
    protected void createModel(Object object) {}
}
