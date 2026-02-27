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

import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.node.api.upgrader.UpgraderRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.sql.Types;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcUpgraderRepository extends JdbcAbstractRepository<UpgradeRecord> implements UpgraderRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUpgraderRepository.class);

    JdbcUpgraderRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "upgraders");
    }

    @Override
    protected JdbcObjectMapper<UpgradeRecord> buildOrm() {
        return JdbcObjectMapper.builder(UpgradeRecord.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("applied_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Maybe<UpgradeRecord> findById(String id) {
        LOGGER.debug("JdbcUpgraderRepository.findById({})", id);
        try {
            return jdbcTemplate
                .query(getOrm().getSelectByIdSql(), getRowMapper(), id)
                .stream()
                .findFirst()
                .map(Maybe::just)
                .orElseGet(Maybe::empty);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find upgrader data by id {}", id, ex);
            return Maybe.error(new TechnicalException("Failed to find upgrader data by id {}", ex));
        }
    }

    @Override
    public Single<UpgradeRecord> create(UpgradeRecord upgradeRecord) {
        LOGGER.debug("JdbcUpgraderRepository.create({})", upgradeRecord);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(upgradeRecord));
            return findById(upgradeRecord.getId()).toSingle();
        } catch (final Exception ex) {
            LOGGER.error("Failed to create upgrade data:", ex);
            return Single.error(new TechnicalException("Failed to create upgrade data", ex));
        }
    }
}
