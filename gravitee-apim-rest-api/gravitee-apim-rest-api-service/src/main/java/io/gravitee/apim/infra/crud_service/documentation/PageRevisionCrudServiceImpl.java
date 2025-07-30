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
package io.gravitee.apim.infra.crud_service.documentation;

import io.gravitee.apim.core.documentation.crud_service.PageRevisionCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageRevision;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.PageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PageRevisionCrudServiceImpl implements PageRevisionCrudService {

    private final PageRevisionRepository pageRevisionRepository;

    public PageRevisionCrudServiceImpl(@Lazy PageRevisionRepository pageRevisionRepository) {
        this.pageRevisionRepository = pageRevisionRepository;
    }

    @Override
    public PageRevision create(Page page) {
        var pageRevisionToCreate = PageAdapter.INSTANCE.toPageRevision(page);

        this.computeHash(pageRevisionToCreate);

        try {
            var lastPageRevision = pageRevisionRepository.findLastByPageId(page.getId());
            pageRevisionToCreate.setRevision(lastPageRevision.map(rev -> rev.getRevision() + 1).orElse(1));

            var createdPageRevision = pageRevisionRepository.create(PageAdapter.INSTANCE.toPageRevisionRepository(pageRevisionToCreate));
            return PageAdapter.INSTANCE.toEntity(createdPageRevision);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(String.format("An error occurred while creating PageRevision for page %s", page), e);
        }
    }

    public void computeHash(PageRevision pageRevision) {
        try {
            String canonicalRevision = canonicalRevision(pageRevision);
            MessageDigest md = MessageDigest.getInstance("sha-256");
            md.update(canonicalRevision.getBytes());
            byte[] digest = md.digest();
            pageRevision.setHash(Hex.encodeHexString(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalManagementException(String.format("Unable to instantiate MessageDigest for page %s", pageRevision), e);
        }
    }

    private String canonicalRevision(PageRevision pageRevision) {
        StringBuilder builder = new StringBuilder();
        builder.append(Optional.ofNullable(pageRevision.getName()).map(c -> c.trim()).orElse(""));
        builder.append('\n');
        builder.append(Optional.ofNullable(pageRevision.getContent()).map(c -> c.trim()).orElse(""));
        builder.append('\n');
        builder.append(pageRevision.getContributor());
        builder.append('\n');
        builder.append(pageRevision.getModificationDate().getTime());
        builder.append('\n');
        return builder.toString();
    }
}
