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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.ListenerModelFixtures;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class ImportExportApiMapperTest extends AbstractMapperTest {

    private final ImportExportApiMapper importExportApiMapper = Mappers.getMapper(ImportExportApiMapper.class);

    @Test
    void shouldMapHttpListenerWithCors() {
        HttpListener httpListener = ListenerModelFixtures.aModelHttpListener();

        var result = importExportApiMapper.mapHttpListener(httpListener);

        assertThat(result).isNotNull();
        assertThat(result.getCors()).isNotNull();

        assertThat(result.getCors().getEnabled()).isEqualTo(httpListener.getCors().isEnabled());
        assertThat(result.getCors().getAllowOrigin()).isEqualTo(httpListener.getCors().getAccessControlAllowOrigin());
        assertThat(result.getCors().getAllowHeaders()).isEqualTo(httpListener.getCors().getAccessControlAllowHeaders());
        assertThat(result.getCors().getAllowMethods()).isEqualTo(httpListener.getCors().getAccessControlAllowMethods());
        assertThat(result.getCors().getAllowCredentials()).isEqualTo(httpListener.getCors().isAccessControlAllowCredentials());
        assertThat(result.getCors().getExposeHeaders()).isEqualTo(httpListener.getCors().getAccessControlExposeHeaders());
        assertThat(result.getCors().getMaxAge()).isEqualTo(httpListener.getCors().getAccessControlMaxAge());
        assertThat(result.getCors().getRunPolicies()).isEqualTo(httpListener.getCors().isRunPolicies());
    }

    @Test
    void shouldMapHttpListenerWithNullCors() {
        HttpListener httpListener = mock(HttpListener.class);
        when(httpListener.getCors()).thenReturn(null);

        var result = importExportApiMapper.mapHttpListener(httpListener);

        assertThat(result).isNotNull();
        assertThat(result.getCors()).isNull();
    }

    @Test
    void shouldUseCorsMappperForHttpListener() {
        HttpListener httpListener = ListenerModelFixtures.aModelHttpListener();
        var expectedCors = CorsMapper.INSTANCE.map(httpListener.getCors());
        var result = importExportApiMapper.mapHttpListener(httpListener);

        assertThat(result).isNotNull();
        assertThat(result.getCors()).isNotNull();

        assertThat(result.getCors().getEnabled()).isEqualTo(expectedCors.getEnabled());
        assertThat(result.getCors().getAllowOrigin()).isEqualTo(expectedCors.getAllowOrigin());
        assertThat(result.getCors().getAllowHeaders()).isEqualTo(expectedCors.getAllowHeaders());
        assertThat(result.getCors().getAllowMethods()).isEqualTo(expectedCors.getAllowMethods());
        assertThat(result.getCors().getAllowCredentials()).isEqualTo(expectedCors.getAllowCredentials());
        assertThat(result.getCors().getExposeHeaders()).isEqualTo(expectedCors.getExposeHeaders());
        assertThat(result.getCors().getMaxAge()).isEqualTo(expectedCors.getMaxAge());
        assertThat(result.getCors().getRunPolicies()).isEqualTo(expectedCors.getRunPolicies());
    }
}
