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

import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.StreamSupport;
import liquibase.database.core.MariaDBDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.CreateDatabaseChangeLogTableGenerator;
import liquibase.statement.core.CreateDatabaseChangeLogTableStatement;
import liquibase.structure.DatabaseObject;
import org.junit.jupiter.api.Test;

class MySqlChangeLogTableWithPrimaryKeyGeneratorTest {

    private final MySqlChangeLogTableWithPrimaryKeyGenerator generator = new MySqlChangeLogTableWithPrimaryKeyGenerator();

    @Test
    void supports_MySQLDatabase() {
        assertThat(generator.supports(new CreateDatabaseChangeLogTableStatement(), new MySQLDatabase())).isTrue();
    }

    @Test
    void supports_MariaDBDatabase() {
        assertThat(generator.supports(new CreateDatabaseChangeLogTableStatement(), new MariaDBDatabase())).isTrue();
    }

    @Test
    void does_not_support_PostgresDatabase() {
        assertThat(generator.supports(new CreateDatabaseChangeLogTableStatement(), new PostgresDatabase())).isFalse();
    }

    @Test
    void priority_outranks_default_generator() {
        assertThat(generator.getPriority()).isGreaterThan(new CreateDatabaseChangeLogTableGenerator().getPriority());
    }

    @Test
    void splices_named_primary_key_into_create_table() {
        Sql[] result = MySqlChangeLogTableWithPrimaryKeyGenerator.spliceWithPrimaryKey(
            new Sql[] { sql("CREATE TABLE DATABASECHANGELOG (ID VARCHAR(255), AUTHOR VARCHAR(255), FILENAME VARCHAR(255))") },
            "pk_DATABASECHANGELOG"
        );
        assertThat(result).hasSize(1);
        assertThat(result[0].toSql()).isEqualTo(
            "CREATE TABLE DATABASECHANGELOG (ID VARCHAR(255), AUTHOR VARCHAR(255), FILENAME VARCHAR(255), CONSTRAINT pk_DATABASECHANGELOG PRIMARY KEY (ID, AUTHOR, FILENAME))"
        );
    }

    @Test
    void honours_configured_constraint_name() {
        Sql[] result = MySqlChangeLogTableWithPrimaryKeyGenerator.spliceWithPrimaryKey(
            new Sql[] { sql("CREATE TABLE acme_databasechangelog (ID VARCHAR(255))") },
            "pk_acme_databasechangelog"
        );
        assertThat(result[0].toSql()).contains("CONSTRAINT pk_acme_databasechangelog PRIMARY KEY (ID, AUTHOR, FILENAME)");
    }

    @Test
    void tolerates_leading_whitespace_in_create_table() {
        Sql[] result = MySqlChangeLogTableWithPrimaryKeyGenerator.spliceWithPrimaryKey(
            new Sql[] { sql("\n  CREATE TABLE DATABASECHANGELOG (ID VARCHAR(255))") },
            "pk_DATABASECHANGELOG"
        );
        assertThat(result[0].toSql()).contains("CONSTRAINT pk_DATABASECHANGELOG PRIMARY KEY (ID, AUTHOR, FILENAME)");
    }

    @Test
    void passes_through_non_create_table_statements_when_a_create_table_is_present() {
        Sql[] result = MySqlChangeLogTableWithPrimaryKeyGenerator.spliceWithPrimaryKey(
            new Sql[] { sql("SET sql_mode = ''"), sql("CREATE TABLE DATABASECHANGELOG (ID VARCHAR(255))") },
            "pk_DATABASECHANGELOG"
        );
        assertThat(result).hasSize(2);
        assertThat(result[0].toSql()).isEqualTo("SET sql_mode = ''");
        assertThat(result[1].toSql()).contains("CONSTRAINT pk_DATABASECHANGELOG PRIMARY KEY (ID, AUTHOR, FILENAME)");
    }

