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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.EntrypointRepository;
import io.gravitee.repository.management.model.Entrypoint;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcEntryPointRepository extends JdbcAbstractCrudRepository<Entrypoint, String> implements EntrypointRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcEntryPointRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Entrypoint.class, "entrypoints", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("value", Types.NVARCHAR, String.class)
            .addColumn("tags", Types.NVARCHAR, String.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Entrypoint item) {
        return item.getId();
    }
    
    @Override
    public Set<Entrypoint> findAllByEnvironment(String environment_id) throws TechnicalException {
        LOGGER.debug("JdbcEntryPointRepository.findAllByEnvironment({})", environment_id);
        try {
            List<Entrypoint> entrypoints = jdbcTemplate.query("select * from entrypoints where environment_id = ?"
                    , ORM.getRowMapper()
                    , environment_id
            );
            return new HashSet<>(entrypoints);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find entrypoints by environment:", ex);
            throw new TechnicalException("Failed to find entrypoints by environment", ex);
        }
    }
}