/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.Api;
import java.sql.Types;
import java.util.Date;
import io.gravitee.repository.management.model.ApiKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcApiKeyRepository extends JdbcAbstractCrudRepository<ApiKey, String> implements ApiKeyRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcApiKeyRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(ApiKey.class, "Key")
            .addColumn("Key", Types.NVARCHAR, String.class)
            .addColumn("Subscription", Types.NVARCHAR, String.class)
            .addColumn("Application", Types.NVARCHAR, String.class)
            .addColumn("Plan", Types.NVARCHAR, String.class)
            .addColumn("ExpireAt", Types.TIMESTAMP, Date.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("Revoked", Types.BIT, boolean.class)
            .addColumn("RevokedAt", Types.TIMESTAMP, Date.class)
            .build();    
    
    @Autowired
    public JdbcApiKeyRepository(DataSource dataSource) {
        super(dataSource, ApiKey.class);
        logger.debug("JdbcApiKeyRepository({})", dataSource);
    }

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(ApiKey item) {
        return item.getKey();
    }

    @Override
    public List<ApiKey> findByCriteria(ApiKeyCriteria akc) throws TechnicalException {

        logger.debug("JdbcApiKeyRepository.findByCriteria({})", akc);

        try {
            List<Object> args = new ArrayList<>();
            
            StringBuilder query = new StringBuilder();
            query.append("select * from ApiKey ");
            boolean first = true;
            if (!akc.isIncludeRevoked()) {
                first = addClause(first, query);
                query.append(" ( Revoked = 0 ) ");
            }
            if ((akc.getPlans() != null) && !akc.getPlans().isEmpty()) {
                first = addClause(first, query);
                query.append(" ( Plan in ( ");
                boolean subFirst = true;
                for (String plan : akc.getPlans()) {
                    if (!subFirst) {
                        query.append(", ");
                    }
                    subFirst = false;
                    query.append(" ?");
                    args.add(plan);
                }
                query.append(" ) ) ");
            }
            if (akc.getFrom() > 0) {
                first = addClause(first, query);
                query.append(" ( UpdatedAt >= ? ) ");
                args.add(new Date(akc.getFrom()));
            }
            if (akc.getTo() > 0) {
                first = addClause(first, query);
                query.append(" ( UpdatedAt <= ? ) ");
                args.add(new Date(akc.getTo()));
            }
            
            
            return jdbcTemplate.query(query.toString()
                    , args.toArray()
                    , ORM.getRowMapper()
            );
            
        } catch (Throwable ex) {
            logger.error("Failed to find api keys by criteria:", ex);
            throw new TechnicalException("Failed to find api keys by criteria", ex);
        }
        
    }

    private boolean addClause(boolean first, StringBuilder query) {
        if (first) {
            query.append(" where ");
        } else {
            query.append(" and ");
        }
        return false;
    }
    
    @Override
    public Set<ApiKey> findBySubscription(String subscription) throws TechnicalException {

        logger.debug("JdbcApiKeyRepository.findBySubscription({})", subscription);

        try {
            List<ApiKey> apiKeys = jdbcTemplate.query("select * from ApiKey where Subscription = ?"
                    , ORM.getRowMapper()
                    , subscription
            );
            return new HashSet<>(apiKeys);
        } catch (Throwable ex) {
            logger.error("Failed to find api keys by subscription:", ex);
            throw new TechnicalException("Failed to find api keys by subscription", ex);
        }
        
    }

    @Override
    public Set<ApiKey> findByPlan(String plan) throws TechnicalException {

        logger.debug("JdbcApiKeyRepository.findByPlan({})", plan);
        try {
            List<ApiKey> items = jdbcTemplate.query("select * from ApiKey where Plan = ?"
                    , getOrm().getRowMapper()
                    , plan
            );
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find api keys by plan:", ex);
            throw new TechnicalException("Failed to find api keys by plan", ex);
        }

    }

}
