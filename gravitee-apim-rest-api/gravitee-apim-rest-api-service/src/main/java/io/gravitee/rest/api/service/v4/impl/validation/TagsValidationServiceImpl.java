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
package io.gravitee.rest.api.service.v4.impl.validation;

import static java.util.stream.Collectors.toSet;
import static org.springframework.util.ObjectUtils.isEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TagNotAllowedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class TagsValidationServiceImpl extends AbstractService implements TagsValidationService {

    private final TagService tagService;

    private ObjectMapper objectMapper;

    @Autowired
    public TagsValidationServiceImpl(final TagService tagService, final ObjectMapper objectMapper) {
        this.tagService = tagService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<String> validateAndSanitize(final ExecutionContext executionContext, final Set<String> oldTags, final Set<String> newTags) {
        final Set<String> existingTags = oldTags == null ? new HashSet<>() : oldTags;
        final Set<String> tagsToUpdate = newTags == null ? new HashSet<>() : newTags;

        Set<String> tags;
        if (existingTags.isEmpty()) {
            tags = tagsToUpdate;
        } else {
            // Filter to keep only those newed or removed
            tags = existingTags
                .stream()
                .filter(tag -> !tagsToUpdate.contains(tag))
                .collect(toSet());
            tags.addAll(
                tagsToUpdate
                    .stream()
                    .filter(tag -> !existingTags.contains(tag))
                    .collect(toSet())
            );
        }

        if (!tags.isEmpty()) {
            final Set<String> userTags = tagService.findByUser(
                getAuthenticatedUsername(),
                executionContext.getOrganizationId(),
                TagReferenceType.ORGANIZATION
            );
            if (!userTags.containsAll(tags)) {
                final String[] notAllowedTags = tags
                    .stream()
                    .filter(tag -> !userTags.contains(tag))
                    .toArray(String[]::new);
                throw new TagNotAllowedException(notAllowedTags);
            }
        }

        return newTags;
    }

    public void validatePlanTagsAgainstApiTags(Set<String> planTags, Api api) {
        try {
            var apiTags = api.getDefinitionVersion() == DefinitionVersion.V4
                ? objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class).getTags()
                : objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class).getTags();
            validatePlanTagsAgainstApiTags(planTags, apiTags);
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException("An error occurs while trying load api definition", e);
        }
    }

    @Override
    public void validatePlanTagsAgainstApiTags(Set<String> planTags, Set<String> apiTags) {
        if (!isEmpty(planTags) && (isEmpty(apiTags) || !apiTags.stream().anyMatch(apiTag -> planTags.contains(apiTag)))) {
            log.debug("Plan rejected, tags {} mismatch the tags defined by the API ({})", planTags, apiTags);
            throw new TagNotAllowedException(planTags.toArray(new String[planTags.size()]));
        }
    }
}
