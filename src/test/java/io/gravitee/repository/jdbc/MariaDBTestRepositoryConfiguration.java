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
package io.gravitee.repository.jdbc;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import javax.inject.Inject;

import static java.lang.String.format;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Conditional(MariaDBCondition.class)
public class MariaDBTestRepositoryConfiguration extends AbstractJdbcTestRepositoryConfiguration {

    @Inject
    private DB embeddedMariaDB;

    @Override
    String getJdbcUrl() {
        final DBConfiguration config = embeddedMariaDB.getConfiguration();
        return format("jdbc:mariadb://localhost:%s/gravitee", config.getPort());
    }

    @Bean(destroyMethod = "stop")
    public DB embeddedMariaDB() throws ManagedProcessException {
        final DB db = DB.newEmbeddedDB(3306);
        db.start();
        db.createDB("gravitee");
        return db;
    }
}