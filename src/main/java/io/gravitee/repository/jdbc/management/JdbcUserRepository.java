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
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

/**
 */
@Repository
public class JdbcUserRepository extends JdbcAbstractCrudRepository<User, String> implements UserRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUserRepository.class);

    private static final String SELECT_ESCAPED_USER_TABLE_NAME = "select * from " + escapeReservedWord("users");

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(User.class, "users", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("email", Types.NVARCHAR, String.class)
            .addColumn("firstname", Types.NVARCHAR, String.class)
            .addColumn("last_connection_at", Types.TIMESTAMP, Date.class)
            .addColumn("lastname", Types.NVARCHAR, String.class)
            .addColumn("password", Types.NVARCHAR, String.class)
            .addColumn("picture", Types.NVARCHAR, String.class)
            .addColumn("source", Types.NVARCHAR, String.class)
            .addColumn("source_id", Types.NVARCHAR, String.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("username", Types.NVARCHAR, String.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(User item) {
        return item.getId();
    }

    @Override
    public Optional<User> findByUsername(String username) throws TechnicalException {
        LOGGER.debug("JdbcUserRepository.findByUsername({})", username);
        try {
            List<User> users = jdbcTemplate.query(SELECT_ESCAPED_USER_TABLE_NAME + " u where u.username = ?"
                    , ORM.getRowMapper()
                    , username
            );
            return users.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find user by username", ex);
            throw new TechnicalException("Failed to find user by username", ex);
        }
    }

    @Override
    public Set<User> findByIds(final List<String> ids) throws TechnicalException {
        final String[] lastId = new String[1];
        List<String> uniqueIds = ids.stream().filter(id -> {
            if (id.equals(lastId[0])) {
                return false;
            } else {
                lastId[0] = id;
                return true;
            }
        }).collect(Collectors.toList());
        LOGGER.debug("JdbcUserRepository.findByIds({})", uniqueIds);
        try {
            final List<User> users = jdbcTemplate.query(SELECT_ESCAPED_USER_TABLE_NAME + " u where u.id in ( "
                    + ORM.buildInClause(uniqueIds) + " )"
                    , (PreparedStatement ps) -> ORM.setArguments(ps, uniqueIds, 1)
                    , ORM.getRowMapper()
            );
            return new HashSet<>(users);
        } catch (final Exception ex) {
            final String msg = "Failed to find users by ids";
            LOGGER.error(msg, ex);
            throw new TechnicalException(msg, ex);
        }
    }
}