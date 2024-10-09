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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createPagingClause;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.management.JdbcHelper.AND_CLAUSE;
import static io.gravitee.repository.jdbc.management.JdbcHelper.WHERE_CLAUSE;
import static io.gravitee.repository.jdbc.orm.JdbcColumn.getDBName;
import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcColumn;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final String PAGE_ATTACHED_MEDIA;
    private final String PAGE_CONFIGURATION;
    private final String PAGE_METADATA;
    private final String PAGE_ACL;

    JdbcPageRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "pages");
        PAGE_ATTACHED_MEDIA = getTableNameFor("page_attached_media");
        PAGE_CONFIGURATION = getTableNameFor("page_configuration");
        PAGE_METADATA = getTableNameFor("page_metadata");
        PAGE_ACL = getTableNameFor("page_acl");
    }

    @Override
    protected JdbcObjectMapper<Page> buildOrm() {
        return JdbcObjectMapper
            .builder(Page.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("cross_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, PageReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("content", Types.NVARCHAR, String.class)
            .addColumn("last_contributor", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, int.class)
            .addColumn("published", Types.BOOLEAN, boolean.class)
            .addColumn("homepage", Types.BOOLEAN, boolean.class)
            .addColumn("visibility", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("parent_id", Types.NVARCHAR, String.class)
            .addColumn("use_auto_fetch", Types.BOOLEAN, Boolean.class)
            .addColumn("excluded_access_controls", Types.BOOLEAN, boolean.class)
            .build();
    }

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
            getOrm().setFromResultSet(page, rs);
            String sourceType = rs.getString("source_type");
            String sourceConfiguration = rs.getString("source_configuration");
            if ((sourceType != null) || (sourceConfiguration != null)) {
                PageSource pageSource = new PageSource();
                pageSource.setType(sourceType);
                pageSource.setConfiguration(sourceConfiguration);
                page.setSource(pageSource);
            }
            addAttachedMedia(page);
            addAccessControls(page);
            return page;
        }
    }

    private final Rm mapper = new Rm();

    private static class Psc implements PreparedStatementCreator {

        private final String sql;
        private final Page page;
        private JdbcObjectMapper<Page> orm;
        private final Object[] ids;

        public Psc(String sql, Page page, JdbcObjectMapper<Page> orm, Object... ids) {
            this.sql = sql;
            this.page = page;
            this.orm = orm;
            this.ids = ids;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection cnctn) throws SQLException {
            LOGGER.debug("SQL: {}", sql);
            LOGGER.debug("page: {}", page);
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = orm.setStatementValues(stmt, page, 1);
            stmt.setString(idx++, page.getSource() == null ? null : page.getSource().getType());
            stmt.setString(idx++, page.getSource() == null ? null : page.getSource().getConfiguration());

            for (Object id : ids) {
                stmt.setObject(idx++, id);
            }
            return stmt;
        }
    }

    private String buildInsertStatement() {
        final StringBuilder builder = new StringBuilder("insert into " + this.tableName + " (");
        boolean first = true;
        for (JdbcColumn column : getOrm().getColumns()) {
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
        for (int i = 0; i < getOrm().getColumns().size(); i++) {
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

    private final String INSERT_SQL = buildInsertStatement();

    private String buildUpdateStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("update " + this.tableName + " set ");
        boolean first = true;
        for (JdbcColumn column : getOrm().getColumns()) {
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

    private final String UPDATE_SQL = buildUpdateStatement();

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
        return new Psc(UPDATE_SQL, page, getOrm(), page.getId());
    }

    @Override
    protected PreparedStatementCreator buildInsertPreparedStatementCreator(Page page) {
        return new Psc(INSERT_SQL, page, getOrm());
    }

    private void addAccessControls(Page page) {
        Set<AccessControl> accessControls = getAccessControls(page.getId());
        page.setAccessControls(accessControls);
    }

    private Set<AccessControl> getAccessControls(String pageId) {
        return new HashSet<>(
            jdbcTemplate.query(
                "select reference_id, reference_type from " + PAGE_ACL + " where page_id = ?",
                (ResultSet rs, int rowNum) -> new AccessControl(rs.getString(1), rs.getString(2)),
                pageId
            )
        );
    }

    private void addAttachedMedia(Page page) {
        List<PageMedia> attachedMedia = getAttachedMedia(page.getId());
        page.setAttachedMedia(attachedMedia);
    }

    private List<PageMedia> getAttachedMedia(String pageId) {
        return jdbcTemplate.query(
            "select media_hash, media_name, attached_at from " + PAGE_ATTACHED_MEDIA + " where page_id = ?",
            (ResultSet rs, int rowNum) -> new PageMedia(rs.getString(1), rs.getString(2), rs.getTimestamp(3)),
            pageId
        );
    }

    private void storeAttachedMedia(Page page, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + PAGE_ATTACHED_MEDIA + " where page_id = ?", page.getId());
        }
        final List<PageMedia> attachedMedia = page.getAttachedMedia();
        if (attachedMedia != null && !attachedMedia.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + PAGE_ATTACHED_MEDIA + " ( page_id, media_hash, media_name, attached_at ) values ( ?, ?, ?, ? )",
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

    private void storeAccessControls(Page page, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + PAGE_ACL + " where page_id = ?", page.getId());
        }
        if (page.getAccessControls() != null && !page.getAccessControls().isEmpty()) {
            Iterator<AccessControl> iterator = page.getAccessControls().iterator();
            jdbcTemplate.batchUpdate(
                "insert into " + PAGE_ACL + " ( page_id, reference_id, reference_type ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        AccessControl accessControl = iterator.next();
                        ps.setString(1, page.getId());
                        ps.setString(2, accessControl.getReferenceId());
                        ps.setString(3, accessControl.getReferenceType());
                    }

                    @Override
                    public int getBatchSize() {
                        return page.getAccessControls().size();
                    }
                }
            );
        }
    }

    private void storeConfiguration(Page page, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + PAGE_CONFIGURATION + " where page_id = ?", page.getId());
        }
        if (page.getConfiguration() != null && !page.getConfiguration().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(page.getConfiguration().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into " + PAGE_CONFIGURATION + " ( page_id, k, v ) values ( ?, ?, ? )",
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
            jdbcTemplate.update("delete from " + PAGE_METADATA + " where page_id = ?", page.getId());
        }
        if (page.getMetadata() != null && !page.getMetadata().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(page.getMetadata().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into " + PAGE_METADATA + " ( page_id, k, v ) values ( ?, ?, ? )",
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
                "from " +
                this.tableName +
                " p " +
                "left join " +
                PAGE_CONFIGURATION +
                " pc on p.id = pc.page_id " +
                "left join " +
                PAGE_METADATA +
                " pm on p.id = pm.page_id " +
                "where p.id = ?",
                rowMapper,
                id
            );
            Optional<Page> result = rowMapper.getRows().stream().findFirst();
            result.ifPresent(page -> {
                this.addAttachedMedia(page);
                this.addAccessControls(page);
            });
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
            Integer totalPages = jdbcTemplate.queryForObject("select count(*) from " + this.tableName + " p", Integer.class);
            jdbcTemplate.query(
                "select p.*, " +
                "pm.k as pm_k, pm.v as pm_v, " +
                "pc.k as pc_k, pc.v as pc_v " +
                "from ( " +
                getOrm().getSelectAllSql() +
                " ORDER BY id " +
                createPagingClause(pageable.pageSize(), pageable.from()) +
                ") as p " +
                "left join " +
                PAGE_CONFIGURATION +
                " pc on p.id = pc.page_id " +
                "left join " +
                PAGE_METADATA +
                " pm on p.id = pm.page_id",
                rowMapper
            );
            List<Page> result = rowMapper
                .getRows()
                .stream()
                .limit(pageable.pageSize())
                .map(p -> {
                    addAccessControls(p);
                    addAttachedMedia(p);
                    return p;
                })
                .collect(Collectors.toList());
            LOGGER.debug("JdbcPageRepository.findAll() = {} result", result.size());
            return new io.gravitee.common.data.domain.Page<>(result, pageable.pageNumber(), result.size(), totalPages);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find page by id:", ex);
            throw new TechnicalException("Failed to find page by id", ex);
        }
    }

    @Override
    public long countByParentIdAndIsPublished(String parentId) throws TechnicalException {
        try {
            final List<Object> args = new ArrayList<>();

            String builder =
                "select count(*) from " + this.tableName + " p " + WHERE_CLAUSE + "parent_id = ?" + AND_CLAUSE + "published = ?";
            args.add(parentId);
            args.add(true);

            LOGGER.debug("SQL: {}", builder);
            LOGGER.debug("Args: {}", args);
            return jdbcTemplate.queryForObject(builder, args.toArray(), Long.class);
        } catch (final Exception e) {
            LOGGER.error("Failed to count page by parent_id:", e);
            throw new TechnicalException("Failed to count page by parentId", e);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + PAGE_CONFIGURATION + " where page_id = ?", id);
        jdbcTemplate.update("delete from " + PAGE_METADATA + " where page_id = ?", id);
        jdbcTemplate.update("delete from " + PAGE_ACL + " where page_id = ?", id);
        jdbcTemplate.update(getOrm().getDeleteSql(), id);
    }

    @Override
    public void unsetHomepage(Collection<String> ids) {
        jdbcTemplate.update(
            "update from " + this.tableName + " where page_id  in (" + getOrm().buildInClause(ids) + ") set homepage = false",
            ids.toArray()
        );
    }

    @Override
    public Page create(Page item) throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.create({})", item);
        try {
            jdbcTemplate.update(buildInsertPreparedStatementCreator(item));
            storeAttachedMedia(item, false);
            storeConfiguration(item, false);
            storeMetadata(item, false);
            storeAccessControls(item, false);
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
            storeAttachedMedia(page, true);
            storeConfiguration(page, true);
            storeMetadata(page, true);
            storeAccessControls(page, true);
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
                "select max(" + ESCAPED_ORDER_COLUMN_NAME + ") from " + this.tableName + " where reference_type = ? and reference_id = ? ",
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
    public Map<String, List<String>> deleteByReferenceIdAndReferenceType(String referenceId, PageReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcPageRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceType, referenceId);
        try {
            final var pageIds = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ?",
                String.class,
                referenceId,
                referenceType.name()
            );

            final var pageIdAndMedias = pageIds
                .stream()
                .collect(
                    Collectors.toMap(pageId -> pageId, pageId -> getAttachedMedia(pageId).stream().map(PageMedia::getMediaHash).toList())
                );

            if (!pageIds.isEmpty()) {
                String inClause = getOrm().buildInClause(pageIds);
                jdbcTemplate.update("delete from " + PAGE_ACL + " where page_id IN (" + inClause + ")", pageIds.toArray());
                jdbcTemplate.update("delete from " + PAGE_ATTACHED_MEDIA + " where page_id IN (" + inClause + ")", pageIds.toArray());
                jdbcTemplate.update("delete from " + PAGE_CONFIGURATION + " where page_id IN (" + inClause + ")", pageIds.toArray());
                jdbcTemplate.update("delete from " + PAGE_METADATA + " where page_id IN (" + inClause + ")", pageIds.toArray());
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }

            LOGGER.debug("JdbcPageRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done", referenceType, referenceId);
            return pageIdAndMedias;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete page for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete page by reference", ex);
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
                "from " +
                this.tableName +
                " p " +
                "left join " +
                PAGE_CONFIGURATION +
                " pc on p.id = pc.page_id " +
                "left join " +
                PAGE_METADATA +
                " pm on p.id = pm.page_id ";
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
                if (criteria.getVisibility() != null) {
                    where.add("p.visibility = ?");
                    params.add(criteria.getVisibility());
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
                    params.add(criteria.getUseAutoFetch());
                }
            }

            if (where.toString().trim().length() > 0) {
                select += " where ";
            }

            final String sql =
                select + where.toString() + "order by " + ESCAPED_ORDER_COLUMN_NAME + ", p.id, p.reference_id, p.reference_type";
            jdbcTemplate.query(sql, rowMapper, params.toArray());

            List<Page> items = rowMapper.getRows();
            for (Page page : items) {
                addAttachedMedia(page);
                addAccessControls(page);
            }
            return items;
        } catch (final Exception ex) {
            final String message = "Failed to find portal pages";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }
}
