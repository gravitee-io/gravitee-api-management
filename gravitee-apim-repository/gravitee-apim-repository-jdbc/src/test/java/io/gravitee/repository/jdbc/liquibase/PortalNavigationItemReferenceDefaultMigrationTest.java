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
package io.gravitee.repository.jdbc.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.sql.SQLException;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PortalNavigationItemReferenceDefaultMigrationTest extends AbstractManagementRepositoryTest {

    private static final String PREFIX = "portal345_";
    private static final String TABLE = PREFIX + "portal_navigation_items";
    private static final String CHANGE = "liquibase/changelogs/v4_14_0/16_drop_default_reference_id_from_portal_navigation_items.yml";

    @Inject
    private DataSource dataSource;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @AfterEach
    void cleanUp() {
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS " + TABLE);
        jdbc.execute("DROP TABLE IF EXISTS " + PREFIX + "databasechangeloglock");
        jdbc.execute("DROP TABLE IF EXISTS " + PREFIX + "databasechangelog");
    }

    @Test
    void should_remove_only_the_default_and_keep_old_references_and_not_null_on_repeat_upgrade() throws Exception {
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE " + TABLE + " (id VARCHAR(64) PRIMARY KEY)");
        migrate("liquibase/changelogs/v4_13_0/14_add_reference_columns_to_portal_navigation_items.yml");
        jdbc.update("INSERT INTO " + TABLE + " (id) VALUES (?)", "legacy");

        migrate(CHANGE);
        migrate(CHANGE);

        assertThat(jdbc.queryForObject("SELECT reference_id FROM " + TABLE + " WHERE id = ?", String.class, "legacy")).isEqualTo(
            "00000000-0000-0000-0000-000000000000"
        );
        assertThatThrownBy(() -> jdbc.update("INSERT INTO " + TABLE + " (id) VALUES (?)", "missing")).hasRootCauseInstanceOf(
            SQLException.class
        );
        assertThatThrownBy(() ->
            jdbc.update("INSERT INTO " + TABLE + " (id, reference_id) VALUES (?, ?)", "null", null)
        ).hasRootCauseInstanceOf(SQLException.class);
        jdbc.update("INSERT INTO " + TABLE + " (id, reference_type, reference_id) VALUES (?, ?, ?)", "portal", "PORTAL", "portal-id");
        jdbc.update("INSERT INTO " + TABLE + " (id, reference_type, reference_id) VALUES (?, ?, ?)", "api", "API", "api-id");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + TABLE, Integer.class)).isEqualTo(3);
    }

    private void migrate(String changelog) throws Exception {
        var previousPrefix = System.getProperty("gravitee_prefix");
        System.setProperty("gravitee_prefix", PREFIX);
        try (var connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDatabaseChangeLogTableName(PREFIX + "databasechangelog");
            database.setDatabaseChangeLogLockTableName(PREFIX + "databasechangeloglock");
            try (var liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)) {
                liquibase.setChangeLogParameter("gravitee_prefix", PREFIX);
                liquibase.update(new Contexts());
            }
        } finally {
            if (previousPrefix == null) {
                System.clearProperty("gravitee_prefix");
            } else {
                System.setProperty("gravitee_prefix", previousPrefix);
            }
        }
    }
}
