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
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author njt
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 *
 */
@Repository
public class JdbcParameterRepository extends JdbcAbstractCrudRepository<Parameter, String> implements ParameterRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcParameterRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Parameter.class, "parameters", "key")
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("value", Types.NVARCHAR, String.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Parameter item) {
        return item.getKey();
    }

    @Override
    public List<Parameter> findAll(List<String> keys) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.findAll({})", keys);
        try {
            if (isEmpty(keys)) {
                return Collections.emptyList();
            }
            List<Parameter> parameters = jdbcTemplate.query("select * from parameters where " + escapeReservedWord("key") + " in ( "
                            + ORM.buildInClause(keys) + " )"
                    , (PreparedStatement ps) -> ORM.setArguments(ps, keys, 1)
                    , ORM.getRowMapper()
            );
            return new ArrayList<>(parameters);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find parameters by keys:", ex);
            throw new TechnicalException("Failed to find parameters by keys", ex);
        }
    }
}