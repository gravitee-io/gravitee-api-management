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
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Date;
import java.util.List;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcDashboardRepository extends JdbcAbstractCrudRepository<Dashboard, String> implements DashboardRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcDashboardRepository.class);

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Dashboard.class, "dashboards", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("query_filter", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, int.class)
            .addColumn("enabled", Types.BOOLEAN, boolean.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(final Dashboard item) {
        return item.getId();
    }

    @Override
    public List<Dashboard> findByReferenceType(String referenceType) throws TechnicalException {
        LOGGER.debug("JdbcDashboardRepository.findByReferenceType({})", referenceType);
        try {
            return jdbcTemplate.query("select * from dashboards where reference_type = ? order by " + escapeReservedWord("order")
                    , ORM.getRowMapper()
                    , referenceType
            );
        } catch (final Exception ex) {
            final String error = "Failed to find dashboards by reference type";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }
}