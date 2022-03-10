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

import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.node.api.upgrader.UpgraderRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

    JdbcUpgraderRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "upgraders");
    }

    @Override
    protected JdbcObjectMapper<UpgradeRecord> buildOrm() {
        return JdbcObjectMapper
            .builder(UpgradeRecord.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("status", Types.INTEGER, int.class)
            .addColumn("started_at", Types.TIMESTAMP, Date.class)
            .addColumn("stopped_at", Types.TIMESTAMP, Date.class)
            .addColumn("message", Types.NVARCHAR, String.class)
            .addColumn("version", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    public Maybe<UpgradeRecord> findById(String id) {
        LOGGER.debug("JdbcUpgraderRepository.findByUpgraderId({})", id);
        try {
            return findById0(id).map(Maybe::just).orElseGet(Maybe::empty);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find upgrader data by id {}", id, ex);
            return Maybe.error(new TechnicalException("Failed to find upgrader data by id {}", ex));
        }
    }

    public Optional<UpgradeRecord> findById0(String id) throws TechnicalException {
        LOGGER.debug("JdbcUpgraderRepository<{}>.findById({})", getOrm().getTableName(), id);
        try {
            List<UpgradeRecord> items = jdbcTemplate.query(getOrm().getSelectByIdSql(), getRowMapper(), id);
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find {} items by id:", getOrm().getTableName(), ex);
            throw new TechnicalException("Failed to find " + getOrm().getTableName() + " items by id", ex);
        }
    }

    @Override
    public Single<UpgradeRecord> create(UpgradeRecord upgradeRecord) {
        LOGGER.debug("JdbcUpgraderRepository.create({})", upgradeRecord);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(upgradeRecord));

            return findById0(upgradeRecord.getId()).map(Maybe::just).orElseGet(Maybe::empty).toSingle();
        } catch (final Exception ex) {
            LOGGER.error("Failed to create upgrade data:", ex);
            return Single.error(new TechnicalException("Failed to create upgrade data", ex));
        }
    }

    @Override
    public Single<UpgradeRecord> update(UpgradeRecord upgradeRecord) {
        LOGGER.debug("JdbcUpgraderRepository.update({})", upgradeRecord);
        if (upgradeRecord == null) {
            return Single.error(new IllegalStateException("Unable to update null item"));
        }

        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(upgradeRecord, upgradeRecord.getId()));

            return findById0(upgradeRecord.getId())
                .map(Single::just)
                .orElseGet(
                    () ->
                        Single.error(
                            new IllegalStateException(String.format("No upgrade record found with id [%s]", upgradeRecord.getId()))
                        )
                );
        } catch (final Exception ex) {
            LOGGER.error("Failed to update upgrader record:", ex);
            return Single.error(new TechnicalException("Failed to update upgrader record", ex));
        }
    }
}
