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
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class GuardrailRenameMigrationTest extends AbstractManagementRepositoryTest {

    private static final String PREFIX = "grd20_";
    private static final String CHANGELOGS = "liquibase/changelogs/v4_14_0/";

    @Inject
    private DataSource dataSource;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @AfterEach
    void cleanUp() {
        var jdbc = new JdbcTemplate(dataSource);
        for (String table : new String[] {
            "api_guardrails",
            "api_guardians",
            "guardrails",
            "guardians",
            "databasechangeloglock",
            "databasechangelog",
        }) {
            jdbc.execute("DROP TABLE IF EXISTS " + PREFIX + table);
        }
    }

    @Test
    void should_keep_the_stored_guardians_and_their_api_links_under_the_guardrail_names() throws Exception {
        var jdbc = new JdbcTemplate(dataSource);
        migrate("07_create_guardians_table.yml");
        migrate("09_make_guardians_environment_id_name_unique.yml");
        migrate("17_add_version_to_guardians.yml");
        migrate("18_create_api_guardians_table.yml");
        jdbc.update(
            "INSERT INTO " + PREFIX + "guardians (id, environment_id, name, phase, definition, lifecycle_state) VALUES (?, ?, ?, ?, ?, ?)",
            "guardian-1",
            "env-1",
            "PII",
            "REQUEST",
            "{}",
            "DEPLOYED"
        );
        jdbc.update("INSERT INTO " + PREFIX + "api_guardians (api_id, guardian_id) VALUES (?, ?)", "api-1", "guardian-1");

        migrate("20_rename_guardians_to_guardrails.yml");

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(jdbc.queryForList("SELECT name FROM " + PREFIX + "guardrails WHERE id = ?", String.class, "guardian-1"))
                .as("migrated guardrail")
                .containsExactly("PII");
            softly
                .assertThat(
                    jdbc.queryForList("SELECT guardrail_id FROM " + PREFIX + "api_guardrails WHERE api_id = ?", String.class, "api-1")
                )
                .as("migrated api link")
                .containsExactly("guardian-1");
        });
        assertThatThrownBy(() ->
            jdbc.update(
                "INSERT INTO " +
                    PREFIX +
                    "guardrails (id, environment_id, name, phase, definition, lifecycle_state) VALUES (?, ?, ?, ?, ?, ?)",
                "guardrail-2",
                "env-1",
                "PII",
                "REQUEST",
                "{}",
                "DEPLOYED"
            )
        )
            .as("environment_id and name stay unique")
            .hasRootCauseInstanceOf(SQLException.class);
    }

    private void migrate(String changelog) throws Exception {
        var previousPrefix = System.getProperty("gravitee_prefix");
        System.setProperty("gravitee_prefix", PREFIX);
        try (var connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDatabaseChangeLogTableName(PREFIX + "databasechangelog");
            database.setDatabaseChangeLogLockTableName(PREFIX + "databasechangeloglock");
            try (var liquibase = new Liquibase(CHANGELOGS + changelog, new ClassLoaderResourceAccessor(), database)) {
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
