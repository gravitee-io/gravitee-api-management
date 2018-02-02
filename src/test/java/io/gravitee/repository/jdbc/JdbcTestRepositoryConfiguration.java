/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.gravitee.repository.jdbc;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author njt
 */
@ComponentScan("com.groupgti.shared.gravitee.repository.jdbc")
@Configuration
public class JdbcTestRepositoryConfiguration {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcTestRepositoryConfiguration.class);

    public JdbcTestRepositoryConfiguration() {
        logger.debug("Constructed");
    }
    
    @Bean
    public DataSource dataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;DB_CLOSE_ON_EXIT=FALSE;IGNORECASE=TRUE;MODE=MySQL");
//        MysqlDataSource ds = new MysqlDataSource();
//        ds.setURL("jdbc:mysql://srv-wall-esbtest3/gravitee_build");
//        ds.setUser("gravyTree");
//        ds.setPassword("TopS3cret");
        return ds;
    }

}