    @Test
    void throws_when_no_create_table_is_present() {
        Sql[] input = { sql("INSERT INTO DATABASECHANGELOG VALUES (1, 'a', 'b')") };
        assertThatThrownBy(() -> MySqlChangeLogTableWithPrimaryKeyGenerator.spliceWithPrimaryKey(input, "pk_DATABASECHANGELOG"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Expected a CREATE TABLE statement");
    }

    @Test
    void throws_when_create_table_has_no_balanced_parens() {
        Sql[] input = { sql("CREATE TABLE DATABASECHANGELOG ID VARCHAR(255") };
        assertThatThrownBy(() -> MySqlChangeLogTableWithPrimaryKeyGenerator.spliceWithPrimaryKey(input, "pk_DATABASECHANGELOG"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no balanced column-list parentheses");
    }

    @Test
    void splices_into_column_list_close_when_trailing_options_contain_parens() {
        // Hypothetical future upstream output with a trailing partition clause whose own
        // parens would fool a naive lastIndexOf(')'). The splice has to walk paren-depth and
        // splice INSIDE the column list, before any trailing table options.
        Sql[] result = MySqlChangeLogTableWithPrimaryKeyGenerator.spliceWithPrimaryKey(
            new Sql[] {
                sql(
                    "CREATE TABLE DATABASECHANGELOG (ID VARCHAR(255), AUTHOR VARCHAR(255), FILENAME VARCHAR(255)) ENGINE=InnoDB PARTITION BY HASH(id) PARTITIONS 4"
                ),
            },
            "pk_DATABASECHANGELOG"
        );
        assertThat(result[0].toSql()).isEqualTo(
            "CREATE TABLE DATABASECHANGELOG (ID VARCHAR(255), AUTHOR VARCHAR(255), FILENAME VARCHAR(255), CONSTRAINT pk_DATABASECHANGELOG PRIMARY KEY (ID, AUTHOR, FILENAME)) ENGINE=InnoDB PARTITION BY HASH(id) PARTITIONS 4"
        );
    }

    @Test
    void throws_when_upstream_already_has_a_primary_key_clause() {
        // If a future Liquibase release ships its own PRIMARY KEY clause inline, we must NOT
        // splice another — that would emit two PK clauses and a duplicate-key DDL error at
        // execution time. Throw with the offending SQL so the breakage surfaces clearly.
        Sql[] input = {
            sql("CREATE TABLE DATABASECHANGELOG (ID VARCHAR(255), AUTHOR VARCHAR(255), FILENAME VARCHAR(255), PRIMARY KEY (ID))"),
        };
        assertThatThrownBy(() -> MySqlChangeLogTableWithPrimaryKeyGenerator.spliceWithPrimaryKey(input, "pk_DATABASECHANGELOG"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already contains a PRIMARY KEY");
    }

    @Test
    void registered_via_service_loader() {
        // META-INF/services/liquibase.sqlgenerator.SqlGenerator is the only thing tying our
        // generator into Liquibase's dispatch — if the file goes missing or gets misnamed
        // during a Maven packaging change, the whole strict-PK splice silently disappears.
        // ServiceLoader.load returns the registered SqlGenerators on the test classpath so
        // we can assert ours is in the list at unit-test time.
        boolean found = StreamSupport.stream(ServiceLoader.load(SqlGenerator.class).spliterator(), false).anyMatch(
            MySqlChangeLogTableWithPrimaryKeyGenerator.class::isInstance
        );
        assertThat(found)
            .as("MySqlChangeLogTableWithPrimaryKeyGenerator must be registered via META-INF/services/liquibase.sqlgenerator.SqlGenerator")
            .isTrue();
    }

    /**
     * End-to-end smoke against the real Liquibase pipeline: drives the generator with the actual
     * upstream {@link CreateDatabaseChangeLogTableStatement} on a real {@link MySQLDatabase}, so a
     * future Liquibase contract change (renamed columns, removed columns, additional partition
     * clauses, etc.) is caught here before it surfaces as ER_3750 in CI. The hand-crafted unit
     * tests above only cover the splice arithmetic — this one pins the splice contract against
     * what Liquibase actually produces.
     */
    @Test
    void generateSql_splices_named_pk_into_real_upstream_output() {
        SortedSet<SqlGenerator<CreateDatabaseChangeLogTableStatement>> emptyChain = new TreeSet<>();
        Sql[] generated = generator.generateSql(
            new CreateDatabaseChangeLogTableStatement(),
            new MySQLDatabase(),
            new SqlGeneratorChain<>(emptyChain)
        );

        assertThat(generated).isNotEmpty();
        String sql = generated[0].toSql();
        assertThat(sql).startsWith("CREATE TABLE");
        // Constraint name derives from Database#getDatabaseChangeLogTableName(); MySQL defaults
        // to lowercase databasechangelog.
        assertThat(sql).contains("CONSTRAINT pk_databasechangelog PRIMARY KEY (ID, AUTHOR, FILENAME)");
        // Spot-check that the columns Liquibase ships with survive the splice — guards against a
        // future upstream change that adds/renames columns and silently breaks our retrofit.
        assertThat(sql)
            .contains("ID")
            .contains("AUTHOR")
            .contains("FILENAME")
            .contains("DATEEXECUTED")
            .contains("ORDEREXECUTED")
            .contains("MD5SUM")
            .contains("DEPLOYMENT_ID");
    }

    private static Sql sql(String text) {
        return new UnparsedSql(text, ";", new DatabaseObject[0]);
    }
}
