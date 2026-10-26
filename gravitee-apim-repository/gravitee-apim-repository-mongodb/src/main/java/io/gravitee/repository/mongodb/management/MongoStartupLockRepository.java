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
package io.gravitee.repository.mongodb.management;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.gravitee.repository.exceptions.StartupLockTimeoutException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.StartupLock;
import io.gravitee.repository.management.api.StartupLockRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoStartupLockRepository implements StartupLockRepository {

    private static final String COLLECTION = "startup_locks";
    private static final String LOCK_ID = "startup";
    private static final String OWNER = "owner";
    private static final String EXPIRES_AT = "expiresAt";
    private static final Duration LEASE = Duration.ofSeconds(30);
    private static final Duration RENEW_INTERVAL = Duration.ofSeconds(10);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    private final MongoOperations mongoOperations;
    private final Duration lease;
    private final Duration renewInterval;

    @Autowired
    public MongoStartupLockRepository(MongoOperations mongoOperations) {
        this(mongoOperations, LEASE, RENEW_INTERVAL);
    }

    MongoStartupLockRepository(MongoOperations mongoOperations, Duration lease, Duration renewInterval) {
        this.mongoOperations = mongoOperations;
        this.lease = lease;
        this.renewInterval = renewInterval;
    }

    @Override
    public StartupLock acquire(Duration timeout) throws TechnicalException {
        String owner = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!tryAcquire(owner)) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new StartupLockTimeoutException("Startup lock [" + LOCK_ID + "] not acquired within " + timeout);
            }
            try {
                TimeUnit.NANOSECONDS.sleep(Math.min(remaining, POLL_INTERVAL.toNanos()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TechnicalException("Interrupted while waiting for startup lock [" + LOCK_ID + "]", e);
            }
        }
        log.debug("Startup lock [{}] acquired by [{}]", LOCK_ID, owner);
        return new MongoStartupLock(owner);
    }

    private boolean tryAcquire(String owner) throws TechnicalException {
        Instant now = Instant.now();
        Query freeOrOwned = query(where("_id").is(LOCK_ID).orOperator(where(EXPIRES_AT).lt(Date.from(now)), where(OWNER).is(owner)));
        try {
            return (
                mongoOperations.findAndModify(
                    freeOrOwned,
                    leaseUpdate(owner, now),
                    FindAndModifyOptions.options().upsert(true).returnNew(true),
                    Document.class,
                    COLLECTION
                ) !=
                null
            );
        } catch (DuplicateKeyException e) {
            return false;
        } catch (DataAccessException e) {
            throw new TechnicalException("Failed to request startup lock [" + LOCK_ID + "]", e);
        }
    }

    private Update leaseUpdate(String owner, Instant now) {
        return new Update().set(OWNER, owner).set(EXPIRES_AT, Date.from(now.plus(lease)));
    }

    private static Query ownedBy(String owner) {
        return query(where("_id").is(LOCK_ID).and(OWNER).is(owner));
    }

    private final class MongoStartupLock implements StartupLock {

        private final String owner;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final ScheduledExecutorService renewer = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("gravitee-startup-lock-renewer").factory()
        );

        private MongoStartupLock(String owner) {
            this.owner = owner;
            renewer.scheduleWithFixedDelay(this::renew, renewInterval.toMillis(), renewInterval.toMillis(), TimeUnit.MILLISECONDS);
        }

        private synchronized void renew() {
            if (closed.get()) {
                return;
            }
            try {
                Update update = new Update().set(EXPIRES_AT, Date.from(Instant.now().plus(lease)));
                if (mongoOperations.updateFirst(ownedBy(owner), update, COLLECTION).getMatchedCount() == 0) {
                    log.error("Startup lock [{}] is lost: its lease expired before [{}] renewed it", LOCK_ID, owner);
                    renewer.shutdown();
                }
            } catch (DataAccessException e) {
                log.warn("Failed to renew the lease of startup lock [{}] held by [{}]", LOCK_ID, owner, e);
            }
        }

        @Override
        public synchronized void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            renewer.shutdown();
            try {
                mongoOperations.remove(ownedBy(owner), COLLECTION);
                log.debug("Startup lock [{}] released by [{}]", LOCK_ID, owner);
            } catch (DataAccessException e) {
                log.error("Failed to release startup lock [{}] held by [{}], it will be freed when its lease expires", LOCK_ID, owner, e);
            }
        }
    }
}
