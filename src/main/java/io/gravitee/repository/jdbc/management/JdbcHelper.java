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
package io.gravitee.repository.jdbc.management;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;


/**
 *
 * @author njt
 */
public class JdbcHelper {

    public static final String AND_CLAUSE = " and ";
    public static final String WHERE_CLAUSE = " where ";

    @FunctionalInterface
    public interface ChildAdder<T> {
        void addChild(T parent, ResultSet rs) throws SQLException;
    }
    
    public static class CollatingRowMapper<T> implements RowCallbackHandler {

        private final RowMapper<T> mapper;
        private final ChildAdder<T> childAdder;
        private final String idColumn;
        private final List<T> rows;
        private Comparable lastId;
        private T current;

        CollatingRowMapper(RowMapper mapper, ChildAdder<T> childAdder, String idColumn) {
            this.mapper = mapper;
            this.childAdder = childAdder;
            this.idColumn = idColumn;
            this.rows = new ArrayList<>();
        }
        
        @Override
        public void processRow(ResultSet rs) throws SQLException {
            
            Comparable currentId = (Comparable)rs.getObject(idColumn);
            if ((lastId == null) || (lastId.compareTo(currentId) != 0)) {
                lastId = currentId;
                current = mapper.mapRow(rs, rows.size() + 1);
                rows.add(current);
            }
            childAdder.addChild(current, rs);
            
        }

        public List<T> getRows() {
            return rows;
        }
    }

    public static class CollatingRowMapperTwoColumn<T> implements RowCallbackHandler {

        private final RowMapper<T> mapper;
        private final ChildAdder<T> childAdder;
        private final String idColumn1;
        private final String idColumn2;
        private final List<T> rows;
        private Comparable lastId1;
        private Comparable lastId2;
        private T current;

        CollatingRowMapperTwoColumn(RowMapper mapper, ChildAdder<T> childAdder, String idColumn1, String idColumn2) {
            this.mapper = mapper;
            this.childAdder = childAdder;
            this.idColumn1 = idColumn1;
            this.idColumn2 = idColumn2;
            this.rows = new ArrayList<>();
        }
        
        @Override
        public void processRow(ResultSet rs) throws SQLException {
            
            Comparable currentId1 = (Comparable)rs.getObject(idColumn1);
            Comparable currentId2 = (Comparable)rs.getObject(idColumn2);
            if (
                    (lastId1 == null) 
                    || (lastId1.compareTo(currentId1) != 0)
                    || (lastId2 == null) 
                    || (lastId2.compareTo(currentId2) != 0)
                    ) {
                lastId1 = currentId1;
                lastId2 = currentId2;
                current = mapper.mapRow(rs, rows.size() + 1);
                rows.add(current);
            }
            childAdder.addChild(current, rs);
        }

        public List<T> getRows() {
            return rows;
        }
    }

    static boolean addCondition(final boolean first, final StringBuilder builder, final String propName,
                                 final Object propVal, final List<Object> args) {
        if (!first) {
            builder.append(" or ");
        }
        if (propVal == null) {
            builder.append(" ( prop.property_key = ? and prop.property_value is null ) ");
            args.add(propName);
        } else {
            builder.append(" ( prop.property_key = ? and prop.property_value = ? ) ");
            args.add(propName);
            args.add(propVal);
        }
        return false;
    }

    static boolean addStringsWhereClause(final Collection<String> collection, final String columnName, final List<Object> argsList,
                                          final StringBuilder builder, boolean started) {
        if (!isEmpty(collection)) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("( ");
            builder.append(columnName);
            builder.append(" in (");
            boolean first = true;
            for (final String item : collection) {
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                builder.append("?");
                argsList.add(item);
            }
            builder.append(") ) ");
            started = true;
        }
        return started;
    }
}