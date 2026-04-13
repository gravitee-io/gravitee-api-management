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
import io.gravitee.repository.management.api.CatalogItemRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.catalog.CatalogItem;
import io.gravitee.repository.management.model.catalog.Model;
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
public class JdbcCatalogItemRepository extends TransactionalRepository implements CatalogItemRepository {

    public record CatalogItemRow(String id, String kind, String sourceId, Instant createdAt, String definition) {}

    private static final Map<Class<? extends CatalogItem>, String> KIND_BY_TYPE = Map.of(Model.class, "model");

    private static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<>() {};

    private final JdbcObjectMapper<CatalogItemRow> orm;
    private final ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    JdbcCatalogItemRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        var tableName = tablePrefix + "catalog_items";
        this.orm = JdbcObjectMapper.builder(CatalogItemRow.class, tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("kind", Types.NVARCHAR, String.class)
            .addColumn("source_id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Instant.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CatalogItem create(CatalogItem item) throws TechnicalException {
        log.debug("Create catalog item [{}]", item.id());
        try {
            var row = toRow(item);
            jdbcTemplate.update(orm.buildInsertPreparedStatementCreator(row));
            return findById(item.id()).orElseThrow();
        } catch (TechnicalException te) {
            throw te;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create catalog item", ex);
        }
    }

    @Override
    public Optional<CatalogItem> findById(String id) throws TechnicalException {
        log.debug("Find catalog item by id [{}]", id);
        try {
            var rows = jdbcTemplate.query(orm.getSelectByIdSql(), orm.getRowMapper(), id);
            return rows.stream().findFirst().map(this::fromRow);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find catalog item by id", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete catalog item [{}]", id);
        try {
            jdbcTemplate.update(orm.getDeleteSql(), id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete catalog item", ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CatalogItem> Page<T> findByType(Class<T> type, Pageable pageable) throws TechnicalException {
        log.debug("Find catalog items by type [{}]", type.getSimpleName());
        var kind = KIND_BY_TYPE.get(type);
        if (kind == null) {
            throw new TechnicalException("Unknown catalog item type: " + type.getSimpleName());
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
            throw new TechnicalException("Failed to find catalog items by type", ex);
        }
    }

    @Override
    public List<CatalogItem> findBySourceId(String sourceId) throws TechnicalException {
        log.debug("Find catalog items by sourceId [{}]", sourceId);
        try {
            var sql = orm.getSelectAllSql() + " where source_id = ? order by created_at desc";
            return jdbcTemplate.query(sql, orm.getRowMapper(), sourceId).stream().map(this::fromRow).toList();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find catalog items by sourceId", ex);
        }
    }

    @Override
    public Page<CatalogItem> findAll(Pageable pageable) throws TechnicalException {
        log.debug("Find all catalog items");
        try {
            var sql = orm.getSelectAllSql() + " order by created_at desc";
            List<CatalogItem> allItems = jdbcTemplate.query(sql, orm.getRowMapper()).stream().map(this::fromRow).toList();
            return JdbcAbstractPageableRepository.getResultAsPage(pageable, allItems);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find all catalog items", ex);
        }
    }

    private CatalogItemRow toRow(CatalogItem item) {
        return new CatalogItemRow(item.id(), KIND_BY_TYPE.get(item.getClass()), item.sourceId(), item.createdAt(), toDefinition(item));
    }

    private String toDefinition(CatalogItem item) {
        try {
            return switch (item) {
                case Model m -> objectMapper.writeValueAsString(Map.of("name", m.name(), "description", m.description()));
            };
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize catalog item definition", e);
        }
    }

    private CatalogItem fromRow(CatalogItemRow row) {
        try {
            Map<String, String> def = objectMapper.readValue(row.definition(), MAP_TYPE_REF);
            return switch (row.kind()) {
                case "model" -> new Model(row.id(), row.sourceId(), row.createdAt(), def.get("name"), def.get("description"));
                default -> throw new IllegalStateException("Unknown catalog item kind: " + row.kind());
            };
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize catalog item definition", e);
        }
    }
}
