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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.CatalogSourceRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.catalog.CatalogSource;
import io.gravitee.repository.management.model.catalog.LlmProvider;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@CustomLog
@Repository
public class JdbcCatalogSourceRepository extends TransactionalRepository implements CatalogSourceRepository {

    public record CatalogSourceRow(String id, String kind, Instant createdAt, String definition) {}

    private static final Map<Class<? extends CatalogSource>, String> KIND_BY_TYPE = Map.of(LlmProvider.class, "llm-provider");

    private static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<>() {};

    private final JdbcObjectMapper<CatalogSourceRow> orm;
    private final ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    JdbcCatalogSourceRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        var tableName = tablePrefix + "catalog_sources";
        this.orm = JdbcObjectMapper.builder(CatalogSourceRow.class, tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("kind", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Instant.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CatalogSource create(CatalogSource source) throws TechnicalException {
        log.debug("Create catalog source [{}]", source.id());
        try {
            var row = toRow(source);
            jdbcTemplate.update(orm.buildInsertPreparedStatementCreator(row));
            return findById(source.id()).orElseThrow();
        } catch (TechnicalException te) {
            throw te;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create catalog source", ex);
        }
    }

    @Override
    public Optional<CatalogSource> findById(String id) throws TechnicalException {
        log.debug("Find catalog source by id [{}]", id);
        try {
            var rows = jdbcTemplate.query(orm.getSelectByIdSql(), orm.getRowMapper(), id);
            return rows.stream().findFirst().map(this::fromRow);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find catalog source by id", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete catalog source [{}]", id);
        try {
            jdbcTemplate.update(orm.getDeleteSql(), id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete catalog source", ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CatalogSource> Page<T> findByType(Class<T> type, Pageable pageable) throws TechnicalException {
        log.debug("Find catalog sources by type [{}]", type.getSimpleName());
        var kind = KIND_BY_TYPE.get(type);
        if (kind == null) {
            throw new TechnicalException("Unknown catalog source type: " + type.getSimpleName());
        }
        try {
            var sql = orm.getSelectAllSql() + " where kind = ? order by created_at desc";
            List<T> allItems = jdbcTemplate
                .query(sql, orm.getRowMapper(), kind)
                .stream()
                .map(row -> (T) fromRow(row))
                .toList();
            return JdbcAbstractPageableRepository.getResultAsPage(pageable, allItems);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find catalog sources by type", ex);
        }
    }

    @Override
    public Page<CatalogSource> findAll(Pageable pageable) throws TechnicalException {
        log.debug("Find all catalog sources");
        try {
            var sql = orm.getSelectAllSql() + " order by created_at desc";
            List<CatalogSource> allItems = jdbcTemplate.query(sql, orm.getRowMapper()).stream().map(this::fromRow).toList();
            return JdbcAbstractPageableRepository.getResultAsPage(pageable, allItems);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find all catalog sources", ex);
        }
    }

    private CatalogSourceRow toRow(CatalogSource source) {
        return new CatalogSourceRow(source.id(), KIND_BY_TYPE.get(source.getClass()), source.createdAt(), toDefinition(source));
    }

    private String toDefinition(CatalogSource source) {
        try {
            return switch (source) {
                case LlmProvider p -> objectMapper.writeValueAsString(Map.of("name", p.name(), "apiKey", p.apiKey()));
            };
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize catalog source definition", e);
        }
    }

    private CatalogSource fromRow(CatalogSourceRow row) {
        try {
            Map<String, String> def = objectMapper.readValue(row.definition(), MAP_TYPE_REF);
            return switch (row.kind()) {
                case "llm-provider" -> new LlmProvider(row.id(), row.createdAt(), def.get("name"), def.get("apiKey"));
                default -> throw new IllegalStateException("Unknown catalog source kind: " + row.kind());
            };
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize catalog source definition", e);
        }
    }
}
