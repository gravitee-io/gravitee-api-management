/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.orm;

import java.io.Reader;
import static java.lang.System.in;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.CharBuffer;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    
    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcObjectMapper.class);
    
    private final Class<T> clazz;
    private final Constructor<T> constructor;    
    private final List<JdbcColumn> columns;
    private final String idColumn;
    private final String insertSql;
    private final String updateSql;
    private final String selectByIdSql;
    private final String selectAllSql;
    private final String deleteSql;
    private final RowMapper<T> rowMapper;

    private static class BatchStringSetter implements BatchPreparedStatementSetter {

        private final Object parentId;
        private final List<String> values;

        public BatchStringSetter(Object parentId, Collection<String> values) {
            this.parentId = parentId;
            if (values instanceof List) {
                this.values = (List)values;
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

        public Psc(String sql, T item, Object... ids) {
            this.sql = sql;
            this.item = item;
            this.ids = ids;
        }
        
        @Override
        public PreparedStatement createPreparedStatement(Connection cnctn) throws SQLException {
            logger.trace("SQL: {}", sql);
            logger.trace("Item: {}", item);
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = setStatementValues(stmt, item, 1);
            
            for (Object id : ids) {
                stmt.setObject(idx++, id);
            }
            return stmt;
        }
        
    };
    
    public PreparedStatementCreator buildPreparedStatementCreator(String sql, T item, Object... ids) {
        return new Psc(sql, item, ids);
    }
    
    private class Rm implements RowMapper<T> {
        
        @Override
        public T mapRow(ResultSet rs, int i) throws SQLException {
            try {
                T item = constructor.newInstance();
                setFromResultSet(item, rs);            
                return item;
            } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException ex) {
                logger.error("Failed to construct {}", clazz.getSimpleName());
                throw new RuntimeException("Failed to construct " + clazz.getSimpleName(), ex);
            }
        }
    };
    
    public static class Builder<T> {

        private Class<T> clazz;
        private String idColumn;
        private String updateSql;
        private List<JdbcColumn> columns = new ArrayList<>();

        private Builder(final Class<T> value, String idColumn) {
            this.clazz = value;
            this.idColumn = idColumn;
        }

        public Builder updateSql(String updateSql) {
            this.updateSql = updateSql;
            return this;
        }
        
        public Builder addColumn(String name, int jdbcType, Class fieldType) {
            this.columns.add(new JdbcColumn(name, jdbcType, clazz, fieldType));
            return this;
        }

        public JdbcObjectMapper build() {
            return new JdbcObjectMapper(clazz, idColumn, columns, updateSql);
        }
    }

    public static JdbcObjectMapper.Builder builder(Class clazz, String idColumn) {
        return new JdbcObjectMapper.Builder(clazz, idColumn);
    }

    private JdbcObjectMapper(final Class clazz, final String idColumn, final List<JdbcColumn> columns, final String updateSql) {
        this.clazz = clazz;
        try {
            this.constructor = clazz.getConstructor();
        } catch(Throwable th) {
            logger.error("Unable to find default constructor for {}", clazz.getSimpleName());
            throw new RuntimeException("Unable to find default constructor for " + clazz.getSimpleName(), th);
        }
        this.columns = columns;
        this.idColumn = idColumn;
        this.insertSql = buildInsertStatement();
        this.updateSql = updateSql == null ? buildUpdateStatement() : updateSql;
        this.deleteSql = "delete from `" + clazz.getSimpleName() + "` where `" + idColumn + "` = ?";
        this.selectByIdSql = "select * from `" + clazz.getSimpleName() + "` where `" + idColumn + "` = ?";
        this.selectAllSql = "select * from `" + clazz.getSimpleName() + "`";
        this.rowMapper = new Rm();
    }    

    public RowMapper<T> getRowMapper() {
        return rowMapper;
    }
    
    public BatchPreparedStatementSetter getBatchStringSetter(Object parentId, Collection<String> values) {
        return new BatchStringSetter(parentId, values);
    }
    
    public List<JdbcColumn> getColumns() {
        return columns;
    }
    
    public String getInsertSql() {
        return insertSql;
    }

    public String getUpdateSql() {
        return updateSql;
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

    public boolean buildInCondition(boolean first, StringBuilder query, String column, Collection<String> args) {
        if ((args != null) && !args.isEmpty()) {
            query.append(first ? " where " : " and ");
            first = false;
            query.append("`").append(column.replaceAll("\\.", "`\\.`")).append("` in ( ");
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
    
    public void buildInClause(StringBuilder builder, Collection data) {
        boolean first = true;
        for (Object item : data) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append("? ");
        }
    }
    
    public int setArguments(PreparedStatement stmt, Collection data, int idx) throws SQLException {
        for (Object item : data) {
            logger.trace("Setting {} to {}", idx, item);
            
            if (item instanceof Enum) {
                stmt.setString(idx++, ((Enum) item).name());
            } else {
                stmt.setObject(idx++, item);
            }
        }
        return idx;
    }
    
    public T setFromResultSet(T item, ResultSet rs) throws SQLException {
        
        for (int i = 0; i < columns.size(); ++i) {
            JdbcColumn column = columns.get(i);
            Object value = rs.getObject(column.name);
            try {
                if (!rs.wasNull()) {
                    
                    if (value instanceof Clob) {
                        Clob clob = (Clob) value;
                        Reader reader = clob.getCharacterStream();
                        char buf[] = new char[128];
                        int chars = 0;
                        StringBuilder rslt = new StringBuilder();
                        while ((chars = reader.read(buf)) >= 0) {
                            rslt.append(buf, 0, chars);
                        }
                        value = rslt.toString();
                    }
                    if (column.javaType.isEnum() && (value instanceof String)) {
                        String stringValue = (String) value;
                        value = Enum.valueOf(column.javaType, stringValue);
                        logger.trace("Converted {}.{} from {} to {}", item.getClass().getSimpleName(), column.name, stringValue, value);
                    }
                    if ((column.javaType == Date.class) && (value instanceof Timestamp)) {
                        Timestamp timestampValue = (Timestamp) value;
                        value = new Date(timestampValue.getTime());
                        logger.trace("Converted {}.{} from {} to {}", item.getClass().getSimpleName(), column.name, timestampValue, value);
                    }
                    
                    column.setter.invoke(item, value);
                }
            } catch(Exception ex) {
                logger.error("Failed to invoke setter {} on {} with {}: ", column.setter, item, value, ex);
            }
        }
        
        return item;
        
    }
    
    private String buildInsertStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into `").append(clazz.getSimpleName());
        builder.append("` (");
        boolean first = true;
        for (JdbcColumn column : columns) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append('`').append(column.name).append('`');
        }
        builder.append(" ) values ( ");
        first = true;
        for (JdbcColumn column : columns) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append("?");
        }
        builder.append(" )");
        return builder.toString();
    }
    
    public int setStatementValues(PreparedStatement stmt, T item, int idx, Collection<JdbcColumn> jdbcColumns) {
        for (JdbcColumn column : jdbcColumns) {
            try {
                Object value = column.getter.invoke(item);
                if (value == null) {
                    logger.trace("Setting {}/{} to null", idx, column.name);
                    stmt.setNull(idx, column.jdbcType);
                } else if ((column.javaType.isEnum()) && (column.jdbcType == Types.NVARCHAR)) {
                    logger.debug("Setting {}/{} to {}", idx, column.name, value);
                    stmt.setString(idx, value.toString());
                } else {
                    logger.trace("Setting {}/{} to {}", idx, column.name, value);
                    stmt.setObject(idx, value);
                }
            } catch(Exception ex) {
                logger.error("Failed to invoke getter {} on {} : ", column.setter, item, ex);
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
        builder.append("update `").append(clazz.getSimpleName());
        builder.append("` set ");
        boolean first = true;
        for (JdbcColumn column : columns) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append('`').append(column.name).append('`');
            builder.append(" = ?");
        }
        builder.append(" where ").append(idColumn).append(" = ?");
        return builder.toString();
    }
    
    public List<String> filterStrings(Collection<String> values) {
        if ((values != null) && ! values.isEmpty()) {
            List<String> items = new ArrayList<>(values.size());
            for (String value : values) {
                if ((value != null) && !value.isEmpty()) {
                    items.add(value);
                }
            }
            return items;
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
}
