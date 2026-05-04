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

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import liquibase.database.Database;
import liquibase.database.core.MariaDBDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.CreateDatabaseChangeLogTableGenerator;
import liquibase.statement.core.CreateDatabaseChangeLogTableStatement;
import liquibase.structure.DatabaseObject;

/**
 * Wraps Liquibase's default {@link CreateDatabaseChangeLogTableGenerator} and injects a named
 * primary key into the generated {@code CREATE TABLE DATABASECHANGELOG} statement on MySQL/MariaDB.
 *
 * <p>As of the currently-pinned Liquibase, the changelog tracking table is created without a
 * primary key, which the MySQL {@code sql_require_primary_key=ON} setting (used by HeatWave /
 * Group Replication / strict primary-key deployments) rejects with error 3750 before any change
 * set can run. Instead of pre-creating the table with a hard-coded schema (which would drift from
 * Liquibase's own DDL when future Liquibase versions add columns), this generator delegates to the
 * default implementation and splices a {@code CONSTRAINT pk_<table> PRIMARY KEY (ID, AUTHOR,
 * FILENAME)} clause — the same triplet Liquibase uses to identify a changeset — into whatever SQL
 * Liquibase produces. The constraint name mirrors the {@code v3_15_20} legacy retrofit so fresh
 * deploys and upgraded deploys end up with the same PK name. New columns added by Liquibase
 * upstream are inherited automatically.
 *
 * <p>Failure modes are fail-fast: if Liquibase ever emits no SQL or a shape we cannot recognise,
 * an {@link IllegalStateException} is thrown at splice time so the underlying upstream contract
 * change surfaces clearly, rather than letting a silent no-op turn into the much more obscure
 * MySQL error 3750 later in the migration.
 *
 * <p>Registered via {@code META-INF/services/liquibase.sqlgenerator.SqlGenerator}. The companion
 * {@link liquibase.sqlgenerator.core.CreateDatabaseChangeLogLockTableGenerator} already emits a
 * {@code PRIMARY KEY (ID)} clause out of the box, so it does not need a similar wrapper.
 */
public class MySqlChangeLogTableWithPrimaryKeyGenerator extends CreateDatabaseChangeLogTableGenerator {

    @Override
    public int getPriority() {
        // Higher than the default generator so Liquibase picks this one for MySQL/MariaDB.
        return super.getPriority() + 1;
    }

    @Override
    public boolean supports(CreateDatabaseChangeLogTableStatement statement, Database database) {
        return database instanceof MySQLDatabase || database instanceof MariaDBDatabase;
    }

    @Override
    public Sql[] generateSql(CreateDatabaseChangeLogTableStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        Sql[] generated = super.generateSql(statement, database, sqlGeneratorChain);
        if (generated == null || generated.length == 0) {
            throw new IllegalStateException(
                "Liquibase " +
                    CreateDatabaseChangeLogTableGenerator.class.getName() +
                    " produced no SQL for the changelog table — strict-PK splicing cannot proceed."
            );
        }
        return spliceWithPrimaryKey(generated, "pk_" + database.getDatabaseChangeLogTableName());
    }

    /**
     * Splices a named {@code PRIMARY KEY (ID, AUTHOR, FILENAME)} clause into the unique {@code
     * CREATE TABLE} statement found in {@code generated}. Other statements pass through unchanged.
     * Throws {@link IllegalStateException} if no statement matches the {@code CREATE TABLE} prefix
     * — this means Liquibase changed its DDL shape and our splice assumptions are stale.
     */
    static Sql[] spliceWithPrimaryKey(Sql[] generated, String constraintName) {
        Sql[] result = new Sql[generated.length];
        boolean spliced = false;
        for (int i = 0; i < generated.length; i++) {
            Sql original = generated[i];
            String sql = original.toSql();
            if (looksLikeCreateTable(sql)) {
                result[i] = spliceOne(original, sql, constraintName);
                spliced = true;
            } else {
                result[i] = original;
            }
        }
        if (!spliced) {
            String dump = Arrays.stream(generated).map(Sql::toSql).collect(Collectors.joining("\n  "));
            throw new IllegalStateException(
                "Expected a CREATE TABLE statement to splice a PRIMARY KEY into; found none. " + "Upstream SQL was:\n  " + dump
            );
        }
        return result;
    }

    private static boolean looksLikeCreateTable(String sql) {
        // Locale.ROOT so the dotted-i / dotless-i fold doesn't bite us in a Turkish locale JVM.
        return sql != null && sql.trim().toUpperCase(Locale.ROOT).startsWith("CREATE TABLE");
    }

    private static Sql spliceOne(Sql original, String sql, String constraintName) {
        // Defensive: refuse to splice if upstream Liquibase ever starts emitting its own
        // PRIMARY KEY clause inside the column list — a future contract change of that shape
        // would otherwise silently produce two PK clauses and a duplicate-key DDL error at
        // execution time. Better to throw here with the offending SQL in the message.
        if (sql.toUpperCase(Locale.ROOT).contains("PRIMARY KEY")) {
            throw new IllegalStateException(
                "Upstream CREATE TABLE already contains a PRIMARY KEY clause; refusing to splice another. " +
                    "This means Liquibase changed its DDL shape and the splice is no longer needed (or needs to merge). " +
                    "Offending SQL: " +
                    sql
            );
        }
        int columnListClose = findColumnListClose(sql);
        if (columnListClose < 0) {
            throw new IllegalStateException("CREATE TABLE has no balanced column-list parentheses to splice into: " + sql);
        }
        String pkClause = ", CONSTRAINT " + constraintName + " PRIMARY KEY (ID, AUTHOR, FILENAME)";
        String withPk = sql.substring(0, columnListClose) + pkClause + sql.substring(columnListClose);
        DatabaseObject[] affected = original.getAffectedDatabaseObjects().toArray(new DatabaseObject[0]);
        return new UnparsedSql(withPk, original.getEndDelimiter(), affected);
    }

    /**
     * Returns the index of the {@code )} that closes the column list of {@code sql}, or {@code -1}
     * if no balanced opener is found. Walks paren-depth from the first {@code (} after
     * {@code CREATE TABLE …} so trailing table options that themselves contain parens — e.g.
     * {@code ENGINE=InnoDB PARTITION BY HASH(id) PARTITIONS 4} — don't fool the splice into
     * injecting outside the column list.
     */
    static int findColumnListClose(String sql) {
        int open = sql.indexOf('(');
        if (open < 0) {
            return -1;
        }
        int depth = 0;
        for (int i = open; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
