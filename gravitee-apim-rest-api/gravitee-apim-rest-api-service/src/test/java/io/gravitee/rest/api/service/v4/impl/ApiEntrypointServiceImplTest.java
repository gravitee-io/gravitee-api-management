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

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiEntrypointServiceImplTest {

    @Mock
    private ParameterService parameterService;

    @Mock
    private EntrypointService entrypointService;

    @Mock
    private AccessPointQueryService accessPointQueryService;

    private ApiEntrypointService apiEntrypointService;

    @BeforeEach
    public void before() {
        apiEntrypointService = new ApiEntrypointServiceImpl(parameterService, entrypointService, accessPointQueryService);
    }

    @Test
    void shouldReturnDefaultEntrypointWithoutApiV4Tags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("https://default-entrypoint");
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    void shouldReturnDefaultEntrypointWithoutApiV4MatchingTags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("https://default-entrypoint");
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    void shouldReturnEntrypointWithApiV4Tags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");

        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://tag-entrypoint/path");
    }

    @Test
    void shouldReturnDefaultTcpPortAndEntrypointWithoutApiV4Tags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        TcpListener tcpListener = TcpListener.builder().hosts(List.of("some_tcp_host")).build();
        apiEntity.setListeners(List.of(tcpListener));
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("https://default-entrypoint");
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("https://default-entrypoint");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("some_tcp_host:4082");
    }

    @Test
    void shouldReturnDefaultTcpPortAndMatchingEntrypointWithApiV4Tags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        TcpListener tcpListener = TcpListener.builder().hosts(List.of("some_tcp_host")).build();
        apiEntity.setListeners(List.of(tcpListener));

        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("https://tag-entrypoint");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("some_tcp_host:4082");
    }

    @Test
    void shouldReturnDefaultEntrypointWithoutApiV2Tags() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setHost("host");
        virtualHost.setPath("path");
        proxy.setVirtualHosts(List.of(virtualHost));
        apiEntity.setProxy(proxy);
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("https://default-entrypoint");
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    void shouldReturnDefaultEntrypointWithoutApiV2MatchingTags() {
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
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    void shouldReturnEntrypointWithApiV2Tags() {
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

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://tag-entrypoint/path");
    }

    @ParameterizedTest
    @EnumSource(value = DefinitionVersion.class, names = { "V1", "V2" })
    void shouldReturnHttpEntrypointListenerForV1AndV2Api(DefinitionVersion version) {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(version);

        String apiEntrypointListener = apiEntrypointService.getApiEntrypointsListenerType(apiEntity);

        assertThat(apiEntrypointListener).isEqualTo("HTTP");
    }

    @Test
    void shouldReturnHttpEntrypointListenerForV4HttpApi() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));

        String apiEntrypointListener = apiEntrypointService.getApiEntrypointsListenerType(apiEntity);

        assertThat(apiEntrypointListener).isEqualTo("HTTP");
    }

    @Test
    void shouldReturnTcpEntrypointListenerForV4TcpApi() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        TcpListener tcpListener = TcpListener.builder().hosts(List.of("some_tcp_host")).build();
        apiEntity.setListeners(List.of(tcpListener));

        String apiEntrypointListener = apiEntrypointService.getApiEntrypointsListenerType(apiEntity);

        assertThat(apiEntrypointListener).isEqualTo("TCP");
    }

    @Test
    void shouldReturnSubscriptionEntrypointListenerForV4MessagingApi() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        SubscriptionListener subscriptionListener = SubscriptionListener.builder().build();
        apiEntity.setListeners(List.of(subscriptionListener));

        String apiEntrypointListener = apiEntrypointService.getApiEntrypointsListenerType(apiEntity);

        assertThat(apiEntrypointListener).isEqualTo("SUBSCRIPTION");
    }

    @Test
    void shouldReturnAccessPointEntrypointsApiv4() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setTags(Set.of("tag"));
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));
        when(accessPointQueryService.getGatewayAccessPoints(any()))
            .thenReturn(
                List.of(
                    AccessPoint.builder().host("ap1Host").secured(true).overriding(true).build(),
                    AccessPoint.builder().host("ap2Host").secured(false).overriding(true).build()
                )
            );
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(2);
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://ap1Host/path");
        assertThat(apiEntrypoints.get(1).getTarget()).isEqualTo("http://ap2Host/path");
    }

    @Test
    void shouldReturnEntrypointWithApiv4AccessPointsAndMatchingTags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));
        when(accessPointQueryService.getGatewayAccessPoints(any()))
            .thenReturn(
                List.of(
                    AccessPoint.builder().host("ap1Host").secured(true).overriding(true).build(),
                    AccessPoint.builder().host("ap2Host").secured(false).overriding(true).build()
                )
            );
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://tag-entrypoint/path");
    }

    @Test
    void shouldReturnAccessPointEntrypointsApiv2() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        Proxy proxy = new Proxy();
        apiEntity.setTags(Set.of("tag"));
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setHost("host");
        virtualHost.setPath("path");
        proxy.setVirtualHosts(List.of(virtualHost));
        apiEntity.setProxy(proxy);
        when(accessPointQueryService.getGatewayAccessPoints(any()))
            .thenReturn(
                List.of(
                    AccessPoint.builder().host("ap1Host").secured(true).overriding(true).build(),
                    AccessPoint.builder().host("ap2Host").secured(false).overriding(true).build()
                )
            );
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(2);
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://ap1Host/path");
        assertThat(apiEntrypoints.get(1).getTarget()).isEqualTo("http://ap2Host/path");
    }

    @Test
    void shouldReturnEntrypointApiv2WithAccessPointsAndMatchingTags() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setTags(Set.of("tag"));
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setHost("host");
        virtualHost.setPath("path");
        proxy.setVirtualHosts(List.of(virtualHost));
        apiEntity.setProxy(proxy);
        when(accessPointQueryService.getGatewayAccessPoints(any()))
            .thenReturn(
                List.of(
                    AccessPoint.builder().host("ap1Host").secured(true).overriding(true).build(),
                    AccessPoint.builder().host("ap2Host").secured(false).overriding(true).build()
                )
            );
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.get(0).getTarget()).isEqualTo("https://tag-entrypoint/path");
    }
}
