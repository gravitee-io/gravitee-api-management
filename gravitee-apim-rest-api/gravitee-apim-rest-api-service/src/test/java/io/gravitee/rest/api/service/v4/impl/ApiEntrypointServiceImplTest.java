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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import java.util.List;
import java.util.Set;
import org.assertj.core.util.Arrays;
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
public class ApiEntrypointServiceImplTest {

    @Mock
    private ParameterService parameterService;

    @Mock
    private EntrypointService entrypointService;

    private ApiEntrypointService apiEntrypointService;

    @Before
    public void before() {
        apiEntrypointService = new ApiEntrypointServiceImpl(parameterService, entrypointService);
    }

    @Test
    public void shouldReturnDefaultEntrypointWithoutApiV4Tags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        HttpListener httpListener = new HttpListener();
        Path path = new Path();
        path.setHost("host");
        path.setPath("path");
        httpListener.setPaths(List.of(path));
        apiEntity.setListeners(List.of(httpListener));
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("https://default-entrypoint");
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints.size()).isEqualTo(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    public void shouldReturnDefaultEntrypointWithoutApiV4MatchingTags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        HttpListener httpListener = new HttpListener();
        Path path = new Path();
        path.setHost("host");
        path.setPath("path");
        httpListener.setPaths(List.of(path));
        apiEntity.setListeners(List.of(httpListener));
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("https://default-entrypoint");
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints.size()).isEqualTo(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    public void shouldReturnEntrypointWithApiV4Tags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        HttpListener httpListener = new HttpListener();
        Path path = new Path();
        path.setHost("host");
        path.setPath("path");
        httpListener.setPaths(List.of(path));
        apiEntity.setListeners(List.of(httpListener));

        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints.size()).isEqualTo(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://tag-entrypoint/path");
    }

    @Test
    public void shouldReturnDefaultEntrypointWithoutApiV2Tags() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setHost("host");
        virtualHost.setPath("path");
        proxy.setVirtualHosts(List.of(virtualHost));
        apiEntity.setProxy(proxy);
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("https://default-entrypoint");
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints.size()).isEqualTo(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    public void shouldReturnDefaultEntrypointWithoutApiV2MatchingTags() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setTags(Set.of("tag"));
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setHost("host");
        virtualHost.setPath("path");
        proxy.setVirtualHosts(List.of(virtualHost));
        apiEntity.setProxy(proxy);
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("https://default-entrypoint");
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints.size()).isEqualTo(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    public void shouldReturnEntrypointWithApiV2Tags() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setTags(Set.of("tag"));
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setHost("host");
        virtualHost.setPath("path");
        proxy.setVirtualHosts(List.of(virtualHost));
        apiEntity.setProxy(proxy);

        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints.size()).isEqualTo(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://tag-entrypoint/path");
    }
}
