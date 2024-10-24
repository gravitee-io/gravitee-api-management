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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageRevision;
import io.gravitee.rest.api.model.PageRevisionEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PageRevisionServiceImpl extends TransactionalService implements PageRevisionService {

    private static final Logger logger = LoggerFactory.getLogger(PageRevisionServiceImpl.class);

    private static final String HASH_ALGO = "sha-256";

    @Lazy
    @Autowired
    private PageRevisionRepository pageRevisionRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public io.gravitee.common.data.domain.Page<PageRevisionEntity> findAll(Pageable pageable) {
        logger.debug("get all page revisions with pageable {}", pageable);
        try {
            io.gravitee.common.data.domain.Page<PageRevision> revisions = pageRevisionRepository.findAll(pageable);
            List<PageRevisionEntity> revisionEntities = revisions.getContent().stream().map(this::convert).collect(Collectors.toList());
            return new io.gravitee.common.data.domain.Page<PageRevisionEntity>(
                revisionEntities,
                revisions.getPageNumber(),
                revisionEntities.size(),
                revisions.getTotalElements()
            );
        } catch (TechnicalException e) {
            logger.warn("An error occurs while trying to get the page revisions {}", pageable, e);
            throw new TechnicalManagementException("An error occurs while trying to get all page revisions", e);
        }
    }

    @Override
    public Optional<PageRevisionEntity> findById(String pageId, int revision) {
        logger.debug("get page revision {}-{}", pageId, revision);
        try {
            return pageRevisionRepository.findById(pageId, revision).map(this::convert);
        } catch (TechnicalException e) {
            logger.warn("An error occurs while trying to get the page revision {}-{}", pageId, revision, e);
            throw new TechnicalManagementException("An error occurs while trying to get a page revision", e);
        }
    }

    @Override
    public Optional<PageRevisionEntity> findLastByPageId(String pageId) {
        logger.debug("get last revision for page {}", pageId);
        try {
            return pageRevisionRepository.findLastByPageId(pageId).map(this::convert);
        } catch (TechnicalException e) {
            logger.warn("An error occurs while trying to get the last revision for page {}", pageId, e);
            throw new TechnicalManagementException("An error occurs while trying to get the last page revision", e);
        }
    }

    @Override
    public List<PageRevisionEntity> findAllByPageId(String pageId) {
        logger.debug("get all revisions for page {}", pageId);
        try {
            return pageRevisionRepository.findAllByPageId(pageId).stream().map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException e) {
            logger.warn("An error occurs while trying to get all the revisions for page {}", pageId, e);
            throw new TechnicalManagementException("An error occurs while trying to get all revisions", e);
        }
    }

    @Override
    public PageRevisionEntity create(Page page) {
        try {
            logger.debug("Create page revision for page {}", page.getId());

            PageType type = PageType.valueOf(page.getType());
            if (!(type == PageType.MARKDOWN || type == PageType.SWAGGER || type == PageType.TRANSLATION)) {
                throw new TechnicalManagementException("Invalid page type for revision");
            }

            PageRevision revision = pageRevisionRepository.create(convert(page));

            return convert(revision);
        } catch (TechnicalException e) {
            logger.warn("An error occurs while trying to create a revision for page {}", page.getId(), e);
            throw new TechnicalManagementException("An error occurs while trying to create a page revision", e);
        }
    }

    @Override
    public void deleteAllByPageId(String pageId) throws TechnicalException {
        pageRevisionRepository.deleteAllByPageId(pageId);
    }

    private PageRevisionEntity convert(PageRevision revision) {
        PageRevisionEntity entity = new PageRevisionEntity();
        entity.setPageId(revision.getPageId());
        entity.setRevision(revision.getRevision());
        entity.setName(revision.getName());
        entity.setContent(revision.getContent());
        entity.setHash(revision.getHash());
        entity.setContributor(revision.getContributor());
        entity.setModificationDate(revision.getCreatedAt());
        return entity;
    }

    private PageRevision convert(Page page) {
        PageRevision revision = new PageRevision();

        revision.setPageId(page.getId());
        revision.setRevision(findLastByPageId(page.getId()).map(rev -> rev.getRevision() + 1).orElse(1));

        revision.setName(page.getName());
        revision.setContent(page.getContent());
        revision.setContributor(page.getLastContributor());
        revision.setCreatedAt(page.getUpdatedAt());

        revision.setHash(computeHash(revision));
        return revision;
    }

    private String computeHash(PageRevision page) {
        try {
            String canonicalRevision = canonicalRevision(page);
            MessageDigest md = MessageDigest.getInstance(HASH_ALGO);
            md.update(canonicalRevision.getBytes());
            byte[] digest = md.digest();
            return Hex.encodeHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalManagementException("Unable to instantiate MessageDigest", e);
        }
    }

    @NotNull
    private String canonicalRevision(PageRevision page) {
        StringBuilder builder = new StringBuilder();
        builder.append(Optional.ofNullable(page.getName()).map(c -> c.trim()).orElse(""));
        builder.append('\n');
        builder.append(Optional.ofNullable(page.getContent()).map(c -> c.trim()).orElse(""));
        builder.append('\n');
        builder.append(page.getContributor());
        builder.append('\n');
        builder.append(page.getCreatedAt().getTime());
        builder.append('\n');
        return builder.toString();
    }
}
