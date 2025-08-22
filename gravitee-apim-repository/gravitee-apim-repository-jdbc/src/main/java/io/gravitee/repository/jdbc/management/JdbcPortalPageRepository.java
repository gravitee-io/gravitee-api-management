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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.management.model.PortalPageContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPortalPageRepository extends JdbcAbstractCrudRepository<PortalPage, String> implements PortalPageRepository {

    private final JdbcPortalPageContextRepository contextRepository;

    public JdbcPortalPageRepository(
        JdbcPortalPageContextRepository contextRepository,
        @Value("${management.jdbc.prefix:}") String tablePrefix
    ) {
        super(tablePrefix, "portal_pages");
        this.contextRepository = contextRepository;
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
    protected JdbcObjectMapper<PortalPage> buildOrm() {
        return JdbcObjectMapper
            .builder(PortalPage.class, this.tableName, "id")
            .addColumn("id", java.sql.Types.NVARCHAR, String.class)
            .addColumn("content", java.sql.Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    protected String getId(PortalPage item) {
        return item.getId();
    }

    @Override
    public Optional<PortalPage> findById(String id) throws TechnicalException {
        Optional<PortalPage> pageOpt = super.findById(id);
        if (pageOpt.isPresent()) {
            PortalPage page = pageOpt.get();
            page.setContexts(findContextsByPageId(page.getId()));
        }
        return pageOpt;
    }

    @Override
    public Set<PortalPage> findAll() throws TechnicalException {
        Set<PortalPage> pages = super.findAll();
        for (PortalPage page : pages) {
            page.setContexts(findContextsByPageId(page.getId()));
        }
        return pages;
    }

    @Override
    public PortalPage create(PortalPage page) throws TechnicalException {
        PortalPage created = super.create(page);
        for (String context : page.getContexts()) {
            PortalPageContext pageContext = new PortalPageContext();
            pageContext.setPageId(created.getId());
            pageContext.setContext(context);
            contextRepository.create(pageContext);
        }
        return created;
    }

    @Override
    public PortalPage update(PortalPage page) throws TechnicalException {
        PortalPage updated = super.update(page);
        var existingContexts = contextRepository.findAllByPageId(page.getId());
        for (String existingContext : existingContexts) {
            if (!page.getContexts().contains(existingContext)) {
                contextRepository.deleteByPageIdAndContext(page.getId(), existingContext);
            }
        }
        for (String newContext : page.getContexts()) {
            if (!existingContexts.contains(newContext)) {
                PortalPageContext pageContext = new PortalPageContext();
                pageContext.setPageId(updated.getId());
                pageContext.setContext(newContext);
                contextRepository.create(pageContext);
            }
        }
        return updated;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        Set<String> contexts = contextRepository.findAllByPageId(id);
        for (String ctx : contexts) {
            contextRepository.deleteByPageIdAndContext(id, ctx);
        }
        super.delete(id);
    }

    @Override
    public void assignContext(String pageId, String context) throws TechnicalException {
        try {
            PortalPageContext ctx = new PortalPageContext();
            ctx.setPageId(pageId);
            ctx.setContext(context);
            contextRepository.create(ctx);
        } catch (Exception e) {
            throw new TechnicalException("Failed to assign context to portal page", e);
        }
    }

    public List<String> findContextsByPageId(String pageId) throws TechnicalException {
        return contextRepository
            .findAll()
            .stream()
            .filter(ctx -> ctx.getPageId().equals(pageId))
            .map(PortalPageContext::getContext)
            .toList();
    }

    @Override
    public void removeContext(String pageId, String context) throws TechnicalException {
        try {
            List<PortalPageContext> contexts = contextRepository
                .findAll()
                .stream()
                .filter(ctx -> ctx.getPageId().equals(pageId) && ctx.getContext().equals(context))
                .toList();
            for (PortalPageContext ctx : contexts) {
                contextRepository.delete(ctx.getPageId());
            }
        } catch (Exception e) {
            throw new TechnicalException("Failed to remove context from portal page", e);
        }
    }

    @Override
    public List<PortalPage> findByContext(String context) throws TechnicalException {
        try {
            List<PortalPageContext> contexts = contextRepository
                .findAll()
                .stream()
                .filter(ctx -> ctx.getContext().equals(context))
                .toList();
            List<String> pageIds = contexts.stream().map(PortalPageContext::getPageId).toList();
            if (pageIds.isEmpty()) return List.of();
            String inSql = String.join(",", pageIds.stream().map(id -> "'" + id + "'").toList());
            List<PortalPage> pages = jdbcTemplate.query("SELECT * FROM " + tableName + " WHERE id IN (" + inSql + ")", PAGE_ROW_MAPPER);
            for (PortalPage page : pages) {
                page.setContexts(findContextsByPageId(page.getId()));
            }
            return pages;
        } catch (Exception e) {
            throw new TechnicalException("Failed to find portal pages by context", e);
        }
    }
}
