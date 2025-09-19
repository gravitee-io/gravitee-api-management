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

import static java.util.Collections.emptySet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TagNotAllowedException;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class TagsValidationServiceImplTest {

    @Mock
    private TagService tagService;

    @Mock
    private ObjectMapper objectMapper;

    private TagsValidationService tagsValidationService;

    @Before
    public void before() {
        tagsValidationService = new TagsValidationServiceImpl(tagService, objectMapper);
    }

    @Test
    public void shouldReturnValidatedTagsWithSameTags() {
        Set<String> oldTags = Set.of("public");
        Set<String> newTags = Set.of("public");
        Set<String> tags = tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags);

        assertEquals(newTags, tags);
    }

    @Test
    public void shouldReturnValidatedTagsWithAllowedTag() {
        Set<String> oldTags = Set.of("public");
        Set<String> newTags = Set.of("private");
        when(tagService.findByUser(any(), any(), any())).thenReturn(Set.of("public", "private"));

        Set<String> tags = tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags);

        assertEquals(newTags, tags);
    }

    @Test
    public void shouldReturnValidatedTagsWithAllowedTags() {
        Set<String> oldTags = Set.of("public");
        Set<String> newTags = Set.of("public", "private");
        when(tagService.findByUser(any(), any(), any())).thenReturn(Set.of("public", "private"));

        Set<String> tags = tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags);

        assertEquals(newTags, tags);
    }

    @Test
    public void shouldNotUpdateWithNotAllowedTag() {
        Set<String> oldTags = Set.of("public");
        Set<String> newTags = Set.of("private");
        when(tagService.findByUser(any(), any(), any())).thenReturn(Set.of());
        assertThatExceptionOfType(TagNotAllowedException.class).isThrownBy(() ->
            tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags)
        );
    }

    @Test
    public void shouldNotUpdateWithExistingNotAllowedTag() {
        Set<String> oldTags = Set.of("public");
        Set<String> newTags = Set.of("private");
        when(tagService.findByUser(any(), any(), any())).thenReturn(Set.of("public"));
        assertThatExceptionOfType(TagNotAllowedException.class).isThrownBy(() ->
            tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags)
        );
    }

    @Test
    public void shouldNotUpdateWithExistingNotAllowedTags() {
        Set<String> oldTags = Set.of("public", "private");
        Set<String> newTags = Set.of("private");
        when(tagService.findByUser(any(), any(), any())).thenReturn(Set.of("private"));
        assertThatExceptionOfType(TagNotAllowedException.class).isThrownBy(() ->
            tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags)
        );
    }

    @Test
    public void shouldAcceptEmptyTagsForBothApiAndPlan() throws Exception {
        tagsValidationService.validatePlanTagsAgainstApiTags(emptySet(), mockApi(DefinitionVersion.V4, emptySet()));
        tagsValidationService.validatePlanTagsAgainstApiTags(emptySet(), mockApi(DefinitionVersion.V2, emptySet()));
    }

    @Test
    public void shouldAcceptPlanTagsWithAtLeastOneApiTag() throws Exception {
        tagsValidationService.validatePlanTagsAgainstApiTags(
            Set.of("planTag1", "apiTag1"),
            mockApi(DefinitionVersion.V4, Set.of("apiTag1", "apiTag2"))
        );
        tagsValidationService.validatePlanTagsAgainstApiTags(
            Set.of("planTag1", "apiTag1"),
            mockApi(DefinitionVersion.V2, Set.of("apiTag1", "apiTag2"))
        );
    }

    @Test
    public void shouldRejectPlanTagsIfApiNoTags() throws Exception {
        assertThatExceptionOfType(TagNotAllowedException.class).isThrownBy(() ->
            tagsValidationService.validatePlanTagsAgainstApiTags(Set.of("tag1"), mockApi(DefinitionVersion.V4, emptySet()))
        );
        assertThatExceptionOfType(TagNotAllowedException.class).isThrownBy(() ->
            tagsValidationService.validatePlanTagsAgainstApiTags(Set.of("tag1"), mockApi(DefinitionVersion.V2, emptySet()))
        );
    }

    @Test
    public void shouldRejectPlanTagsIfNoCommonApiTags() throws Exception {
        assertThatExceptionOfType(TagNotAllowedException.class).isThrownBy(() ->
            tagsValidationService.validatePlanTagsAgainstApiTags(Set.of("planTag1"), mockApi(DefinitionVersion.V4, Set.of("apiTag1")))
        );
        assertThatExceptionOfType(TagNotAllowedException.class).isThrownBy(() ->
            tagsValidationService.validatePlanTagsAgainstApiTags(Set.of("planTag1"), mockApi(DefinitionVersion.V2, Set.of("apiTag1")))
        );
    }

    private Api mockApi(DefinitionVersion version, Set<String> tags) throws Exception {
        var api = new Api();
        if (version == DefinitionVersion.V4) {
            var apiDefinition = new io.gravitee.definition.model.v4.Api();
            apiDefinition.setDefinitionVersion(version);
            apiDefinition.setTags(tags);
            api.setDefinitionVersion(version);
            api.setDefinition(version.name());
            doReturn(apiDefinition).when(objectMapper).readValue(version.name(), io.gravitee.definition.model.v4.Api.class);
        } else {
            var apiDefinition = new io.gravitee.definition.model.Api();
            apiDefinition.setDefinitionVersion(version);
            apiDefinition.setTags(tags);
            api.setDefinitionVersion(version);
            api.setDefinition(version.name());
            doReturn(apiDefinition).when(objectMapper).readValue(version.name(), io.gravitee.definition.model.Api.class);
        }
        return api;
    }
}
