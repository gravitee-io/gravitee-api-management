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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ResourceRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Resource;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@CustomLog
@Repository
public class JdbcResourceRepository extends JdbcAbstractCrudRepository<Resource, String> implements ResourceRepository {

    JdbcResourceRepository(@Value("${management.jdbc.prefix:}") String prefix) {
        super(prefix, "resources");
    }

    @Override
    protected JdbcObjectMapper<Resource> buildOrm() {
        return JdbcObjectMapper.builder(Resource.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, Resource.ReferenceType.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("configuration", Types.NCLOB, String.class)
            .addColumn("enabled", Types.BOOLEAN, boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(Resource item) {
        return item.getId();
    }

    @Override
    public Page<Resource> findByReference(Resource.ReferenceType referenceType, String referenceId, Pageable pageable, String query)
        throws TechnicalException {
        log.debug("JdbcResourceRepository.findByReference({}, {}, {}, {})", referenceType, referenceId, pageable, query);
        try {
            StringBuilder sql = new StringBuilder(getOrm().getSelectAllSql());
            sql.append(" where reference_type = ? and reference_id = ? ");
            List<Object> args = new ArrayList<>();
            args.add(referenceType.name());
            args.add(referenceId);
            if (query != null && !query.isBlank()) {
                String like = "%" + query.toLowerCase() + "%";
                sql.append(" and ( lower(name) like ? or lower(type) like ? ) ");
                args.add(like);
                args.add(like);
            }
            sql.append(" order by created_at asc ");

            List<Resource> resources = jdbcTemplate.query(sql.toString(), getOrm().getRowMapper(), args.toArray());
            return getResultAsPage(pageable, resources);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find resources by reference [" + referenceType + "/" + referenceId + "]", ex);
        }
    }

    @Override
    public boolean existsByNameAndReference(String name, Resource.ReferenceType referenceType, String referenceId)
        throws TechnicalException {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "select count(1) from " + tableName + " where reference_type = ? and reference_id = ? and name = ?",
                Integer.class,
                referenceType.name(),
                referenceId,
                name
            );
            return count != null && count > 0;
        } catch (Exception ex) {
            throw new TechnicalException(
                "Failed to check resource existence [" + name + " / " + referenceType + " / " + referenceId + "]",
                ex
            );
        }
    }
}
