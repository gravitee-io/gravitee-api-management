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
package io.gravitee.repository.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.JdbcDatabaseContainer;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class TableConstraintsTest extends AbstractRepositoryTest {

    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcDatabaseContainer jdbcDatabaseContainer;

    @Inject
    private Properties graviteeProperties;

    @Test
    public void shouldCheckEveryTableHasAPrimaryKey() {
        // The information_schema query below is specific to MySQL/MariaDB. Use assumeTrue so the
        // test reports as SKIPPED on other engines rather than vacuously passing.
        String image = jdbcDatabaseContainer.getDockerImageName();
        assumeTrue(
            image.contains(DatabaseConfigurationEnum.MYSQL.getDockerImageName()) ||
                image.contains(DatabaseConfigurationEnum.MARIADB.getDockerImageName()),
            "TableConstraintsTest only runs on MySQL / MariaDB"
        );

        String prefix = graviteeProperties.getProperty("management.jdbc.prefix", "");

        List<String> tables = new ArrayList<>();
        final JdbcTemplate jt = new JdbcTemplate(dataSource);

        // Sanity precondition: the schema bootstrap actually created the core APIM tables. A
        // failed Liquibase setup that left only the databasechangelog + lock tables would have
        // tableCount > 0 but be missing every meaningful table — checking for representative
        // core tables (apis, subscriptions, plans, users) closes that loophole, since each is
        // created in v1_14_0 / v1_15_0 within the first dozen changesets.
        Integer coreTableCount = jt.queryForObject(
            "select count(*) from information_schema.tables " +
                " where table_schema = (select database())" +
                "   and table_type = 'BASE TABLE'" +
                "   and table_name in (?, ?, ?, ?)",
            Integer.class,
            prefix + "apis",
            prefix + "subscriptions",
            prefix + "plans",
            prefix + "users"
        );
        assertEquals(
            Integer.valueOf(4),
            coreTableCount,
            "Bootstrap is missing one or more core tables (apis, subscriptions, plans, users) — Liquibase setup likely failed"
        );

        // (select database()) reads the connection's current database so the test does not depend
        // on the testcontainer's schema name being literally 'test'.
        jt.query(
            "select tab.table_schema as database_name,\n" +
                "       tab.table_name\n" +
                "from information_schema.tables tab\n" +
                "         left join information_schema.table_constraints tco\n" +
                "                   on tab.table_schema = tco.table_schema\n" +
                "                       and tab.table_name = tco.table_name\n" +
                "                       and tco.constraint_type = 'PRIMARY KEY'\n" +
                "where tco.constraint_type is null\n" +
                "  and tab.table_schema not in ('mysql', 'information_schema',\n" +
                "                               'performance_schema', 'sys')\n" +
                "  and tab.table_type = 'BASE TABLE'\n" +
                "  and tab.table_schema = (select database())\n" +
                "order by tab.table_schema,\n" +
                "         tab.table_name;",
            rs -> {
                if (rs.wasNull()) {
                    return;
                }
                do {
                    tables.add(rs.getString("table_name").replace(prefix, ""));
                } while (rs.next());
            }
        );

        assertEquals(0, tables.size(), "Following tables are missing a primary key:\n" + String.join("\n", tables));
    }

    @Override
    protected String getTestCasesPath() {
        return "";
    }

    @Override
    protected String getModelPackage() {
        return "";
    }

    @Override
    protected void createModel(Object object) throws TechnicalException {}
}
