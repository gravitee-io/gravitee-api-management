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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author GraviteeSource Team
 */
public abstract class JdbcAbstractFindAllRepository<T> extends JdbcAbstractRepository<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAbstractFindAllRepository.class);

    JdbcAbstractFindAllRepository(String prefix, String tableName) {
        super(prefix, tableName);
    }

    public Set<T> findAll() throws TechnicalException {
        LOGGER.debug("JdbcAbstractFindAllRepository<{}>.findAll()", getOrm().getTableName());
        try {
            return new HashSet<>(jdbcTemplate.query(getOrm().getSelectAllSql(), getRowMapper()));
        } catch (final Exception ex) {
            String errorMessage = String.format("Failed to find all %s items:", getOrm().getTableName());
            LOGGER.error(errorMessage, ex);
            throw new TechnicalException(errorMessage, ex);
        }
    }
}
