/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.gravitee.repository.jdbc;

import io.gravitee.repository.config.TestRepositoryInitializer;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author njt
 */
@Component
public class JdbcTestRepositoryInitializer implements TestRepositoryInitializer {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcTestRepositoryInitializer.class);

    private final DataSource dataSource;

    private static final List<String> tablesToTruncate = Arrays.asList(
            "Api",
            "ApiKey",
            "ApiGroup",
            "ApiLabel",
            "ApiTag",
            "ApiView",
            "Application",
            "ApplicationGroup",
            "Audit",
            "AuditProperty",
            "Event",
            "EventProperty",
            "Group",
            "GroupEventRule",
            "GroupAdministrator",
            "Membership",
            "MembershipRole",
            "Metadata",
            "Page",
            "PageExcludedGroup",
            "Plan",
            "PlanApi",
            "PlanCharacteristic",
            "PlanExcludedGroup",
            "RateLimit",
            "Rating",
            "RatingAnswer",
            "Role",
            "RolePermission",
            "Subscription",
            "Tag",
            "Tenant",
            "User",
            "UserRole",
            "View"
    );
    
    private static final List<String> tablesToDrop = concatenate(tablesToTruncate
            , Arrays.asList(
            "ClientMessageLog",
            "DATABASECHANGELOG",
            "DATABASECHANGELOGLOCK"
    ));
    
    private static <T> List<T> concatenate(List<T> first, List<T> second) {
        List result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    @Autowired
    public JdbcTestRepositoryInitializer(DataSource dataSource) {
        logger.debug("Constructed");
        this.dataSource = dataSource;
        
        JdbcTemplate jt = new JdbcTemplate(dataSource);
        for (String table : tablesToDrop) {
            logger.debug("Dropping {}", table);
            jt.execute("drop table if exists `" + table + "`");
        }
    }

    private Liquibase createLiquibase(Connection conn) throws LiquibaseException {
        Liquibase liquibase = new Liquibase("liquibase/master.yml",
                 new ClassLoaderResourceAccessor(this.getClass().getClassLoader()), new JdbcConnection(conn));
        liquibase.setIgnoreClasspathPrefix(true);

        return liquibase;
    }

    private void performUpdate(Liquibase liquibase) throws LiquibaseException {
        liquibase.update((Contexts) null);
    }

    private void runLiquibase(DataSource dataSource) {
        logger.debug("Running LiquiBase on {}", dataSource);
        try (Connection conn = dataSource.getConnection()) {
            Liquibase liquibase = createLiquibase(conn);
            performUpdate(liquibase);
        } catch (Exception ex) {
            logger.error("Failed to set up database: ", ex);
        }
    }

    @Override
    public void setUp() {
        logger.debug("setUp");
        runLiquibase(dataSource);
    }

    @Override
    public void tearDown() {
        logger.debug("tearDown");
        
        JdbcTemplate jt = new JdbcTemplate(dataSource);
        jt.execute((Connection con) -> {
            con.nativeSQL("SET REFERENTIAL_INTEGRITY FALSE");
            for (String table : tablesToTruncate) {
                jt.execute("truncate table `" + table + '`');
            }
            con.nativeSQL("SET REFERENTIAL_INTEGRITY TRUE");
            return null;
        });
    }

}
