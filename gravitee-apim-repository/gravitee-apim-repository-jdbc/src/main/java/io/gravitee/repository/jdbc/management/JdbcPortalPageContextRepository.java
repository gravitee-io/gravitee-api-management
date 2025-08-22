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

import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.model.PortalPageContext;
import java.sql.Types;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPortalPageContextRepository extends JdbcAbstractCrudRepository<PortalPageContext, String> {

    public JdbcPortalPageContextRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portal_page_contexts");
    }

    @Override
    protected JdbcObjectMapper<PortalPageContext> buildOrm() {
        return JdbcObjectMapper
            .builder(PortalPageContext.class, this.tableName, "page_id")
            .addColumn("page_id", Types.NVARCHAR, String.class)
            .addColumn("context", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    protected String getId(PortalPageContext item) {
        return item.getPageId();
    }

    public Set<String> findAllByPageId(String pageId) {
        return new java.util.HashSet<>(
            jdbcTemplate.query("SELECT context FROM " + tableName + " WHERE page_id = ?", (rs, rowNum) -> rs.getString("context"), pageId)
        );
    }

    public void deleteByPageIdAndContext(String id, String existingContext) {
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE page_id = ? AND context = ?", id, existingContext);
    }
}
