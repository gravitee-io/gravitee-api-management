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

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.GammaMetricDefinitionRepository;
import io.gravitee.repository.management.model.GammaMetricDefinition;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Types;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcGammaMetricDefinitionRepository
    extends JdbcAbstractCrudRepository<GammaMetricDefinition, String>
    implements GammaMetricDefinitionRepository {

    /**
     * Runs the insert in a savepoint of the caller's transaction (in a transaction of its own otherwise). On PostgreSQL
     * a failed statement aborts the whole transaction: without the savepoint, an insert refused as a duplicate would
     * make the caller's next statement fail, such as the read of the record that won the race.
     */
    private final TransactionTemplate savepoint;

    JdbcGammaMetricDefinitionRepository(
        @Value("${management.jdbc.prefix:}") String tablePrefix,
        @Lazy @Autowired @Qualifier("graviteeTransactionManager") PlatformTransactionManager transactionManager
    ) {
        super(tablePrefix, "gamma_metric_definitions");
        savepoint = new TransactionTemplate(transactionManager);
        savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    @Override
    protected JdbcObjectMapper<GammaMetricDefinition> buildOrm() {
        return JdbcObjectMapper.builder(GammaMetricDefinition.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("raw_key", Types.NVARCHAR, String.class)
            .addMirroredColumn("raw_key_hash", Types.NVARCHAR, item -> rawKeyHash(item.getRawKey()))
            .addColumn("label", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("unit", Types.NVARCHAR, String.class)
            .addColumn("exposed", Types.BOOLEAN, boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(GammaMetricDefinition item) {
        return item.getId();
    }

    /**
     * Inserts directly rather than through the inherited {@code create}, which logs every failure at error level: a
     * second record on a key is an expected outcome here, answered as a duplicate.
     */
    @Override
    public GammaMetricDefinition create(GammaMetricDefinition item) throws TechnicalException {
        log.debug("JdbcGammaMetricDefinitionRepository.create({})", item.getRawKey());
        try {
            savepoint.executeWithoutResult(status -> jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item)));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new DuplicateKeyException(
                "Gamma metric definition [" + item.getRawKey() + "] already exists in environment [" + item.getEnvironmentId() + "]",
                ex
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create gamma metric definition [" + item.getRawKey() + "]", ex);
        }
        return findById(item.getId()).orElseThrow(() ->
            new TechnicalException("Gamma metric definition [" + item.getId() + "] not found after its creation")
        );
    }

    @Override
    public List<GammaMetricDefinition> findByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcGammaMetricDefinitionRepository.findByEnvironmentId({})", environmentId);
        try {
            return jdbcTemplate
                .query(getOrm().getSelectAllSql() + " where environment_id = ?", getRowMapper(), environmentId)
                .stream()
                .sorted(Comparator.comparing(GammaMetricDefinition::getRawKey))
                .toList();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find gamma metric definitions by environment id: " + environmentId, ex);
        }
    }

    @Override
    public Optional<GammaMetricDefinition> findByEnvironmentIdAndRawKey(String environmentId, String rawKey) throws TechnicalException {
        log.debug("JdbcGammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey({}, {})", environmentId, rawKey);
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " where environment_id = ? and raw_key_hash = ?",
                    getRowMapper(),
                    environmentId,
                    rawKeyHash(rawKey)
                )
                .stream()
                .findFirst();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find gamma metric definition [" + rawKey + "] of environment id: " + environmentId, ex);
        }
    }

    /**
     * Keys are case-sensitive, while MySQL, MariaDB and SQL Server compare {@code raw_key} case-insensitively under
     * their default collations: the lowercase hex digest gives the unique constraint and the lookup an exact
     * comparison whatever the collation. Ordering is done here for the same reason.
     */
    private static String rawKeyHash(String rawKey) {
        if (rawKey == null) {
            return null;
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcGammaMetricDefinitionRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete gamma metric definitions by environment id: " + environmentId, ex);
        }
    }
}
