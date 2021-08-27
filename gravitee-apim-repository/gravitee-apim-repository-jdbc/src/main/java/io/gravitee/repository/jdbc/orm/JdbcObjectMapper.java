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
package io.gravitee.repository.jdbc.orm;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static io.gravitee.repository.jdbc.orm.JdbcColumn.getDBName;
import static java.lang.Byte.parseByte;
import static java.util.Collections.emptyList;
import static org.springframework.util.StringUtils.hasText;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author njt
 */
public class JdbcObjectMapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcObjectMapper.class);

    private final Constructor<T> constructor;
    private final List<JdbcColumn> columns;
    private final String idColumn;
    private final String insertSql;
    private final String updateSql;
    private final String selectByIdSql;
    private final String selectAllSql;
    private final String deleteSql;
    private final RowMapper<T> rowMapper;
    private final String tableName;

    private static class BatchStringSetter implements BatchPreparedStatementSetter {

        private final Object parentId;
        private final List<String> values;

        BatchStringSetter(Object parentId, Collection<String> values) {
            this.parentId = parentId;
            if (values instanceof List) {
                this.values = (List) values;
            } else {
                this.values = new ArrayList<>(values);
            }
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            ps.setObject(1, parentId);
            ps.setString(2, values.get(i));
        }

        @Override
        public int getBatchSize() {
            return values.size();
        }
    }

    private class Psc implements PreparedStatementCreator {

        private final String sql;
        private final T item;
        private final Object[] ids;

        Psc(String sql, T item, Object... ids) {
            this.sql = sql;
            this.item = item;
            this.ids = ids;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection cnctn) throws SQLException {
            LOGGER.trace("SQL: {}", sql);
            LOGGER.trace("Item: {}", item);
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = setStatementValues(stmt, item, 1);

            for (Object id : ids) {
                stmt.setObject(idx++, id);
            }
            return stmt;
        }
    }

    private class Rm implements RowMapper<T> {

        @Override
        public T mapRow(ResultSet rs, int i) {
            try {
                T item = constructor.newInstance();
                setFromResultSet(item, rs);
                return item;
            } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException ex) {
                LOGGER.error("Failed to construct {}", tableName);
                throw new IllegalStateException("Failed to construct " + tableName, ex);
            }
        }
    }

    public static class Builder<T> {

        private Class<T> clazz;
        private String idColumn;
        private String tableName;
        private String updateSql;
        private List<JdbcColumn> columns = new ArrayList<>();

        private Builder(final Class<T> value, final String tableName, String idColumn) {
            this.clazz = value;
            this.tableName = tableName;
            this.idColumn = idColumn;
        }

        public Builder<T> updateSql(String updateSql) {
            this.updateSql = updateSql;
            return this;
        }

        public Builder<T> addColumn(String name, int jdbcType, Class fieldType) {
            this.columns.add(new JdbcColumn(name, jdbcType, clazz, fieldType));
            return this;
        }

        public JdbcObjectMapper<T> build() {
            return new JdbcObjectMapper<>(clazz, idColumn, columns, updateSql, tableName);
        }
    }

    public static <T> JdbcObjectMapper.Builder<T> builder(final Class<T> clazz, final String tableName) {
        return builder(clazz, tableName, null);
    }

    public static <T> JdbcObjectMapper.Builder<T> builder(final Class<T> clazz, final String tableName, final String idColumn) {
        return new JdbcObjectMapper.Builder<>(clazz, tableName, idColumn);
    }

    private JdbcObjectMapper(
        final Class<T> clazz,
        final String idColumn,
        final List<JdbcColumn> columns,
        final String updateSql,
        final String tableName
    ) {
        try {
            this.constructor = clazz.getConstructor();
        } catch (final Exception e) {
            LOGGER.error("Unable to find default constructor for {}", tableName);
            throw new IllegalStateException("Unable to find default constructor for " + tableName, e);
        }
        this.tableName = tableName;
        this.columns = columns;
        this.idColumn = idColumn;
        this.insertSql = buildInsertStatement();
        this.updateSql = updateSql == null ? buildUpdateStatement() : updateSql;
        this.deleteSql = "delete from " + escapeReservedWord(tableName) + WHERE_CLAUSE + escapeReservedWord(idColumn) + " = ?";
        this.selectByIdSql = "select * from " + escapeReservedWord(tableName) + WHERE_CLAUSE + escapeReservedWord(idColumn) + " = ?";
        this.selectAllSql = "select * from " + escapeReservedWord(tableName);
        this.rowMapper = new Rm();
    }

    public RowMapper<T> getRowMapper() {
        return rowMapper;
    }

    public BatchPreparedStatementSetter getBatchStringSetter(Object parentId, Collection<String> values) {
        return new BatchStringSetter(parentId, values);
    }

    public String getTableName() {
        return tableName;
    }

    public List<JdbcColumn> getColumns() {
        return columns;
    }

    public String getSelectByIdSql() {
        return selectByIdSql;
    }

    public String getSelectAllSql() {
        return selectAllSql;
    }

    public String getDeleteSql() {
        return deleteSql;
    }

    public PreparedStatementCreator buildInsertPreparedStatementCreator(T item) {
        return new Psc(insertSql, item);
    }

    public PreparedStatementCreator buildUpdatePreparedStatementCreator(T item, Object... ids) {
        return new Psc(updateSql, item, ids);
    }

    public boolean buildInCondition(boolean first, StringBuilder query, String column, Collection args) {
        if ((args != null) && !args.isEmpty()) {
            query.append(first ? " where " : " and ");
            first = false;
            query.append(column).append(" in ( ");
            buildInClause(query, args);
            query.append(") ");
        }
        return first;
    }

    public String buildInClause(Collection data) {
        StringBuilder builder = new StringBuilder();
        buildInClause(builder, data);
        return builder.toString();
    }

    private void buildInClause(StringBuilder builder, Collection data) {
        boolean first = true;
        for (int i = 0; i < data.size(); i++) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append("? ");
        }
    }

    public int setArguments(PreparedStatement stmt, Collection data, int idx) throws SQLException {
        for (Object item : data) {
            LOGGER.trace("Setting {} to {}", idx, item);

            if (item instanceof Enum) {
                stmt.setString(idx++, ((Enum) item).name());
            } else {
                stmt.setObject(idx++, item);
            }
        }
        return idx;
    }

    public void setFromResultSet(final T item, final ResultSet rs) {
        for (final JdbcColumn column : columns) {
            try {
                Object value = rs.getObject(getDBName(column.name));
                if (!rs.wasNull()) {
                    if (value instanceof Clob) {
                        Clob clob = (Clob) value;
                        Reader reader = clob.getCharacterStream();
                        final char[] buf = new char[128];
                        int chars;
                        StringBuilder rslt = new StringBuilder();
                        while ((chars = reader.read(buf)) >= 0) {
                            rslt.append(buf, 0, chars);
                        }
                        value = rslt.toString();
                    }
                    value = checkTypeAndConvert(item, column, value);
                    column.setter.invoke(item, value);
                }
            } catch (SQLException ex) {
                LOGGER.debug("Field {} is not part of the result set; {}", getDBName(column.name), ex.getMessage());
            } catch (Exception ex) {
                LOGGER.error("Failed to invoke setter {} on {}; {}", column.setter, item, ex.getMessage());
            }
        }
    }

    private Object checkTypeAndConvert(final T item, final JdbcColumn column, final Object value) {
        LOGGER.trace("Converted {}.{} from {} to {}", getDBName(item.getClass().getSimpleName()), getDBName(column.name), value, value);
        if (column.javaType.isEnum() && (value instanceof String)) {
            final String stringValue = (String) value;
            if (hasText(stringValue)) {
                return Enum.valueOf(column.javaType, stringValue);
            } else {
                return null;
            }
        } else if ((column.javaType == Date.class) && (value instanceof Timestamp)) {
            final Timestamp timestampValue = (Timestamp) value;
            return new Date(timestampValue.getTime());
        } else if (column.javaType == byte.class) {
            return parseByte(value.toString());
        } else if (column.javaType == Long.class) {
            return ((Integer) value).longValue();
        } else if (column.javaType == InputStream.class) {
            byte[] data = (byte[]) value;
            return new ByteArrayInputStream(data);
        }
        return value;
    }

    private String buildInsertStatement() {
        final StringBuilder builder = new StringBuilder("insert into ");
        builder.append(escapeReservedWord(tableName));
        builder.append(" (");
        boolean first = true;
        for (JdbcColumn column : columns) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
        }
        builder.append(" ) values ( ");
        first = true;
        for (int i = 0; i < columns.size(); i++) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append("?");
        }
        builder.append(" )");
        return builder.toString();
    }

    private int setStatementValues(PreparedStatement stmt, T item, int idx, Collection<JdbcColumn> jdbcColumns) {
        for (final JdbcColumn column : jdbcColumns) {
            try {
                final Object value = column.getter.invoke(item);
                if (value == null) {
                    LOGGER.debug("Setting {}/{} to null for the type {}", idx, getDBName(column.name), column.jdbcType);
                    if (column.jdbcType == Types.NVARCHAR) {
                        stmt.setNull(idx, Types.VARCHAR);
                    } else {
                        stmt.setNull(idx, column.jdbcType);
                    }
                } else if (column.javaType.isEnum() && (column.jdbcType == Types.NVARCHAR)) {
                    stmt.setString(idx, value.toString());
                } else if (value instanceof Date) {
                    final Date date = (Date) value;
                    stmt.setTimestamp(idx, new Timestamp(date.getTime()));
                } else if (value instanceof InputStream && column.jdbcType == Types.BLOB) {
                    stmt.setBlob(idx, (InputStream) value);
                } else if (value instanceof byte[] && column.jdbcType == Types.BLOB) {
                    stmt.setBytes(idx, (byte[]) value);
                } else {
                    stmt.setObject(idx, value);
                }
                if (value != null) {
                    LOGGER.debug("Setting {}/{} to {} for the type {}", idx, getDBName(column.name), value, column.jdbcType);
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to invoke getter {} on {} : ", column.setter, item, ex);
            }
            ++idx;
        }
        return idx;
    }

    public int setStatementValues(PreparedStatement stmt, T item, int idx) {
        return setStatementValues(stmt, item, idx, columns);
    }

    private String buildUpdateStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("update ");
        builder.append(escapeReservedWord(tableName));
        builder.append(" set ");
        boolean first = true;
        for (JdbcColumn column : columns) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
            builder.append(" = ?");
        }
        builder.append(" where ");
        builder.append(escapeReservedWord(idColumn));
        builder.append(" = ?");
        return builder.toString();
    }

    public List<String> filterStrings(Collection<String> values) {
        if ((values != null) && !values.isEmpty()) {
            List<String> items = new ArrayList<>(values.size());
            for (String value : values) {
                if ((value != null) && !value.isEmpty()) {
                    items.add(value);
                }
            }
            return items;
        } else {
            return emptyList();
        }
    }
}
