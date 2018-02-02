/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import java.sql.Types;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcSubscriptionRepository extends JdbcAbstractCrudRepository<Subscription, String> implements SubscriptionRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcSubscriptionRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Subscription.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Plan", Types.NVARCHAR, String.class)
            .addColumn("Application", Types.NVARCHAR, String.class)
            .addColumn("StartingAt", Types.TIMESTAMP, Date.class)
            .addColumn("EndingAt", Types.TIMESTAMP, Date.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("ProcessedAt", Types.TIMESTAMP, Date.class)
            .addColumn("ProcessedBy", Types.NVARCHAR, String.class)
            .addColumn("SubscribedBy", Types.NVARCHAR, String.class)
            .addColumn("Reason", Types.NVARCHAR, String.class)
            .addColumn("Status", Types.NVARCHAR, Subscription.Status.class)
            .build();    
    
    public JdbcSubscriptionRepository(DataSource dataSource) {
        super(dataSource, Subscription.class);
    }

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Subscription item) {
        return item.getId();
    }

    @Override
    public Set<Subscription> findByPlan(String planId) throws TechnicalException {

        logger.debug("JdbcSubscriptionRepository.findByPlan({})", planId);
        
        try {
            List<Subscription> items = jdbcTemplate.query("select * from Subscription where Plan = ?"
                    , getRowMapper()
                    , planId
            );
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find subscription by plan:", ex);
            throw new TechnicalException("Failed to find subscription by plan", ex);
        }
        
    }

    @Override
    public Set<Subscription> findByApplication(String application) throws TechnicalException {

        logger.debug("JdbcSubscriptionRepository.findByApplication({})", application);
        
        try {
            List<Subscription> items = jdbcTemplate.query("select * from Subscription where Application = ?"
                    , getRowMapper()
                    , application
            );
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find subscription by application:", ex);
            throw new TechnicalException("Failed to find subscription by application", ex);
        }
        
    }

    
    
}
