/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc;

import com.groupgti.shared.gravitee.repository.jdbc.configuration.JdbcManagementRepositoryConfiguration;
import com.groupgti.shared.gravitee.repository.jdbc.configuration.JdbcRateLimitRepositoryConfiguration;
import io.gravitee.repository.Repository;
import io.gravitee.repository.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class JdbcRepository implements Repository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcRepository.class);
    
    @Override
    public String type() {
        logger.debug("JdbcRepository.type()");
        return "jdbc";
    }

    @Override
    public Scope[] scopes() {
        logger.debug("JdbcRepository.scopes()");
        return new Scope [] {
                Scope.MANAGEMENT,
                Scope.RATE_LIMIT
        };
    }

    @Override
    public Class<?> configuration(Scope scope) {
        logger.debug("JdbcRepository.configuration({})", scope);
        switch (scope) {
            case MANAGEMENT:
                return JdbcManagementRepositoryConfiguration.class;
            case RATE_LIMIT:
                return JdbcRateLimitRepositoryConfiguration.class;

        }

        return null;
    }

}
