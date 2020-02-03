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

import java.sql.Types;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

/**
 *
 * @author njt
 */
@Repository
public class JdbcViewRepository implements ViewRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcViewRepository.class);

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(View.class, "views", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("default_view", Types.BIT, boolean.class)
            .addColumn("hidden", Types.BIT, boolean.class)
            .addColumn("order", Types.INTEGER, int.class)
            .addColumn("highlight_api", Types.NVARCHAR, String.class)
            .addColumn("picture", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    public Set<View> findAllByEnvironment(String environment) throws TechnicalException {
        LOGGER.debug("JdbcViewRepository.findAllByEnvironment({})", environment);
        try {
            List<View> views = jdbcTemplate.query("select * from views where environment = ?"
                    , ORM.getRowMapper()
                    , environment
            );
            return new HashSet<>(views);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find views by environment:", ex);
            throw new TechnicalException("Failed to find views by environment", ex);
        }
    }

    @Override
    public Optional<View> findById(String id, String environment) throws TechnicalException {
        LOGGER.debug("JdbcViewRepository.findById({}, {})", id, environment);
        try {
            List<View> items = jdbcTemplate.query("select * from views where id = ? and environment = ?"
                    , ORM.getRowMapper()
                    , id
                    , environment
            );
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find views items by id and environment: {} - {}", id, environment, ex);
            throw new TechnicalException("Failed to find views items by id and environment : " + id + " - " + environment, ex);
        }
    }

    @Override
    public View create(View item) throws TechnicalException {
        LOGGER.debug("JdbcViewRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            return findById(item.getId(), item.getEnvironment()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create views item:", ex);
            throw new TechnicalException("Failed to create views item.", ex);
        }
    }

    @Override
    public View update(View item) throws TechnicalException {
        LOGGER.debug("JdbcViewRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException("Unable to update null item");
        }
        try {
            int rows = jdbcTemplate.update("update views set "
                                        + " id = ?"
                                        + " , environment = ?"
                                        + " , name = ?"
                                        + " , description = ?"
                                        + " , default_view = ?"
                                        + " , hidden = ?"
                                        + " , " + escapeReservedWord("order") + " = ?"
                                        + " , highlight_api = ?"
                                        + " , picture = ?"
                                        + " , created_at = ? "
                                        + " , updated_at = ? "
                                        + " where "
                                        + " id = ? "
                                        + " and environment = ? "

                                , item.getId()
                                , item.getEnvironment()
                                , item.getName()
                                , item.getDescription()
                                , item.isDefaultView()
                                , item.isHidden()
                                , item.getOrder()
                                , item.getHighlightApi()
                                , item.getPicture()
                                , item.getCreatedAt()
                                , item.getUpdatedAt()
                                , item.getId()
                                , item.getEnvironment()
                        );
            if (rows == 0) {
                throw new IllegalStateException("Unable to update views " + item.getId() + " for the environment " + item.getEnvironment());
            } else {
                return findById(item.getId(), item.getEnvironment()).orElse(null);
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update views item:", ex);
            throw new TechnicalException("Failed to update views item", ex);
        }
    }

    @Override
    public void delete(String id, String environment) throws TechnicalException {
        LOGGER.debug("JdbcViewRepository.delete({}, {})", id, environment);
        try {
            jdbcTemplate.update("delete from views where id = ? and environment = ?", id, environment);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete views item:", ex);
            throw new TechnicalException("Failed to delete views item", ex);
        }
        
    }

    @Override
    public Set<View> findAll() throws TechnicalException {
        LOGGER.debug("JdbcViewRepository.findAll()");
        try {
            List<View> items = jdbcTemplate.query(ORM.getSelectAllSql(), ORM.getRowMapper());
            return new HashSet<>(items);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all views items:", ex);
            throw new TechnicalException("Failed to find all views items", ex);
        }
    }
}
