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
package io.gravitee.repository.jdbc.ratelimit;

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.function.Supplier;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRateLimitRepository implements RateLimitRepository<RateLimit> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRateLimitRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    private static String buildInsertStatement() {
        return "insert into ratelimit (" +
                escapeReservedWord("key") +
                " , counter " +
                " , " + escapeReservedWord("limit") + " " +
                " , subscription " +
                " , reset_time " +
                " ) values (?,  ? ,  ?,  ?,  ?)";
    }

    private static String buildUpdateStatement() {
        return "update ratelimit set " +
                " counter = ? " +
                " , " + escapeReservedWord("limit") + " = ? " +
                " , reset_time = ? " +
                " where " + escapeReservedWord("key") + " = ?";
    }

    private static String buildSelectStatement() {
        return "select " +
                escapeReservedWord("key") +
                ", counter, " + escapeReservedWord("limit") + ", subscription, reset_time"
                + " from ratelimit"
                + " where " + escapeReservedWord("key") + " = ?";
    }

    private static final String INSERT_SQL = buildInsertStatement();
    private static final String UPDATE_SQL = buildUpdateStatement();
    private static final String SELECT_SQL = buildSelectStatement();

    public JdbcRateLimitRepository(
            @Autowired @Qualifier("graviteeTransactionManager") PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);

        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
        this.transactionTemplate.setTimeout(5); // 5 seconds
    }

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        LOGGER.debug("JdbcRateLimitRepository.incrementAndGet({}, {}, {})", key, weight, supplier);

        return transactionTemplate.execute(new TransactionCallback<Single<RateLimit>>() {

            @Override
            public Single<RateLimit> doInTransaction(TransactionStatus transactionStatus) {
                try {
                    RateLimit rate = jdbcTemplate.query(SELECT_SQL, MAPPER, key);

                    if (rate == null || rate.getResetTime() < System.currentTimeMillis()) {
                        rate = supplier.get();
                    }

                    rate.setCounter(rate.getCounter() + weight);

                    final RateLimit fRate = rate;

                    final int nbUpdatedElements = jdbcTemplate.update((Connection cnctn) -> {
                        PreparedStatement stmt = cnctn.prepareStatement(UPDATE_SQL);
                        stmt.setLong(1, fRate.getCounter());
                        stmt.setLong(2, fRate.getLimit());
                        stmt.setLong(3, fRate.getResetTime());
                        stmt.setString(4, fRate.getKey());
                        return stmt;
                    });

                    if (nbUpdatedElements == 0) {
                        jdbcTemplate.update((Connection cnctn) -> {
                            PreparedStatement stmt = cnctn.prepareStatement(INSERT_SQL);
                            stmt.setString(1, fRate.getKey());
                            stmt.setLong(2, fRate.getCounter());
                            stmt.setLong(3, fRate.getLimit());
                            stmt.setString(4, fRate.getSubscription());
                            stmt.setLong(5, fRate.getResetTime());
                            return stmt;
                        });
                    }

                    return Single.just(rate);
                } catch (Exception ex) {
                    transactionStatus.setRollbackOnly();
                    return Single.error(ex);
                }
            }
        });
    }

    private static final ResultSetExtractor<RateLimit> MAPPER = rs -> {
        if (!rs.next()) {
            return null;
        }

        RateLimit rateLimit = new RateLimit(rs.getString(1));
        rateLimit.setCounter(rs.getLong(2));
        rateLimit.setLimit(rs.getLong(3));
        rateLimit.setSubscription(rs.getString(4));
        rateLimit.setResetTime(rs.getLong(5));

        return rateLimit;
    };
}