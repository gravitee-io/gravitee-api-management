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

import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.model.ApiQualityRule;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcApiQualityRuleRepository extends JdbcAbstractFindAllRepository<ApiQualityRule> implements ApiQualityRuleRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcApiQualityRuleRepository.class);

    JdbcApiQualityRuleRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "api_quality_rules");
    }

    @Override
    protected JdbcObjectMapper<ApiQualityRule> buildOrm() {
        return JdbcObjectMapper.builder(ApiQualityRule.class, this.tableName)
            .updateSql(
                "update " +
                    this.tableName +
                    " set " +
                    " api = ?" +
                    " , quality_rule = ?" +
                    " , checked = ?" +
                    " , created_at = ? " +
                    " , updated_at = ? " +
                    WHERE_CLAUSE +
                    " api = ? " +
                    " and quality_rule = ? "
            )
            .addColumn("api", Types.NVARCHAR, String.class)
            .addColumn("quality_rule", Types.NVARCHAR, String.class)
            .addColumn("checked", Types.BOOLEAN, boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Optional<ApiQualityRule> findById(String api, String qualityRule) throws TechnicalException {
        LOGGER.debug("JdbcApiQualityRuleRepository.findById({}, {})", api, qualityRule);
        try {
            final List<ApiQualityRule> apiQualityRules = jdbcTemplate.query(
                "select" +
                    " api, quality_rule, checked, created_at, updated_at " +
                    " from " +
                    this.tableName +
                    " where api = ? and quality_rule = ?",
                getOrm().getRowMapper(),
                api,
                qualityRule
            );
            return apiQualityRules.stream().findFirst();
        } catch (final Exception ex) {
            final String error = "Failed to find apiQualityRule by id";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public ApiQualityRule create(ApiQualityRule apiQualityRule) throws TechnicalException {
        LOGGER.debug("JdbcApiQualityRuleRepository.create({})", apiQualityRule);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(apiQualityRule));
            return findById(apiQualityRule.getApi(), apiQualityRule.getQualityRule()).orElse(null);
        } catch (final Exception ex) {
            final String error = "Failed to create apiQualityRule";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public ApiQualityRule update(ApiQualityRule apiQualityRule) throws TechnicalException {
        LOGGER.debug("JdbcApiQualityRuleRepository.update({})", apiQualityRule);
        if (apiQualityRule == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(
                getOrm().buildUpdatePreparedStatementCreator(apiQualityRule, apiQualityRule.getApi(), apiQualityRule.getQualityRule())
            );
            return findById(apiQualityRule.getApi(), apiQualityRule.getQualityRule()).orElseThrow(() ->
                new IllegalStateException(
                    format("No apiQualityRule found with id [%s, %s]", apiQualityRule.getApi(), apiQualityRule.getQualityRule())
                )
            );
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            final String error = "Failed to update apiQualityRule";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void delete(String api, String qualityRule) throws TechnicalException {
        LOGGER.debug("JdbcApiQualityRuleRepository.delete({}, {})", api, qualityRule);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where api = ? and quality_rule = ? ", api, qualityRule);
        } catch (final Exception ex) {
            final String error = "Failed to delete apiQualityRule";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public List<ApiQualityRule> findByQualityRule(String qualityRule) throws TechnicalException {
        LOGGER.debug("JdbcApiQualityRuleRepository.findByQualityRule({})", qualityRule);
        try {
            final String query =
                "select " + " api, quality_rule, checked, created_at, updated_at " + " from " + this.tableName + " where quality_rule = ? ";
            return jdbcTemplate.query(query, (PreparedStatement ps) -> ps.setString(1, qualityRule), getOrm().getRowMapper());
        } catch (final Exception ex) {
            final String error = "Failed to find apiQualityRule by quality rule";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public List<ApiQualityRule> findByApi(String api) throws TechnicalException {
        LOGGER.debug("JdbcApiQualityRuleRepository.findByApi({})", api);
        try {
            final String query =
                "select " + " api, quality_rule, checked, created_at, updated_at " + " from " + this.tableName + " where api = ? ";
            return jdbcTemplate.query(query, (PreparedStatement ps) -> ps.setString(1, api), getOrm().getRowMapper());
        } catch (final Exception ex) {
            final String error = "Failed to find apiQualityRule by api";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void deleteByQualityRule(String qualityRule) throws TechnicalException {
        LOGGER.debug("JdbcApiQualityRuleRepository.deleteByQualityRule({})", qualityRule);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where quality_rule = ? ", qualityRule);
        } catch (final Exception ex) {
            final String error = "Failed to delete apiQualityRule by quality rule";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void deleteByApi(String api) throws TechnicalException {
        LOGGER.debug("JdbcApiQualityRuleRepository.deleteByApi({})", api);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where api = ? ", api);
        } catch (final Exception ex) {
            final String error = "Failed to delete apiQualityRule by api";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }
}
