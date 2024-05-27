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
package io.gravitee.apim.infra.query_service.tag;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.tag.model.Tag;
import io.gravitee.apim.core.tag.query_service.TagQueryService;
import io.gravitee.apim.infra.adapter.TagAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.TagReferenceType;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TagQueryServiceImpl implements TagQueryService {

    private final TagRepository tagRepository;

    public TagQueryServiceImpl(@Lazy TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public List<Tag> findByName(String organizationId, String name) {
        try {
            log.debug("findByName {}", name);
            if (name == null) {
                return Collections.emptyList();
            }

            var groups = tagRepository.findByReference(organizationId, TagReferenceType.ORGANIZATION);
            return groups
                .stream()
                .filter(group -> group.getName().equals(name))
                .map(TagAdapter.INSTANCE::toModel)
                .sorted(Comparator.comparing(Tag::getName))
                .toList();
        } catch (TechnicalException ex) {
            throw new TechnicalDomainException("An error occurs while trying to find tags by name", ex);
        }
    }
}
