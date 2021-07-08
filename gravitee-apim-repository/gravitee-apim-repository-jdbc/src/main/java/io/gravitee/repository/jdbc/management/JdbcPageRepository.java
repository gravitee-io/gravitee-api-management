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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createPagingClause;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.orm.JdbcColumn.getDBName;
import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcColumn;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageMedia;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageSource;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * @author njt
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcPageRepository extends JdbcAbstractCrudRepository<Page, String> implements PageRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPageRepository.class);

    private static final String ESCAPED_ORDER_COLUMN_NAME = escapeReservedWord("order");

    private static final JdbcObjectMapper ORM = JdbcObjectMapper
        .builder(Page.class, "pages", "id")
        .addColumn("id", Types.NVARCHAR, String.class)
        .addColumn("reference_type", Types.NVARCHAR, PageReferenceType.class)
        .addColumn("reference_id", Types.NVARCHAR, String.class)
        .addColumn("type", Types.NVARCHAR, String.class)
        .addColumn("name", Types.NVARCHAR, String.class)
        .addColumn("content", Types.NVARCHAR, String.class)
        .addColumn("last_contributor", Types.NVARCHAR, String.class)
        .addColumn("order", Types.INTEGER, int.class)
        .addColumn("published", Types.BOOLEAN, boolean.class)
        .addColumn("homepage", Types.BOOLEAN, boolean.class)
        .addColumn("created_at", Types.TIMESTAMP, Date.class)
        .addColumn("updated_at", Types.TIMESTAMP, Date.class)
        .addColumn("parent_id", Types.NVARCHAR, String.class)
        .addColumn("use_auto_fetch", Types.BOOLEAN, Boolean.class)
        .build();

    private static final JdbcHelper.ChildAdder<Page> CHILD_ADDER = (Page parent, ResultSet rs) -> {
        Map<String, String> configuration = parent.getConfiguration();
        if (configuration == null) {
            configuration = new HashMap<>();
            parent.setConfiguration(configuration);
        }
        if (rs.getString("pc_k") != null) {
            configuration.put(rs.getString("pc_k"), rs.getString("pc_v"));
        }

        Map<String, String> metadata = parent.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            parent.setMetadata(metadata);
        }
        if (rs.getString("pm_k") != null) {
            metadata.put(rs.getString("pm_k"), rs.getString("pm_v"));
        }
    };

    private class Rm implements RowMapper<Page> {

        @Override
        public Page mapRow(ResultSet rs, int i) throws SQLException {
            Page page = new Page();
            ORM.setFromResultSet(page, rs);
            String sourceType = rs.getString("source_type");
            String sourceConfiguration = rs.getString("source_configuration");
            if ((sourceType != null) || (sourceConfiguration != null)) {
                PageSource pageSource = new PageSource();
                pageSource.setType(sourceType);
                pageSource.setConfiguration(sourceConfiguration);
                page.setSource(pageSource);
            }
            addExcludedGroups(page);
            addAttachedMedia(page);
            return page;
        }
    }

    private final Rm mapper = new Rm();

    private static class Psc implements PreparedStatementCreator {

        private final String sql;
        private final Page page;
        private final Object[] ids;

        public Psc(String sql, Page page, Object... ids) {
            this.sql = sql;
            this.page = page;
            this.ids = ids;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection cnctn) throws SQLException {
            LOGGER.debug("SQL: {}", sql);
            LOGGER.debug("page: {}", page);
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = ORM.setStatementValues(stmt, page, 1);
            stmt.setString(idx++, page.getSource() == null ? null : page.getSource().getType());
            stmt.setString(idx++, page.getSource() == null ? null : page.getSource().getConfiguration());

            for (Object id : ids) {
                stmt.setObject(idx++, id);
            }
            return stmt;
        }
    }

    private static String buildInsertStatement() {
        final StringBuilder builder = new StringBuilder("insert into pages (");
        boolean first = true;
        for (JdbcColumn column : (List<JdbcColumn>) ORM.getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
        }
        builder.append(", source_type");
        builder.append(", source_configuration");
        builder.append(" ) values ( ");
        first = true;
        for (int i = 0; i < ORM.getColumns().size(); i++) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append("?");
        }
        builder.append(", ?");
        builder.append(", ?");
        builder.append(" )");
        return builder.toString();
    }

    private static final String INSERT_SQL = buildInsertStatement();

    private static String buildUpdateStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("update pages set ");
        boolean first = true;
        for (JdbcColumn column : (List<JdbcColumn>) ORM.getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
            builder.append(" = ?");
        }
        builder.append(", source_type = ?");
        builder.append(", source_configuration = ?");

        builder.append(" where id = ?");
        return builder.toString();
    }

    private static final String UPDATE_SQL = buildUpdateStatement();

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Page item) {
        return item.getId();
    }

    @Override
    protected RowMapper<Page> getRowMapper() {
        return mapper;
    }

    @Override
    protected PreparedStatementCreator buildUpdatePreparedStatementCreator(Page page) {
        return new Psc(UPDATE_SQL, page, page.getId());
    }

    @Override
    protected PreparedStatementCreator buildInsertPreparedStatementCreator(Page page) {
        return new Psc(INSERT_SQL, page);
    }

    private void addAttachedMedia(Page page) {
        List<PageMedia> attachedMedia = getAttachedMedia(page.getId());
        page.setAttachedMedia(attachedMedia);
    }

    private List<PageMedia> getAttachedMedia(String pageId) {
        return jdbcTemplate.query(
            "select media_hash, media_name, attached_at from page_attached_media where page_id = ?",
            (ResultSet rs, int rowNum) -> new PageMedia(rs.getString(1), rs.getString(2), rs.getTimestamp(3)),
            pageId
        );
    }

    private void storeAttachedMedia(Page page, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from page_attached_media where page_id = ?", page.getId());
        }
        final List<PageMedia> attachedMedia = page.getAttachedMedia();
        if (attachedMedia != null && !attachedMedia.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into page_attached_media ( page_id, media_hash, media_name, attached_at ) values ( ?, ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, page.getId());
                        ps.setString(2, attachedMedia.get(i).getMediaHash());
                        ps.setString(3, attachedMedia.get(i).getMediaName());
                        ps.setTimestamp(4, new java.sql.Timestamp(attachedMedia.get(i).getAttachedAt().getTime()));
                    }

                    @Override
                    public int getBatchSize() {
                        return attachedMedia.size();
                    }
                }
            );
        }
    }

    private void addExcludedGroups(Page page) {
        List<String> excludedGroups = getExcludedGroups(page.getId());
        page.setExcludedGroups(excludedGroups);
    }

    private List<String> getExcludedGroups(String pageId) {
        return jdbcTemplate.query(
            "select excluded_group from page_excluded_groups where page_id = ?",
            (ResultSet rs, int rowNum) -> rs.getString(1),
            pageId
        );
    }

    private void storeExcludedGroups(Page page, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from page_excluded_groups where page_id = ?", page.getId());
        }
        final List<String> excludedGroups = page.getExcludedGroups();
        if (excludedGroups != null && !excludedGroups.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into page_excluded_groups ( page_id, excluded_group ) values ( ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, page.getId());
                        ps.setString(2, excludedGroups.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return excludedGroups.size();
                    }
                }
            );
        }
    }

    private void storeConfiguration(Page page, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from page_configuration where page_id = ?", page.getId());
        }
        if (page.getConfiguration() != null && !page.getConfiguration().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(page.getConfiguration().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into page_configuration ( page_id, k, v ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, page.getId());
                        ps.setString(2, entries.get(i).getKey());
                        ps.setString(3, entries.get(i).getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return entries.size();
                    }
                }
            );
        }
    }

    private void storeMetadata(Page page, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from page_metadata where page_id = ?", page.getId());
        }
        if (page.getMetadata() != null && !page.getMetadata().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(page.getMetadata().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into page_metadata ( page_id, k, v ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, page.getId());
                        ps.setString(2, entries.get(i).getKey());
                        ps.setString(3, entries.get(i).getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return entries.size();
                    }
                }
            );
        }
    }

    @Override
    public Optional<Page> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Page> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            jdbcTemplate.query(
                "select p.*, " +
                "pm.k as pm_k, pm.v as pm_v, " +
                "pc.k as pc_k, pc.v as pc_v " +
                "from pages p " +
                "left join page_configuration pc on p.id = pc.page_id " +
                "left join page_metadata pm on p.id = pm.page_id " +
                "where p.id = ?",
                rowMapper,
                id
            );
            Optional<Page> result = rowMapper.getRows().stream().findFirst();
            result.ifPresent(
                page -> {
                    this.addExcludedGroups(page);
                    this.addAttachedMedia(page);
                }
            );
            LOGGER.debug("JdbcPageRepository.findById({}) = {}", id, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find page by id:", ex);
            throw new TechnicalException("Failed to find page by id", ex);
        }
    }

    @Override
    public io.gravitee.common.data.domain.Page<Page> findAll(Pageable pageable) throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.findAll()", pageable);
        try {
            JdbcHelper.CollatingRowMapper<Page> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");
            Integer totalPages = jdbcTemplate.queryForObject("select count(*) from pages p", Integer.class);
            jdbcTemplate.query(
                "select p.*, " +
                "pm.k as pm_k, pm.v as pm_v, " +
                "pc.k as pc_k, pc.v as pc_v " +
                "from ( select * from pages ORDER BY id " +
                createPagingClause(pageable.pageSize(), pageable.from()) +
                ") as p " +
                "left join page_configuration pc on p.id = pc.page_id " +
                "left join page_metadata pm on p.id = pm.page_id",
                rowMapper
            );
            List<Page> result = rowMapper
                .getRows()
                .stream()
                .limit(pageable.pageSize())
                .map(
                    p -> {
                        addExcludedGroups(p);
                        addAttachedMedia(p);
                        return p;
                    }
                )
                .collect(Collectors.toList());
            LOGGER.debug("JdbcPageRepository.findAll() = {} result", result.size());
            return new io.gravitee.common.data.domain.Page<>(result, pageable.pageNumber(), result.size(), totalPages);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find page by id:", ex);
            throw new TechnicalException("Failed to find page by id", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from page_excluded_groups where page_id = ?", id);
        jdbcTemplate.update("delete from page_configuration where page_id = ?", id);
        jdbcTemplate.update("delete from page_metadata where page_id = ?", id);
        jdbcTemplate.update(ORM.getDeleteSql(), id);
    }

    @Override
    public Page create(Page item) throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.create({})", item);
        try {
            jdbcTemplate.update(buildInsertPreparedStatementCreator(item));
            storeExcludedGroups(item, false);
            storeAttachedMedia(item, false);
            storeConfiguration(item, false);
            storeMetadata(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create page", ex);
            throw new TechnicalException("Failed to create page", ex);
        }
    }

    @Override
    public Page update(final Page page) throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.update({})", page);
        if (page == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(buildUpdatePreparedStatementCreator(page));
            storeExcludedGroups(page, true);
            storeAttachedMedia(page, true);
            storeConfiguration(page, true);
            storeMetadata(page, true);
            return findById(page.getId()).orElseThrow(() -> new IllegalStateException(format("No page found with id [%s]", page.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update page", ex);
            throw new TechnicalException("Failed to update page", ex);
        }
    }

    @Override
    public Integer findMaxPageReferenceIdAndReferenceTypeOrder(String referenceId, PageReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.findMaxPageReferenceIdAndReferenceTypeOrder({}, {})", referenceId, referenceType);
        try {
            Integer result = jdbcTemplate.queryForObject(
                "select max(" + ESCAPED_ORDER_COLUMN_NAME + ") from pages where reference_type = ? and reference_id = ? ",
                Integer.class,
                referenceType.name(),
                referenceId
            );
            return result == null ? 0 : result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find max page order by api:", ex);
            throw new TechnicalException("Failed to find max page order by api", ex);
        }
    }

    @Override
    public List<Page> search(PageCriteria criteria) throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.search()");
        try {
            JdbcHelper.CollatingRowMapper<Page> rowMapper = new JdbcHelper.CollatingRowMapper<>(mapper, CHILD_ADDER, "id");

            String select =
                "select distinct p.*, " +
                "pm.k as pm_k, pm.v as pm_v, " +
                "pc.k as pc_k, pc.v as pc_v " +
                "from pages p " +
                "left join page_configuration pc on p.id = pc.page_id " +
                "left join page_metadata pm on p.id = pm.page_id ";
            StringJoiner where = new StringJoiner(" and ", " ", " ");
            List<Object> params = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getHomepage() != null) {
                    where.add("p.homepage = ?");
                    params.add(criteria.getHomepage());
                }
                if (criteria.getReferenceId() != null) {
                    where.add("p.reference_id = ?");
                    params.add(criteria.getReferenceId());
                }
                if (criteria.getReferenceType() != null) {
                    where.add("p.reference_type = ?");
                    params.add(criteria.getReferenceType());
                }
                if (criteria.getPublished() != null) {
                    where.add("p.published = ?");
                    params.add(criteria.getPublished());
                }
                if (criteria.getName() != null) {
                    where.add("p.name = ?");
                    params.add(criteria.getName());
                }
                if (criteria.getParent() != null) {
                    where.add("p.parent_id = ?");
                    params.add(criteria.getParent());
                }
                if (criteria.getRootParent() != null && criteria.getRootParent().equals(Boolean.TRUE)) {
                    where.add("p.parent_id is null");
                }
                if (criteria.getType() != null) {
                    where.add("p.type = ?");
                    params.add(criteria.getType());
                }
                if (criteria.getUseAutoFetch() != null) {
                    where.add("p.use_auto_fetch = ?");
                    params.add(criteria.getUseAutoFetch().booleanValue());
                }
            }

            if (where.toString().trim().length() > 0) {
                select += " where ";
            }

            jdbcTemplate.query(select + where.toString() + "order by " + ESCAPED_ORDER_COLUMN_NAME, rowMapper, params.toArray());

            List<Page> items = rowMapper.getRows();
            for (Page page : items) {
                addExcludedGroups(page);
                addAttachedMedia(page);
            }
            return items;
        } catch (final Exception ex) {
            final String message = "Failed to find portal pages";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }
}
