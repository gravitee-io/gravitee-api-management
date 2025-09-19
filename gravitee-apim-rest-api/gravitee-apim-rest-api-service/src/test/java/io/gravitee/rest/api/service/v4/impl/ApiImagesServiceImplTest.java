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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.v4.ApiImagesService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ApiImagesServiceImplTest {

    public static final String DATA_IMAGE = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkS";

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private AuditService auditService;

    private ApiImagesService cut;

    @BeforeEach
    public void setUp() {
        cut = new ApiImagesServiceImpl(apiRepository, auditService);
    }

    @Test
    public void shouldReturnApiNotFound() throws TechnicalException {
        when(apiRepository.findById("my-api")).thenReturn(Optional.empty());
        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() ->
            cut.getApiPicture(GraviteeContext.getExecutionContext(), "my-api")
        );

        when(apiRepository.findById("my-api")).thenReturn(Optional.of(new Api()));
        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() ->
            cut.getApiPicture(GraviteeContext.getExecutionContext(), "my-api")
        );

        when(apiRepository.findById("my-api")).thenReturn(Optional.empty());
        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() ->
            cut.getApiBackground(GraviteeContext.getExecutionContext(), "my-api")
        );

        when(apiRepository.findById("my-api")).thenReturn(Optional.of(new Api()));
        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() ->
            cut.getApiBackground(GraviteeContext.getExecutionContext(), "my-api")
        );

        when(apiRepository.findById("my-api")).thenReturn(Optional.empty());
        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() ->
            cut.updateApiPicture(GraviteeContext.getExecutionContext(), "my-api", "new-picture")
        );

        when(apiRepository.findById("my-api")).thenReturn(Optional.of(new Api()));
        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() ->
            cut.updateApiPicture(GraviteeContext.getExecutionContext(), "my-api", "new-picture")
        );
    }

    @Test
    public void shouldGetDefaultPicture() throws TechnicalException {
        ReflectionTestUtils.setField(cut, "defaultApiIcon", "src/test/resources/media/logo.png");
        Api foundApi = new Api();
        foundApi.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        when(apiRepository.findById("my-api")).thenReturn(Optional.of(foundApi));

        var result = cut.getApiPicture(GraviteeContext.getExecutionContext(), "my-api");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("image/png");
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    public void shouldGetApiPicture() throws TechnicalException {
        Api foundApi = new Api();
        foundApi.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        foundApi.setPicture(DATA_IMAGE);
        when(apiRepository.findById("my-api")).thenReturn(Optional.of(foundApi));

        var result = cut.getApiPicture(GraviteeContext.getExecutionContext(), "my-api");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("image/jpeg");
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    public void shouldUpdateApiPicture() throws TechnicalException {
        Api foundApi = new Api();
        foundApi.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        when(apiRepository.findById("my-api")).thenReturn(Optional.of(foundApi));

        cut.updateApiPicture(GraviteeContext.getExecutionContext(), "my-api", DATA_IMAGE);
        verify(apiRepository, times(1)).update(argThat(api -> DATA_IMAGE.equals(api.getPicture()) && api.getUpdatedAt() != null));
        verify(auditService, times(1)).createApiAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            eq("my-api"),
            anyMap(),
            eq(Api.AuditEvent.API_UPDATED),
            any(),
            eq(foundApi),
            argThat(api -> DATA_IMAGE.equals(((Api) api).getPicture()) && (((Api) api).getUpdatedAt() != null))
        );
    }

    @Test
    public void shouldGetEmptyBackground() throws TechnicalException {
        Api foundApi = new Api();
        foundApi.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        when(apiRepository.findById("my-api")).thenReturn(Optional.of(foundApi));

        var result = cut.getApiBackground(GraviteeContext.getExecutionContext(), "my-api");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isNull();
        assertThat(result.getContent()).isNull();
    }

    @Test
    public void shouldGetApiBackground() throws TechnicalException {
        Api foundApi = new Api();
        foundApi.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        foundApi.setBackground(DATA_IMAGE);
        when(apiRepository.findById("my-api")).thenReturn(Optional.of(foundApi));

        var result = cut.getApiBackground(GraviteeContext.getExecutionContext(), "my-api");
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("image/jpeg");
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    public void shouldUpdateApiBackground() throws TechnicalException {
        Api foundApi = new Api();
        foundApi.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        when(apiRepository.findById("my-api")).thenReturn(Optional.of(foundApi));

        cut.updateApiBackground(GraviteeContext.getExecutionContext(), "my-api", DATA_IMAGE);
        verify(apiRepository, times(1)).update(argThat(api -> DATA_IMAGE.equals(api.getBackground()) && api.getUpdatedAt() != null));
        verify(auditService, times(1)).createApiAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            eq("my-api"),
            anyMap(),
            eq(Api.AuditEvent.API_UPDATED),
            any(),
            eq(foundApi),
            argThat(api -> DATA_IMAGE.equals(((Api) api).getBackground()) && (((Api) api).getUpdatedAt() != null))
        );
    }
}
