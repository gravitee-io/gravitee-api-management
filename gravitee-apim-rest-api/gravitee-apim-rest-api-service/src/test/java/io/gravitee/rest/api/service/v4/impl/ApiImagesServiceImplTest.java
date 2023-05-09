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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ApiImagesServiceImplTest {

    @Mock
    private ApiSearchService apiSearchService;

    private ApiImagesService cut;

    @BeforeEach
    public void setUp() {
        cut = new ApiImagesServiceImpl(apiSearchService);
    }

    @Test
    public void shouldCatchIOException() {
        ReflectionTestUtils.setField(cut, "defaultApiIcon", "src/test/resources/media/unknown.png");

        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), "my-api")).thenReturn(new Api());

        var result = cut.getApiPicture(GraviteeContext.getExecutionContext(), "my-api");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isNull();
        assertThat(result.getContent()).isNull();
    }

    @Test
    public void shouldGetDefaultPicture() {
        ReflectionTestUtils.setField(cut, "defaultApiIcon", "src/test/resources/media/logo.png");

        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), "my-api")).thenReturn(new Api());

        var result = cut.getApiPicture(GraviteeContext.getExecutionContext(), "my-api");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("image/png");
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    public void shouldGetApiPicture() {
        Api api = new Api();
        api.setPicture("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkS");
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), "my-api")).thenReturn(api);

        var result = cut.getApiPicture(GraviteeContext.getExecutionContext(), "my-api");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("image/jpeg");
        assertThat(result.getContent()).isNotEmpty();
    }
}
