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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.PageRevision;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcPageRevisionRepository extends JdbcAbstractFindAllRepository<PageRevision> implements PageRevisionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPageRevisionRepository.class);

    JdbcPageRevisionRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "page_revisions");
    }

    @Override
    protected JdbcObjectMapper<PageRevision> buildOrm() {
        return JdbcObjectMapper
            .builder(PageRevision.class, this.tableName, "page_id")
            .addColumn("page_id", Types.NVARCHAR, String.class)
            .addColumn("revision", Types.INTEGER, int.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("content", Types.NVARCHAR, String.class)
            .addColumn("hash", Types.NVARCHAR, String.class)
            .addColumn("contributor", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Page<PageRevision> findAll(Pageable pageable) throws TechnicalException {
        String rowCountSql = "SELECT count(1) AS row_count FROM " + this.tableName;
        int total = jdbcTemplate.queryForObject(rowCountSql, (rs, rowNum) -> rs.getInt(1));

        String querySql =
            getOrm().getSelectAllSql() + " ORDER BY page_id, revision " + createPagingClause(pageable.pageSize(), pageable.from());
        List<PageRevision> revisions = jdbcTemplate.query(querySql, getOrm().getRowMapper());

        return new Page<>(revisions, pageable.pageNumber(), revisions.size(), total);
    }

    @Override
    public Optional<PageRevision> findById(String pageId, int revision) throws TechnicalException {
        LOGGER.debug("JdbcPageRevisionRepository.findById({}, {})", pageId, revision);
        try {
            final List<PageRevision> items = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where page_id = ? and revision = ?",
                getOrm().getRowMapper(),
                pageId,
                revision
            );
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find page revision by id", ex);
            throw new TechnicalException("Failed to find page revision by id", ex);
        }
    }

    @Override
    public PageRevision create(PageRevision item) throws TechnicalException {
        LOGGER.debug("JdbcPageRevisionRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            return findById(item.getPageId(), item.getRevision()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create page revision", ex);
            throw new TechnicalException("Failed to create page revision", ex);
        }
    }

    @Override
    public List<PageRevision> findAllByPageId(String pageId) throws TechnicalException {
        LOGGER.debug("JdbcPageRevisionRepository.findAllByPageId({})", pageId);
        try {
            List<PageRevision> result = jdbcTemplate.query(
                "select p.* from " + this.tableName + " p where p.page_id = ? order by revision desc",
                getOrm().getRowMapper(),
                pageId
            );
            LOGGER.debug("JdbcPageRevisionRepository.findLastByPageId({}) = {}", pageId, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find revisions by page id: {}", pageId, ex);
            throw new TechnicalException("Failed to find revisions by page id", ex);
        }
    }

    @Override
    public Optional<PageRevision> findLastByPageId(String pageId) throws TechnicalException {
        LOGGER.debug("JdbcPageRevisionRepository.findLastByPageId({})", pageId);
        try {
            List<PageRevision> rows = jdbcTemplate.query(
                "select p.* from " + this.tableName + " p where p.page_id = ? order by revision desc " + createPagingClause(1, 0),
                getOrm().getRowMapper(),
                pageId
            );
            Optional<PageRevision> result = rows.stream().findFirst();
            LOGGER.debug("JdbcPageRevisionRepository.findLastByPageId({}) = {}", pageId, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find last revision by page id: {}", pageId, ex);
            throw new TechnicalException("Failed to find last revision by page id", ex);
        }
    }

    @Override
<<<<<<< HEAD
    public List<String> deleteByPageId(String pageId) throws TechnicalException {
        LOGGER.debug("JdbcPageRevisionRepository.deleteByPageId({})", pageId);
        try {
            List<PageRevision> result = jdbcTemplate.query(
                "select p.* from " + this.tableName + " p where p.page_id = ? order by revision desc",
                getOrm().getRowMapper(),
                pageId
            );

            if (!result.isEmpty()) {
                jdbcTemplate.update("delete from " + this.tableName + " where page_id = ?", pageId);
            }

            LOGGER.debug("JdbcPageRevisionRepository.delete({}) - Done", pageId);
            return result.stream().map(p -> p.getPageId() + ":" + p.getRevision()).toList();
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete page revision by id: {}", pageId, ex);
            throw new TechnicalException("Failed to delete page revision by page id", ex);
=======
    public void deleteAllByPageId(String pageId) throws TechnicalException {
        LOGGER.debug("JdbcPageRevisionRepository.deleteAllByPageId({})", pageId);
        try {
            String sql = "DELETE FROM " + this.tableName + " WHERE page_id = ?";
            int rowsAffected = jdbcTemplate.update(sql, pageId);
            LOGGER.debug("JdbcPageRevisionRepository.deleteAllByPageId({}) = {} rows deleted", pageId, rowsAffected);

            if (rowsAffected == 0) {
                LOGGER.warn("No revisions found for page id: {}", pageId);
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete revisions by page id: {}", pageId, ex);
            throw new TechnicalException("Failed to delete revisions by page id", ex);
>>>>>>> 9fa0033e0f (fix: delete page revisions when api is deleted)
        }
    }
}
