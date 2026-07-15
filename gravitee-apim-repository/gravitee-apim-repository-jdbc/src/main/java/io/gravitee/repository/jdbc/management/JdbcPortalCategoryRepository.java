/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.repository.management.api.PortalCategoryRepository;
import io.gravitee.repository.management.model.PortalCategory;
import java.sql.Types;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcPortalCategoryRepository extends JdbcAbstractCrudRepository<PortalCategory, String> implements PortalCategoryRepository {

    JdbcPortalCategoryRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portal_categories");
    }

    @Override
    protected JdbcObjectMapper<PortalCategory> buildOrm() {
        return JdbcObjectMapper.builder(PortalCategory.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("title", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("visible", Types.BOOLEAN, boolean.class)
            .build();
    }

    @Override
    protected String getId(final PortalCategory item) {
        return item.getId();
    }

    @Override
    public List<PortalCategory> findAllByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcPortalCategoryRepository.findAllByEnvironmentId({})", environmentId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where environment_id = ? order by title",
                getOrm().getRowMapper(),
                environmentId
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find portal categories by environment id", ex);
        }
    }
}
