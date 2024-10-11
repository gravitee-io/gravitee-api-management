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
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.PageRevision;
import io.gravitee.repository.mongodb.management.internal.model.PageRevisionMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageRevisionPkMongo;
import io.gravitee.repository.mongodb.management.internal.page.revision.PageRevisionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoPageRevisionRepository implements PageRevisionRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoPageRevisionRepository.class);

    @Autowired
    private PageRevisionMongoRepository internalPageRevisionRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Page<PageRevision> findAll(Pageable pageable) throws TechnicalException {
        org.springframework.data.domain.Page<PageRevisionMongo> revisions = internalPageRevisionRepo.findAll(
            PageRequest.of(pageable.pageNumber(), pageable.pageSize())
        );
        return new Page<>(
            revisions.getContent().stream().map(page -> mapper.map(page)).collect(Collectors.toList()),
            pageable.pageNumber(),
            revisions.getNumberOfElements(),
            revisions.getTotalElements()
        );
    }

    @Override
    public Optional<PageRevision> findById(String pageId, int revision) throws TechnicalException {
        logger.debug("Find page revision by ID [{}]", pageId);

        PageRevisionMongo page = internalPageRevisionRepo.findById(new PageRevisionPkMongo(pageId, revision)).orElse(null);
        PageRevision res = mapper.map(page);

        logger.debug("Find page revision by ID [{}] - Done", pageId);
        return Optional.ofNullable(res);
    }

    @Override
    public PageRevision create(PageRevision page) throws TechnicalException {
        logger.debug("Create revision for page [{}]", page.getName());

        PageRevisionMongo pageMongo = mapper.map(page);
        PageRevisionMongo createdPageMongo = internalPageRevisionRepo.insert(pageMongo);

        PageRevision res = mapper.map(createdPageMongo);

        logger.debug("Create revision for page [{}] - Done", page.getName());

        return res;
    }

    @Override
    public List<PageRevision> findAllByPageId(String pageId) throws TechnicalException {
        try {
            return internalPageRevisionRepo.findAllByPageId(pageId).stream().map(rev -> mapper.map(rev)).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("An error occurred when querying all revisions for page [{}]", pageId, e);
            throw new TechnicalException("An error occurred when querying page revisions");
        }
    }

    @Override
    public Optional<PageRevision> findLastByPageId(String pageId) throws TechnicalException {
        try {
            return internalPageRevisionRepo.findLastByPageId(pageId).map(rev -> mapper.map(rev));
        } catch (Exception e) {
            logger.error("An error occurred when querying last revision for page [{}]", pageId, e);
            throw new TechnicalException("An error occurred when querying the last page revision");
        }
    }

    @Override
    public void deleteAllByPageId(String pageId) throws TechnicalException {
        try {
            internalPageRevisionRepo.deleteAllByPageId(pageId);
        } catch (Exception e) {
            logger.error("An error occurred when deleting revision for page [{}]", pageId, e);
            throw new TechnicalException("An error occurred when deleting the page revisions");
        }
    }

    @Override
    public Set<PageRevision> findAll() throws TechnicalException {
        return internalPageRevisionRepo
            .findAll()
            .stream()
            .map(pageRevisionMongo -> mapper.map(pageRevisionMongo))
            .collect(Collectors.toSet());
    }
}
