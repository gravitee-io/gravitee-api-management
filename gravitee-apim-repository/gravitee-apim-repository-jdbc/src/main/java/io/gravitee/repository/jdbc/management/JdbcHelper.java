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

import static org.springframework.util.CollectionUtils.isEmpty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

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
        private final Map<Comparable, T> rows;

        CollatingRowMapper(RowMapper mapper, ChildAdder<T> childAdder, String idColumn) {
            this.mapper = mapper;
            this.childAdder = childAdder;
            this.idColumn = idColumn;
            this.rows = new LinkedHashMap<>();
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            T current;
            Comparable currentId = (Comparable) rs.getObject(idColumn);
            current = rows.get(currentId);
            if (current == null) {
                current = mapper.mapRow(rs, rows.size() + 1);
                rows.put(currentId, current);
            }
            childAdder.addChild(current, rs);
        }

        public List<T> getRows() {
            return new ArrayList<>(rows.values());
        }
    }

    static boolean addCondition(
        final boolean first,
        final StringBuilder builder,
        final String propName,
        final Object propVal,
        final List<Object> args
    ) {
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

    static boolean addStringsWhereClause(
        final Collection<String> collection,
        final String columnName,
        final List<Object> argsList,
        final StringBuilder builder,
        boolean started
    ) {
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
