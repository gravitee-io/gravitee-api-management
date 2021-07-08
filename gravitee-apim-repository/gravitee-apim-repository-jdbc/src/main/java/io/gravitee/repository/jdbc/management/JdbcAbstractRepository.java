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

import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class JdbcAbstractRepository<T> extends TransactionalRepository {

    private final String prefix;
    private final JdbcObjectMapper<T> orm;
    protected final String tableName;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    JdbcAbstractRepository(String prefix, String tableName) {
        this.prefix = prefix;
        this.tableName = prefix + Objects.requireNonNull(tableName, "Table name not provided");
        this.orm = buildOrm();
    }

    protected abstract JdbcObjectMapper<T> buildOrm();

    protected RowMapper<T> getRowMapper() {
        return this.orm.getRowMapper();
    }

    protected final String getTableNameFor(String tableName) {
        return this.prefix + tableName;
    }

    protected final JdbcObjectMapper<T> getOrm() {
        return orm;
    }
}
