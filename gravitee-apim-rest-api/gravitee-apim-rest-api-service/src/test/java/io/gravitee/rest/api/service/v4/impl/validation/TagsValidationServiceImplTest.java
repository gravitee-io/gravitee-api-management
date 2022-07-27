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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TagNotAllowedException;
import io.gravitee.rest.api.service.v4.TagsValidationService;
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

    private TagsValidationService tagsValidationService;

    @Before
    public void before() {
        tagsValidationService = new TagsValidationServiceImpl(tagService);
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
        assertThatExceptionOfType(TagNotAllowedException.class)
            .isThrownBy(() -> tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags));
    }

    @Test
    public void shouldNotUpdateWithExistingNotAllowedTag() {
        Set<String> oldTags = Set.of("public");
        Set<String> newTags = Set.of("private");
        when(tagService.findByUser(any(), any(), any())).thenReturn(Set.of("public"));
        assertThatExceptionOfType(TagNotAllowedException.class)
            .isThrownBy(() -> tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags));
    }

    @Test
    public void shouldNotUpdateWithExistingNotAllowedTags() {
        Set<String> oldTags = Set.of("public", "private");
        Set<String> newTags = Set.of("private");
        when(tagService.findByUser(any(), any(), any())).thenReturn(Set.of("private"));
        assertThatExceptionOfType(TagNotAllowedException.class)
            .isThrownBy(() -> tagsValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), oldTags, newTags));
    }
}
