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

import static org.junit.Assert.assertEquals;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.JdbcDatabaseContainer;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
public class TableConstraintsTest extends AbstractRepositoryTest {

    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcDatabaseContainer jdbcDatabaseContainer;

    @Inject
    private Properties graviteeProperties;

    @Test
    public void shouldCheckEveryTableHasAPrimaryKey() {
        // Skip test if db is not MySql as the following query is specific to MySql
        // FIXME: Use JUnit assumptions instead when we upgrade to JUnit 5
        if (!jdbcDatabaseContainer.getDockerImageName().contains(DatabaseConfigurationEnum.MYSQL.getDockerImageName())) {
            return;
        }

        String prefix = graviteeProperties.getProperty("management.jdbc.prefix", "");

        List<String> tables = new ArrayList<>();
        final JdbcTemplate jt = new JdbcTemplate(dataSource);

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
                "  and tab.table_schema = 'test' -- <-- database name\n" +
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

        assertEquals("Following tables are missing a primary key:\n" + String.join("\n", tables), 0, tables.size());
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
