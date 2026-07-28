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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.GammaDashboardRepository;
import io.gravitee.repository.management.model.GammaDashboard;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcGammaDashboardRepository extends JdbcAbstractCrudRepository<GammaDashboard, String> implements GammaDashboardRepository {

    /**
     * Unknown properties are ignored on purpose, but this only picks the lesser of two silent failures — it does not
     * make the store forward-compatible.
     *
     * <p>{@link io.gravitee.repository.jdbc.orm.JdbcObjectMapper#setFromResultSet} catches per column and only logs, so
     * a deserialization failure never fails the query: the row still maps and the field is left {@code null}. With the
     * feature enabled, a row written by a newer node would therefore come back with {@code filters} entirely
     * {@code null}, and the next {@code update()} would write that null back. Ignoring the unknown field instead loses
     * only that field.
     *
     * <p>Both paths lose data silently on a rollback or a rolling upgrade. Closing the hole properly means either
     * storing these columns opaquely, like {@code widgets}, or enforcing {@code version} before the first write
     * endpoint ships.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    JdbcGammaDashboardRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "gamma_dashboards");
    }

    /**
     * {@code filters} is declared last on purpose: its element type cannot be expressed as a {@code Class} literal, so
     * {@code List.class} makes that call an unchecked invocation, which would erase the generics of every builder call
     * chained after it.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected JdbcObjectMapper<GammaDashboard> buildOrm() {
        return JdbcObjectMapper.builder(GammaDashboard.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("title", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("created_by", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("version", Types.INTEGER, Integer.class)
            .addColumn(
                "time_range",
                Types.NCLOB,
                GammaDashboard.TimeRange.class,
                JdbcGammaDashboardRepository::serializeTimeRange,
                JdbcGammaDashboardRepository::deserializeTimeRange
            )
            // Already an opaque JSON String on the model, so it needs no conversion.
            .addColumn("widgets", Types.NCLOB, String.class)
            .addColumn(
                "filters",
                Types.NCLOB,
                List.class,
                JdbcGammaDashboardRepository::serializeFilters,
                JdbcGammaDashboardRepository::deserializeFilters
            )
            .build();
    }

    @Override
    protected String getId(GammaDashboard item) {
        return item.getId();
    }

    @Override
    public List<GammaDashboard> findByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcGammaDashboardRepository.findByEnvironmentId({})", environmentId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where environment_id = ? order by created_at, id",
                getRowMapper(),
                environmentId
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find gamma dashboards by environment id: " + environmentId, ex);
        }
    }

    @Override
    public Optional<GammaDashboard> findByIdAndEnvironmentId(String id, String environmentId) throws TechnicalException {
        log.debug("JdbcGammaDashboardRepository.findByIdAndEnvironmentId({}, {})", id, environmentId);
        try {
            return jdbcTemplate
                .query(getOrm().getSelectAllSql() + " where id = ? and environment_id = ?", getRowMapper(), id, environmentId)
                .stream()
                .findFirst();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find gamma dashboard by id: " + id + " and environment id: " + environmentId, ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcGammaDashboardRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete gamma dashboards by environment id: " + environmentId, ex);
        }
    }

    private static String serializeFilters(List<?> filters) {
        return serialize(filters);
    }

    private static List<GammaDashboard.Filter> deserializeFilters(String json) {
        return deserialize(json, new TypeReference<>() {});
    }

    private static String serializeTimeRange(GammaDashboard.TimeRange timeRange) {
        return serialize(timeRange);
    }

    private static GammaDashboard.TimeRange deserializeTimeRange(String json) {
        return deserialize(json, new TypeReference<>() {});
    }

    private static String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize gamma dashboard JSON column", e);
        }
    }

    private static <T> T deserialize(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize gamma dashboard JSON column", e);
        }
    }
}
