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
package io.gravitee.rest.api.service.impl.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.search.model.IndexablePage;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.MessageRecipient;
import io.gravitee.rest.api.model.ApiPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.command.CommandSearchIndexerEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.model.command.NewCommandEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.CommandService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.AbstractNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.SearchEngineIndexer;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SearchEngineServiceImpl implements SearchEngineService {

    private static final String ACTION_INDEX = "I";
    private static final String ACTION_DELETE = "D";
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SearchEngineServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private SearchEngineIndexer indexer;

    @Autowired
    private Collection<DocumentTransformer> transformers;

    @Autowired
    private Collection<DocumentSearcher> searchers;

    @Autowired
    @Lazy
    private CommandService commandService;

    @Autowired
    @Lazy
    private ApiMetadataService apiMetadataService;

    @Autowired
    @Lazy
    private ApiSearchService apiSearchService;

    @Autowired
    @Lazy
    private PageService pageService;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    private PageCrudService pageCrudService;

    @Autowired
    private ApiCrudService apiCrudService;

    @Autowired
    private ApiIndexerDomainService apiIndexerDomainService;

    @Async("indexerThreadPoolTaskExecutor")
    @Override
    public void index(ExecutionContext executionContext, Indexable source, boolean locally, boolean commit) {
        indexLocally(source, commit);

        if (!locally) {
            CommandSearchIndexerEntity content = new CommandSearchIndexerEntity();
            content.setAction(ACTION_INDEX);
            content.setId(source.getId());
            content.setClazz(source.getClass().getName());

            sendCommands(executionContext, content);
        }
    }

    @Async("indexerThreadPoolTaskExecutor")
    @Override
    public void delete(ExecutionContext executionContext, Indexable source, boolean locally) {
        if (locally) {
            deleteLocally(source);
        } else {
            CommandSearchIndexerEntity content = new CommandSearchIndexerEntity();
            content.setAction(ACTION_DELETE);
            content.setId(source.getId());
            content.setClazz(source.getClass().getName());

            sendCommands(executionContext, content);
        }
    }

    @Override
    public void commit() {
        try {
            indexer.commit();
        } catch (TechnicalException te) {
            logger.error("Unexpected error while Lucene commit", te);
        }
    }

    @Override
    public void process(ExecutionContext executionContext, CommandSearchIndexerEntity content) {
        if (ACTION_DELETE.equals(content.getAction())) {
            try {
                Indexable source = createInstance(content.getClazz());
                source.setId(content.getId());
                deleteLocally(source);
            } catch (Exception ex) {
                throw new TechnicalManagementException(
                    "Unable to delete document for content [ " + content.getId() + " - " + content.getClazz() + " ]",
                    ex
                );
            }
        } else if (ACTION_INDEX.equals(content.getAction())) {
            Indexable source = getSource(executionContext, content.getClazz(), content.getId());
            if (source != null) {
                indexLocally(source, true);
            }
        }
    }

    private void sendCommands(final ExecutionContext executionContext, CommandSearchIndexerEntity content) {
        try {
            NewCommandEntity msg = new NewCommandEntity();
            msg.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
            msg.setTo(MessageRecipient.MANAGEMENT_APIS.name());
            msg.setTtlInSeconds(60);
            msg.setContent(mapper.writeValueAsString(content));
            commandService.send(executionContext, msg);
        } catch (JsonProcessingException e) {
            logger.error("Unexpected error while sending a message", e);
        }
    }

    private Indexable getSource(final ExecutionContext executionContext, String clazz, String id) {
        try {
            if (ApiEntity.class.getName().equals(clazz) || io.gravitee.rest.api.model.v4.api.ApiEntity.class.getName().equals(clazz)) {
                GenericApiEntity genericApi = apiSearchService.findGenericById(executionContext, id);
                return apiMetadataService.fetchMetadataForApi(executionContext, genericApi);
            } else if (PageEntity.class.getName().equals(clazz) || ApiPageEntity.class.getName().equals(clazz)) {
                return pageService.findById(id);
            } else if (UserEntity.class.getName().equals(clazz)) {
                return userService.findById(executionContext, id);
            } else if (IndexablePage.class.getName().equals(clazz)) {
                return new IndexablePage(pageCrudService.get(id));
            } else if (IndexableApi.class.getName().equals(clazz)) {
                var api = apiCrudService.get(id);
                return apiIndexerDomainService.toIndexableApi(
                    new Indexer.IndexationContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                    api
                );
            }
        } catch (final AbstractNotFoundException nfe) {
            // ignore not found exception because may be due to synchronization not yet processed by DBs
        }
        return null;
    }

    private void indexLocally(Indexable source, boolean commit) {
        transformers
            .stream()
            .filter(transformer -> transformer.handle(source.getClass()))
            .findFirst()
            .ifPresent(transformer -> {
                try {
                    indexer.index(transformer.transform(source), commit);
                } catch (TechnicalException te) {
                    logger.error("Unexpected error while indexing a document", te);
                }
            });
    }

    private void deleteLocally(Indexable source) {
        transformers
            .stream()
            .filter(transformer -> transformer.handle(source.getClass()))
            .findFirst()
            .ifPresent(transformer -> {
                try {
                    indexer.remove(transformer.transform(source));
                } catch (TechnicalException te) {
                    logger.error("Unexpected error while deleting a document", te);
                }
            });
    }

    private Indexable createInstance(String className) throws Exception {
        try {
            final Class<?> clazz = Class.forName(className);
            Assert.isAssignable(Indexable.class, clazz);
            return (Indexable) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            logger.error("Unable to instantiate class: {}", className, ex);
            throw ex;
        }
    }

    @Override
    public SearchResult search(final ExecutionContext executionContext, Query<? extends Indexable> query) {
        Optional<SearchResult> results = searchers
            .stream()
            .filter(searcher -> searcher.handle(query.getRoot()))
            .findFirst()
            .flatMap(searcher -> {
                try {
                    if (searcher instanceof ApiDocumentSearcher) {
                        Optional<DocumentSearcher> pageDocumentSearcher = searchers
                            .stream()
                            .filter(s -> s.handle(PageEntity.class))
                            .findFirst();
                        if (pageDocumentSearcher.isPresent()) {
                            SearchResult apiReferences = pageDocumentSearcher.get().searchReference(executionContext, query);
                            if (!apiReferences.getDocuments().isEmpty()) {
                                query.setIds(apiReferences.getDocuments());
                            }
                        }
                    }
                    return Optional.of(searcher.search(executionContext, query));
                } catch (TechnicalException te) {
                    logger.error("Unexpected error while searching a document", te);
                    return Optional.empty();
                }
            });

        return results.orElse(null);
    }
}
