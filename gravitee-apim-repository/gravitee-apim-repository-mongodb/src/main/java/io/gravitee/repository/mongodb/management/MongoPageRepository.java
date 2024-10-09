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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.mongodb.management.internal.model.AccessControlMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageMediaMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageSourceMongo;
import io.gravitee.repository.mongodb.management.internal.page.PageMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Guillaume GILLON (guillaume.gillon@outlook.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoPageRepository implements PageRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoPageRepository.class);

    @Autowired
    private PageMongoRepository internalPageRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public List<Page> search(PageCriteria criteria) throws TechnicalException {
        logger.debug("Search Pages by criteria");
        List<PageMongo> results = internalPageRepo.search(criteria);
        logger.debug("Search Pages by criteria. Count={} - Done", results == null ? 0 : results.size());
        return mapper.mapPages(results);
    }

    @Override
    public Optional<Page> findById(String pageId) throws TechnicalException {
        logger.debug("Find page by ID [{}]", pageId);

        PageMongo page = internalPageRepo.findById(pageId).orElse(null);
        Page res = mapper.map(page);

        logger.debug("Find page by ID [{}] - Done", pageId);
        return Optional.ofNullable(res);
    }

    @Override
    public Page create(Page page) throws TechnicalException {
        logger.debug("Create page [{}]", page.getName());

        PageMongo pageMongo = mapper.map(page);
        PageMongo createdPageMongo = internalPageRepo.insert(pageMongo);

        Page res = mapper.map(createdPageMongo);

        logger.debug("Create page [{}] - Done", page.getName());

        return res;
    }

    @Override
    public Page update(Page page) throws TechnicalException {
        if (page == null) {
            throw new IllegalStateException("Page must not be null");
        }

        PageMongo pageMongo = internalPageRepo.findById(page.getId()).orElse(null);
        if (pageMongo == null) {
            throw new IllegalStateException(String.format("No page found with id [%s]", page.getId()));
        }

        try {
            pageMongo.setName(page.getName());
            pageMongo.setCrossId(page.getCrossId());
            pageMongo.setContent(page.getContent());
            pageMongo.setType(page.getType());
            pageMongo.setId(page.getId());
            pageMongo.setReferenceId(page.getReferenceId());
            pageMongo.setReferenceType(page.getReferenceType().name());
            pageMongo.setLastContributor(page.getLastContributor());
            pageMongo.setCreatedAt(page.getCreatedAt());
            pageMongo.setUpdatedAt(page.getUpdatedAt());
            pageMongo.setOrder(page.getOrder());
            pageMongo.setPublished(page.isPublished());
            pageMongo.setVisibility(page.getVisibility());
            pageMongo.setHomepage(page.isHomepage());
            pageMongo.setExcludedAccessControls(page.isExcludedAccessControls());
            pageMongo.setAccessControls(convertACL(page.getAccessControls()));
            if (page.getAttachedMedia() != null) {
                pageMongo.setAttachedMedia(convert(page.getAttachedMedia()));
            } else {
                pageMongo.setAttachedMedia(null);
            }
            pageMongo.setParentId(page.getParentId());
            if (page.getSource() != null) {
                pageMongo.setSource(convert(page.getSource()));
            } else {
                pageMongo.setSource(null);
            }
            pageMongo.setUseAutoFetch(page.getUseAutoFetch());
            pageMongo.setConfiguration(page.getConfiguration());
            pageMongo.setMetadata(page.getMetadata());

            PageMongo pageMongoUpdated = internalPageRepo.save(pageMongo);
            return mapper.map(pageMongoUpdated);
        } catch (Exception e) {
            logger.error("An error occurred when updating page", e);
            throw new TechnicalException("An error occurred when updating page");
        }
    }

    private Set<AccessControlMongo> convertACL(Set<AccessControl> accessControls) {
        if (accessControls != null) {
            return accessControls.stream().map(this::convert).collect(Collectors.toSet());
        }
        return null;
    }

    private AccessControlMongo convert(AccessControl accessControl) {
        AccessControlMongo accessControlMongo = new AccessControlMongo();
        accessControlMongo.setReferenceId(accessControl.getReferenceId());
        accessControlMongo.setReferenceType(accessControl.getReferenceType());
        return accessControlMongo;
    }

    @Override
    public void delete(String pageId) throws TechnicalException {
        try {
            internalPageRepo.deleteById(pageId);
        } catch (Exception e) {
            logger.error("An error occurred when deleting page [{}]", pageId, e);
            throw new TechnicalException("An error occurred when deleting page");
        }
    }

    @Override
    public void unsetHomepage(Collection<String> ids) {
        internalPageRepo.unsetHomepage(ids);
    }

    @Override
    public Integer findMaxPageReferenceIdAndReferenceTypeOrder(String referenceId, PageReferenceType referenceType)
        throws TechnicalException {
        try {
            return internalPageRepo.findMaxPageReferenceIdAndReferenceTypeOrder(referenceId, referenceType.name());
        } catch (Exception e) {
            logger.error("An error occurred when searching max order page for reference [{}, {}]", referenceId, referenceType, e);
            throw new TechnicalException("An error occurred when searching max order page for reference");
        }
    }

    @Override
    public io.gravitee.common.data.domain.Page<Page> findAll(Pageable pageable) throws TechnicalException {
        try {
            io.gravitee.common.data.domain.Page<PageMongo> page = internalPageRepo.findAll(pageable);
            List<Page> pageItems = mapper.mapPages(page.getContent());
            return new io.gravitee.common.data.domain.Page<>(pageItems, page.getPageNumber(), pageItems.size(), page.getTotalElements());
        } catch (Exception e) {
            logger.error("An error occurred when searching all pages", e);
            throw new TechnicalException("An error occurred when searching all pages");
        }
    }

    @Override
    public long countByParentIdAndIsPublished(String parentId) throws TechnicalException {
        try {
            return internalPageRepo.countByParentIdAndIsPublished(parentId);
        } catch (Exception e) {
            logger.error("An error occurred when counting page by parent_id {}", parentId, e);
            throw new TechnicalException("An error occurred when counting page by parentId");
        }
    }

    @Override
    public Map<String, List<String>> deleteByReferenceIdAndReferenceType(String referenceId, PageReferenceType referenceType)
        throws TechnicalException {
        logger.debug("Delete pages by refId: {}/{}", referenceId, referenceType);
        try {
            final var pageIdAndMedias = new HashMap<String, List<String>>();
            internalPageRepo
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .forEach(page ->
                    pageIdAndMedias.put(
                        page.getId(),
                        page.getAttachedMedia() != null
                            ? page.getAttachedMedia().stream().map(PageMediaMongo::getMediaHash).toList()
                            : Collections.emptyList()
                    )
                );
            logger.debug("Delete pages by refId: {}/{}", referenceId, referenceType);
            return pageIdAndMedias;
        } catch (Exception e) {
            logger.error("Failed to delete page by refId: {}/{}", referenceId, referenceType, e);
            throw new TechnicalException("Failed to delete page by reference");
        }
    }

    private PageSourceMongo convert(PageSource pageSource) {
        PageSourceMongo pageSourceMongo = new PageSourceMongo();
        pageSourceMongo.setType(pageSource.getType());
        pageSourceMongo.setConfiguration(pageSource.getConfiguration());
        return pageSourceMongo;
    }

    private List<PageMediaMongo> convert(List<PageMedia> attachedMedia) {
        return attachedMedia
            .stream()
            .map(pageMedia -> {
                PageMediaMongo pmm = new PageMediaMongo();
                pmm.setMediaHash(pageMedia.getMediaHash());
                pmm.setMediaName(pageMedia.getMediaName());
                pmm.setAttachedAt(pageMedia.getAttachedAt());
                return pmm;
            })
            .collect(Collectors.toList());
    }

    @Override
    public Set<Page> findAll() throws TechnicalException {
        return internalPageRepo.findAll().stream().map(pageMongo -> mapper.map(pageMongo)).collect(Collectors.toSet());
    }
}
