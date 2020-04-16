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
package io.gravitee.management.service.impl.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.management.idp.api.IdentityProvider;
import io.gravitee.management.model.ApiPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.command.CommandSearchIndexerEntity;
import io.gravitee.management.model.command.CommandTags;
import io.gravitee.management.model.command.NewCommandEntity;
import io.gravitee.management.model.search.Indexable;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.CommandService;
import io.gravitee.management.service.PageService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.management.service.impl.search.lucene.DocumentTransformer;
import io.gravitee.management.service.impl.search.lucene.SearchEngineIndexer;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.MessageRecipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SearchEngineServiceImpl implements SearchEngineService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SearchEngineServiceImpl.class);

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
    private ApiService apiService;

    @Autowired
    @Lazy
    private PageService pageService;

    @Autowired
    @Lazy
    private UserService userService;

    private ObjectMapper mapper = new ObjectMapper();

    private static final String ACTION_INDEX = "I";
    private static final String ACTION_DELETE = "D";

    @Async
    @Override
    public void index(Indexable source, boolean locally) {
        if (locally) {
            indexLocally(source);
        } else {
            CommandSearchIndexerEntity content = new CommandSearchIndexerEntity();
            content.setAction(ACTION_INDEX);
            content.setId(source.getId());
            content.setClazz(source.getClass().getName());

            sendCommands(content);
        }
    }

    @Async
    @Override
    public void delete(Indexable source, boolean locally) {
        if (locally) {
            deleteLocally(source);
        } else {
            CommandSearchIndexerEntity content = new CommandSearchIndexerEntity();
            content.setAction(ACTION_DELETE);
            content.setId(source.getId());
            content.setClazz(source.getClass().getName());

            sendCommands(content);
        }
    }

    @Override
    public void process(CommandSearchIndexerEntity content) {
        if (ACTION_DELETE.equals(content.getAction())) {
            try {
                Indexable source = createInstance(content.getClazz());
                source.setId(content.getId());
                deleteLocally(source);
            } catch (Exception ex) {
                throw new TechnicalManagementException("Unable to delete document for content [ " + content.getId() + " - " + content.getClazz() + " ]", ex);
            }
        } else if (ACTION_INDEX.equals(content.getAction())) {
            Indexable source = getSource(content.getClazz(), content.getId());
            if (source == null) {
                logger.error("Unable to get source from message content [{}]", content);
                throw new TechnicalManagementException("Unable to get source from message content [" + content + "]");
            }
            indexLocally(source);
        }
    }

    @Override
    public SearchResult search(io.gravitee.management.service.search.query.Query<? extends Indexable> query) {
        Optional<SearchResult> results = searchers.stream()
                .filter(searcher -> searcher.handle(query.getRoot()))
                .findFirst()
                .flatMap(searcher -> {
                    try {
                        return Optional.of(searcher.search(query));
                    } catch (TechnicalException te) {
                        logger.error("Unexpected error while deleting a document", te);
                        return Optional.empty();
                    }
                });

        return results.get();
    }

    private void sendCommands(CommandSearchIndexerEntity content) {
        try {
            NewCommandEntity msg = new NewCommandEntity();
            msg.setTags(Collections.singletonList(CommandTags.DATA_TO_INDEX));
            msg.setTo(MessageRecipient.MANAGEMENT_APIS.name());
            msg.setTtlInSeconds(60);
            msg.setContent(mapper.writeValueAsString(content));
            commandService.send(msg);
        } catch (JsonProcessingException e) {
            logger.error("Unexpected error while sending a message", e);
        }
    }

    private Indexable getSource(String clazz, String id) {
        if (ApiEntity.class.getName().equals(clazz)) {
            return apiService.findById(id);
        } else if (PageEntity.class.getName().equals(clazz) || ApiPageEntity.class.getName().equals(clazz)) {
            return pageService.findById(id);
        } else if (UserEntity.class.getName().equals(clazz)) {
            return userService.findById(id);
        }
        return null;
    }

    private void indexLocally(Indexable source) {
        transformers.stream()
                .filter(transformer -> transformer.handle(source.getClass()))
                .findFirst()
                .ifPresent(transformer -> {
                    try {
                        indexer.index(transformer.transform(source));
                    } catch (TechnicalException te) {
                        logger.error("Unexpected error while indexing a document", te);
                    }
                });
    }

    private void deleteLocally(Indexable source) {
        transformers.stream()
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
            logger.error("Unable to instantiate class: {}", ex);
            throw ex;
        }
    }

}
