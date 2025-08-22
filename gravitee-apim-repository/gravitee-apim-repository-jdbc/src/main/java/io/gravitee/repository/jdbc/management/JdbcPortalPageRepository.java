package io.gravitee.repository.jdbc.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcPortalPageRepository implements PortalPageRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPortalPageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<PortalPage> PAGE_ROW_MAPPER = new RowMapper<>() {
        @Override
        public PortalPage mapRow(ResultSet rs, int rowNum) throws SQLException {
            PortalPage page = new PortalPage();
            page.setId(rs.getString("id"));
            page.setContent(rs.getString("content"));
            return page;
        }
    };

    @Override
    public PortalPage create(PortalPage page) throws TechnicalException {
        try {
            jdbcTemplate.update("INSERT INTO portal_pages (id, content) VALUES (?, ?)", page.getId(), page.getContent());
            return page;
        } catch (Exception e) {
            throw new TechnicalException("Failed to create portal page", e);
        }
    }

    @Override
    public PortalPage findById(String id) throws TechnicalException {
        try {
            PortalPage page = jdbcTemplate.queryForObject("SELECT * FROM portal_pages WHERE id = ?", PAGE_ROW_MAPPER, id);
            if (page != null) {
                page.setContexts(findContextsByPageId(id));
            }
            return page;
        } catch (Exception e) {
            throw new TechnicalException("Failed to find portal page by id", e);
        }
    }

    @Override
    public List<PortalPage> findAll() throws TechnicalException {
        try {
            List<PortalPage> pages = jdbcTemplate.query("SELECT * FROM portal_pages", PAGE_ROW_MAPPER);
            for (PortalPage page : pages) {
                page.setContexts(findContextsByPageId(page.getId()));
            }
            return pages;
        } catch (Exception e) {
            throw new TechnicalException("Failed to find all portal pages", e);
        }
    }

    @Override
    public PortalPage update(PortalPage page) throws TechnicalException {
        try {
            jdbcTemplate.update("UPDATE portal_pages SET content = ? WHERE id = ?", page.getContent(), page.getId());
            return page;
        } catch (Exception e) {
            throw new TechnicalException("Failed to update portal page", e);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            jdbcTemplate.update("DELETE FROM portal_page_contexts WHERE page_id = ?", id);
            jdbcTemplate.update("DELETE FROM portal_pages WHERE id = ?", id);
        } catch (Exception e) {
            throw new TechnicalException("Failed to delete portal page", e);
        }
    }

    @Override
    public void assignContext(String pageId, String context) throws TechnicalException {
        try {
            jdbcTemplate.update("INSERT INTO portal_page_contexts (page_id, context) VALUES (?, ?)", pageId, context);
        } catch (Exception e) {
            throw new TechnicalException("Failed to assign context to portal page", e);
        }
    }

    @Override
    public void removeContext(String pageId, String context) throws TechnicalException {
        try {
            jdbcTemplate.update("DELETE FROM portal_page_contexts WHERE page_id = ? AND context = ?", pageId, context);
        } catch (Exception e) {
            throw new TechnicalException("Failed to remove context from portal page", e);
        }
    }

    @Override
    public List<PortalPage> findByContext(String context) throws TechnicalException {
        try {
            List<String> pageIds = jdbcTemplate.query("SELECT page_id FROM portal_page_contexts WHERE context = ?", (rs, rowNum) -> rs.getString("page_id"), context);
            if (pageIds.isEmpty()) return List.of();
            String inSql = String.join(",", pageIds.stream().map(id -> "'" + id + "'").toList());
            List<PortalPage> pages = jdbcTemplate.query("SELECT * FROM portal_pages WHERE id IN (" + inSql + ")", PAGE_ROW_MAPPER);
            for (PortalPage page : pages) {
                page.setContexts(findContextsByPageId(page.getId()));
            }
            return pages;
        } catch (Exception e) {
            throw new TechnicalException("Failed to find portal pages by context", e);
        }
    }

    private List<String> findContextsByPageId(String pageId) {
        return jdbcTemplate.query("SELECT context FROM portal_page_contexts WHERE page_id = ?", (rs, rowNum) -> rs.getString("context"), pageId);
    }
}
