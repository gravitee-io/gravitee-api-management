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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementCreator;

/**
 *
 * @author njt
 */
public abstract class JdbcAbstractCrudRepository<T, I> extends JdbcAbstractPageableRepository<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAbstractCrudRepository.class);

    JdbcAbstractCrudRepository(String prefix, String tableName) {
        super(prefix, tableName);
    }

    protected abstract I getId(T item);

    public Optional<T> findById(I id) throws TechnicalException {
        LOGGER.debug("JdbcAbstractCrudRepository<{}>.findById({})", getOrm().getTableName(), id);
        try {
            List<T> items = jdbcTemplate.query(getOrm().getSelectByIdSql(), getRowMapper(), id);
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} items by id:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " items by id", ex);
        }
    }

    public Set<T> findAll() throws TechnicalException {
        LOGGER.debug("JdbcAbstractCrudRepository<{}>.findAll()", getOrm().getTableName());
        try {
            List<T> items = jdbcTemplate.query(getOrm().getSelectAllSql(), getRowMapper());
            return new HashSet<>(items);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all {} items:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find all " + getOrm().getTableName() + " items", ex);
        }
    }

    public T create(T item) throws TechnicalException {
        LOGGER.debug("JdbcAbstractCrudRepository<{}>.create({})", getOrm().getTableName(), item);
        try {
            jdbcTemplate.update(buildInsertPreparedStatementCreator(item));
            return findById(getId(item)).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create {} item:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to create " + getOrm().getTableName() + " item.", ex);
        }
    }

    protected PreparedStatementCreator buildInsertPreparedStatementCreator(T item) {
        return getOrm().buildInsertPreparedStatementCreator(item);
    }

    public T update(T item) throws TechnicalException {
        LOGGER.debug("JdbcAbstractCrudRepository<{}>.update({})", getOrm().getTableName(), item);
        if (item == null) {
            throw new IllegalStateException("Unable to update null item");
        }
        try {
            int rows = jdbcTemplate.update(buildUpdatePreparedStatementCreator(item));
            if (rows == 0) {
                throw new IllegalStateException("Unable to update " + getOrm().getTableName() + " " + getId(item));
            } else {
                return findById(getId(item)).orElse(null);
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update {} item:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to update " + getOrm().getTableName() + " item", ex);
        }
    }

    protected PreparedStatementCreator buildUpdatePreparedStatementCreator(T item) {
        return getOrm().buildUpdatePreparedStatementCreator(item, getId(item));
    }

    public void delete(I id) throws TechnicalException {
        LOGGER.debug("JdbcAbstractCrudRepository<{}>.delete({})", getOrm().getTableName(), id);
        try {
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete {} item:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to delete " + getOrm().getTableName() + " item", ex);
        }
    }
}
