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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.model.ApiHeader;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcApiHeaderRepository extends JdbcAbstractCrudRepository<ApiHeader, String> implements ApiHeaderRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiHeaderRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(ApiHeader.class, "api_headers", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("value", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, int.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(ApiHeader item) {
        return item.getId();
    }

    @Override
    public Set<ApiHeader> findAllByEnvironment(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcApiHeaderRepository.findAllByEnvironment({})", environmentId);
        try {
            List<ApiHeader> apiHeaders = jdbcTemplate.query("select * from api_headers where environment_id = ?"
                    , ORM.getRowMapper()
                    , environmentId
            );
            return new HashSet<>(apiHeaders);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find api headers by environment:", ex);
            throw new TechnicalException("Failed to find api headers by environment", ex);
        }
    }
}
