/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.rate;

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcRateLimitRepository implements RateLimitRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcRateLimitRepository.class);

    private final JdbcTemplate jdbcTemplate;
    
    public JdbcRateLimitRepository(DataSource dataSource) {
        logger.debug("JdbcRateLimitRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static String buildInsertStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into RateLimit (");
        builder.append(" `Key` ");
        builder.append(" ,  Counter ");
        builder.append(" ,  LastRequest ");
        builder.append(" ,  ResetTime ");
        builder.append(" ,  CreatedAt ");
        builder.append(" ,  UpdatedAt ");
        builder.append(" ,  Async ");
        builder.append(" ) values ( ");
        builder.append(" ? ");
        builder.append(" ,  ? ");
        builder.append(" ,  ? ");
        builder.append(" ,  ? ");
        builder.append(" ,  ? ");
        builder.append(" ,  ? ");
        builder.append(" ,  ? ");
        builder.append(" )");
        builder.append(" ON DUPLICATE KEY UPDATE ");
        builder.append("    Counter = ? ");
        builder.append(" ,  LastRequest = ? ");
        builder.append(" ,  ResetTime = ? ");
        builder.append(" ,  CreatedAt = ? ");
        builder.append(" ,  UpdatedAt = ? ");
        builder.append(" ,  Async = ? ");
        return builder.toString();
    }
    
    private static final String INSERT_SQL = buildInsertStatement();
    
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

        logger.debug("JdbcRateLimitRepository.get({})", rateLimitKey);
        
        try {
            List<RateLimit> items = jdbcTemplate.query("select `Key` "
                    + " , Counter, LastRequest, ResetTime, CreatedAt, UpdatedAt, Async "
                    + " from RateLimit "
                    + " where `Key` = ?"
                    , MAPPER
                    , rateLimitKey
            );
            if (items.isEmpty()) {
                return new RateLimit(rateLimitKey);
            } else {
                return items.get(0);
            }
        } catch (Throwable ex) {
            logger.error("Failed to get rate limit:", ex);
            return null;
        }
        
    }

    @Override
    public void save(RateLimit rateLimit) {
        
        logger.debug("JdbcRateLimitRepository.save({})", rateLimit);
        try {
            jdbcTemplate.update((Connection cnctn) -> {
                PreparedStatement stmt = cnctn.prepareStatement(INSERT_SQL);
                stmt.setString(1, rateLimit.getKey());
                stmt.setLong(2, rateLimit.getCounter());
                stmt.setLong(3, rateLimit.getLastRequest());
                stmt.setLong(4, rateLimit.getResetTime());
                stmt.setLong(5, rateLimit.getCreatedAt());
                stmt.setLong(6, rateLimit.getUpdatedAt());
                stmt.setBoolean(7, rateLimit.isAsync());
                stmt.setLong(8, rateLimit.getCounter());
                stmt.setLong(9, rateLimit.getLastRequest());
                stmt.setLong(10, rateLimit.getResetTime());
                stmt.setLong(11, rateLimit.getCreatedAt());
                stmt.setLong(12, rateLimit.getUpdatedAt());
                stmt.setBoolean(13, rateLimit.isAsync());

                return stmt;
            });
        } catch (Throwable ex) {
            logger.error("Failed to save rate limit:", ex);
        }

    }

    @Override
    public Iterator<RateLimit> findAsyncAfter(long timestamp) {
        
        try {
            List<RateLimit> items = jdbcTemplate.query("select `Key`, Counter, LastRequest, ResetTime, CreatedAt, UpdatedAt, Async "
                    + " from RateLimit "
                    + " where Async = 1 and UpdatedAt > ?"
                    , MAPPER
                    , new Date(timestamp)
            );
            return items.iterator();
        } catch (Throwable ex) {
            logger.error("Failed to find async rate limits after {}:", timestamp, ex);
            return null;
        }
        
    }

}
