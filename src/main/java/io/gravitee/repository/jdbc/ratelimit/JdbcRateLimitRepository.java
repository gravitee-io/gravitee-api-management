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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRateLimitRepository implements RateLimitRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRateLimitRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static String buildInsertStatement() {
        return "insert into ratelimit (" +
                escapeReservedWord("key") +
                " , counter " +
                " , last_request " +
                " , reset_time " +
                " , created_at " +
                " , updated_at " +
                " , async " +
                " ) values (?,  ? ,  ?,  ?,  ?,  ?,  ?)";
    }

    private static final String INSERT_SQL = buildInsertStatement();

    private static String buildUpdateStatement() {
        return "update ratelimit set " +
                escapeReservedWord("key") + " = ? " +
                " , counter = ? " +
                " , last_request = ? " +
                " , reset_time = ? " +
                " , created_at = ? " +
                " , updated_at = ? " +
                " , async = ?" +
                " where " + escapeReservedWord("key") + " = ?";
    }

    private static final String UPDATE_SQL = buildUpdateStatement();

    private static class Rm implements RowMapper<RateLimit> {

        @Override
        public RateLimit mapRow(ResultSet rs, int i) throws SQLException {
            RateLimit rateLimit = new RateLimit(rs.getString(1));
            rateLimit.setCounter(rs.getLong(2));
            rateLimit.setLastRequest(rs.getLong(3));
            rateLimit.setResetTime(rs.getLong(4));
            rateLimit.setCreatedAt(rs.getLong(5));
            rateLimit.setUpdatedAt(rs.getLong(6));
            rateLimit.setAsync(rs.getBoolean(7));
            return rateLimit;
        }

    }

    private static final Rm MAPPER = new Rm();

    @Override
    public RateLimit get(String rateLimitKey) {
        LOGGER.debug("JdbcRateLimitRepository.get({})", rateLimitKey);
        final String escapedKeyColumn = escapeReservedWord("key");
        List<RateLimit> items = jdbcTemplate.query("select " + escapedKeyColumn
                        + " , counter, last_request, reset_time, created_at, updated_at, async "
                        + " from ratelimit "
                        + " where " + escapedKeyColumn + " = ?"
                , MAPPER
                , rateLimitKey
        );
        if (items.isEmpty()) {
            return new RateLimit(rateLimitKey);
        } else {
            return items.get(0);
        }
    }

    @Override
    public void save(RateLimit rateLimit) {
        LOGGER.debug("JdbcRateLimitRepository.save({})", rateLimit);
        final int nbUpdatedElements = jdbcTemplate.update((Connection cnctn) -> {
            PreparedStatement stmt = cnctn.prepareStatement(UPDATE_SQL);
            stmt.setString(1, rateLimit.getKey());
            stmt.setLong(2, rateLimit.getCounter());
            stmt.setLong(3, rateLimit.getLastRequest());
            stmt.setLong(4, rateLimit.getResetTime());
            stmt.setLong(5, rateLimit.getCreatedAt());
            stmt.setLong(6, rateLimit.getUpdatedAt());
            stmt.setBoolean(7, rateLimit.isAsync());
            stmt.setString(8, rateLimit.getKey());
            return stmt;
        });
        if (nbUpdatedElements == 0) {
            jdbcTemplate.update((Connection cnctn) -> {
                PreparedStatement stmt = cnctn.prepareStatement(INSERT_SQL);
                stmt.setString(1, rateLimit.getKey());
                stmt.setLong(2, rateLimit.getCounter());
                stmt.setLong(3, rateLimit.getLastRequest());
                stmt.setLong(4, rateLimit.getResetTime());
                stmt.setLong(5, rateLimit.getCreatedAt());
                stmt.setLong(6, rateLimit.getUpdatedAt());
                stmt.setBoolean(7, rateLimit.isAsync());
                return stmt;
            });
        }
    }

    @Override
    public Iterator<RateLimit> findAsyncAfter(long timestamp) {
        final List<RateLimit> items = jdbcTemplate.query("select " + escapeReservedWord("key") + ", counter, last_request, reset_time, created_at, updated_at, async "
                        + " from ratelimit "
                        + " where async = true and updated_at > ?"
                , MAPPER
                , timestamp
        );
        return items.iterator();
    }
}