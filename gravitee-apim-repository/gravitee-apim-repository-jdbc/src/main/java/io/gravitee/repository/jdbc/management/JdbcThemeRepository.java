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

import static java.util.stream.Collectors.toSet;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcThemeRepository extends JdbcAbstractCrudRepository<Theme, String> implements ThemeRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcThemeRepository.class);

    JdbcThemeRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "themes");
    }

    @Override
    protected JdbcObjectMapper<Theme> buildOrm() {
        return JdbcObjectMapper
            .builder(Theme.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("enabled", Types.BIT, boolean.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("logo", Types.NVARCHAR, String.class)
            .addColumn("optional_logo", Types.NVARCHAR, String.class)
            .addColumn("background_image", Types.NVARCHAR, String.class)
            .addColumn("favicon", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    protected String getId(final Theme item) {
        return item.getId();
    }

    @Override
    public Set<Theme> findAll() throws TechnicalException {
        return super.findAll().stream().collect(toSet());
    }

    @Override
    public Set<Theme> findByReferenceIdAndReferenceType(String referenceId, String referenceType) throws TechnicalException {
        LOGGER.debug("JdbcThemeRepository.findByReference({})", referenceType);
        try {
            return new HashSet(
                jdbcTemplate.query(
                    getOrm().getSelectAllSql() + " where reference_id = ? and reference_type = ?",
                    getOrm().getRowMapper(),
                    referenceId,
                    referenceType
                )
            );
        } catch (final Exception ex) {
            final String error = "Failed to find themes by reference type";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }
}
