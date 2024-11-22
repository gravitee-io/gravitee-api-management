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
import io.gravitee.repository.management.api.ScoringFunctionRepository;
import io.gravitee.repository.management.model.ScoringFunction;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class JdbcScoringFunctionRepository
    extends JdbcAbstractCrudRepository<ScoringFunction, String>
    implements ScoringFunctionRepository {

    JdbcScoringFunctionRepository(@Value("${management.jdbc.prefix:}") String prefix) {
        super(prefix, "scoring_functions");
    }

    @Override
    protected String getId(ScoringFunction item) {
        return item.getId();
    }

    @Override
    protected JdbcObjectMapper<ScoringFunction> buildOrm() {
        return JdbcObjectMapper
            .builder(ScoringFunction.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("payload", Types.NCLOB, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public List<ScoringFunction> findAllByReferenceId(String referenceId, String referenceType) throws TechnicalException {
        var query = "select * from %s where reference_id = ? and reference_type = ?".formatted(this.tableName);
        return jdbcTemplate.query(query, getOrm().getRowMapper(), referenceId, referenceType);
    }

    @Override
    public List<String> deleteByReferenceId(String referenceId, String referenceType) throws TechnicalException {
        var rows = jdbcTemplate.queryForList(
            "select id from %s where reference_id = ? and reference_type = ?".formatted(this.tableName),
            String.class,
            referenceId,
            referenceType
        );
        if (!rows.isEmpty()) {
            jdbcTemplate.update(
                "delete from %s where reference_id = ? and reference_type = ?".formatted(this.tableName),
                referenceId,
                referenceType
            );
        }
        return rows;
    }
}
