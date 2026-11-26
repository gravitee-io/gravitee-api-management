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
import java.sql.Timestamp;
import java.time.Instant;
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

class GammaMetricDefinitionRawKeyMigrationTest extends AbstractManagementRepositoryTest {

    private static final String PREFIX = "obs220_";
    private static final String TABLE = PREFIX + "gamma_metric_definitions";
    private static final String CREATE = "liquibase/changelogs/v4_14_0/02_create_gamma_metric_definitions_table.yml";
    private static final String WIDEN = "liquibase/changelogs/v4_14_0/22_widen_gamma_metric_definitions_raw_key.yml";
    /** 'custom.' and 256 UTF-16 units, an emoji (two units, four bytes in UTF-8) among them. */
    private static final String LONGEST_CUSTOM_KEY = "custom." + "a".repeat(254) + "🚀";

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
    void should_keep_existing_rows_and_the_not_null_and_take_the_longest_custom_key_on_repeat_upgrade() throws Exception {
        var jdbc = new JdbcTemplate(dataSource);
        migrate(CREATE);
        insert(jdbc, "existing", "double_checkout_basket-value", "Basket value");

        migrate(WIDEN);
        migrate(WIDEN);

        assertThat(jdbc.queryForObject("SELECT raw_key FROM " + TABLE + " WHERE id = ?", String.class, "existing")).isEqualTo(
            "double_checkout_basket-value"
        );
        assertThatThrownBy(() -> insert(jdbc, "null-key", null, null)).hasRootCauseInstanceOf(SQLException.class);
        insert(jdbc, "widest", "custom." + "a".repeat(256), null);
        insert(jdbc, "longest", LONGEST_CUSTOM_KEY, "Rocket 🚀");
        assertThat(jdbc.queryForObject("SELECT raw_key FROM " + TABLE + " WHERE id = ?", String.class, "longest")).isEqualTo(
            LONGEST_CUSTOM_KEY
        );
        assertThat(jdbc.queryForObject("SELECT label FROM " + TABLE + " WHERE id = ?", String.class, "longest")).isEqualTo("Rocket 🚀");
        assertThatThrownBy(() -> insert(jdbc, "too-long", "custom." + "a".repeat(257), null)).hasRootCauseInstanceOf(SQLException.class);
    }

    private static void insert(JdbcTemplate jdbc, String id, String rawKey, String label) {
        var now = Timestamp.from(Instant.parse("2026-11-26T00:00:00Z"));
        jdbc.update(
            "INSERT INTO " +
                TABLE +
                " (id, environment_id, raw_key, raw_key_hash, label, exposed, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            id,
            "DEFAULT",
            rawKey,
            id,
            label,
            true,
            now,
            now
        );
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
