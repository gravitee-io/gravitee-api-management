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
import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.PortalPageContent;
import java.sql.Types;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class JdbcPortalPageContentRepository
    extends JdbcAbstractCrudRepository<PortalPageContent, String>
    implements PortalPageContentRepository {

    JdbcPortalPageContentRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portal_page_contents");
    }

    @Override
    protected JdbcObjectMapper<PortalPageContent> buildOrm() {
        return JdbcObjectMapper.builder(PortalPageContent.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, PortalPageContent.Type.class)
            .addColumn("configuration", Types.NVARCHAR, String.class)
            .addColumn("content", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    public List<PortalPageContent> findAllByType(PortalPageContent.Type type) throws TechnicalException {
        log.debug("JdbcPortalPageContentRepository.findAllByType({})", type);
        try {
            final String sql = getOrm().getSelectAllSql() + " where type = ?";
            return jdbcTemplate.query(sql, getOrm().getRowMapper(), type.name());
        } catch (Exception ex) {
            log.error("Failed to find portal page contents by type", ex);
            throw new TechnicalException("Failed to find portal page contents by type", ex);
        }
    }

    @Override
    protected String getId(PortalPageContent item) {
        return item.getId();
    }
}
