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
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import java.util.List;
import java.util.Set;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
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
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
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
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "https://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    void shouldReturnDefaultEntrypointWithoutApiV4MatchingTags() {
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "https://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    void shouldReturnEntrypointWithApiV4Tags() {
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        entrypointEntity.setTarget(EntrypointEntity.Target.HTTP);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://tag-entrypoint/path");
    }

    @Test
    void shouldReturnDefaultTcpPortAndEntrypointWithoutApiV4Tags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        TcpListener tcpListener = TcpListener.builder().hosts(List.of("some_tcp_host")).build();
        apiEntity.setListeners(List.of(tcpListener));

        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "https://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("https://default-entrypoint");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("some_tcp_host:4082");
    }

    @Test
    void shouldReturnDefaultTcpPortAndMatchingEntrypointWithApiV4Tags() {
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        TcpListener tcpListener = TcpListener.builder().hosts(List.of("some_tcp_host")).build();
        apiEntity.setListeners(List.of(tcpListener));

        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        entrypointEntity.setTarget(EntrypointEntity.Target.TCP);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("https://tag-entrypoint");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("some_tcp_host:4082");
    }

    @Test
    void shouldReturnDefaultEntrypointWithoutApiV2Tags() {
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "https://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setHost("host");
        virtualHost.setPath("path");
        proxy.setVirtualHosts(List.of(virtualHost));
        apiEntity.setProxy(proxy);
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://default-entrypoint/path");
    }

    @Test
    void shouldReturnDefaultEntrypointWithoutApiV2MatchingTags() {
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "https://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setTags(Set.of("tag"));
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        virtualHost.setHost("host");
        virtualHost.setPath("path");
        proxy.setVirtualHosts(List.of(virtualHost));
        apiEntity.setProxy(proxy);
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://default-entrypoint/path");
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
        entrypointEntity.setTarget(EntrypointEntity.Target.HTTP);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://tag-entrypoint/path");
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
        when(accessPointQueryService.getGatewayAccessPoints(any())).thenReturn(
            List.of(
                AccessPoint.builder().host("ap1Host").secured(true).overriding(true).build(),
                AccessPoint.builder().host("ap2Host").secured(false).overriding(true).build()
            )
        );
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        entrypointEntity.setTarget(EntrypointEntity.Target.HTTP);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(2);
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://ap1Host/path");
        assertThat(apiEntrypoints.get(1).getTarget()).isEqualTo("http://ap2Host/path");
    }

    @Test
    void shouldReturnEntrypointWithApiv4AccessPointsAndMatchingTags() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("path").build())).build();
        apiEntity.setListeners(List.of(httpListener));
        when(accessPointQueryService.getGatewayAccessPoints(any())).thenReturn(
            List.of(
                AccessPoint.builder().host("tag-entrypoint").secured(true).overriding(true).build(),
                AccessPoint.builder().host("ap2Host").secured(false).overriding(true).build()
            )
        );
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        entrypointEntity.setTarget(EntrypointEntity.Target.HTTP);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://tag-entrypoint/path");
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
        when(accessPointQueryService.getGatewayAccessPoints(any())).thenReturn(
            List.of(
                AccessPoint.builder().host("ap1Host").secured(true).overriding(true).build(),
                AccessPoint.builder().host("ap2Host").secured(false).overriding(true).build()
            )
        );
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag-unmatching"));
        entrypointEntity.setValue("https://tag-entrypoint");
        entrypointEntity.setTarget(EntrypointEntity.Target.HTTP);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(2);
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://ap1Host/path");
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
        when(accessPointQueryService.getGatewayAccessPoints(any())).thenReturn(
            List.of(
                AccessPoint.builder().host("tag-entrypoint").secured(true).overriding(true).build(),
                AccessPoint.builder().host("ap2Host").secured(false).overriding(true).build()
            )
        );
        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(Arrays.array("tag"));
        entrypointEntity.setValue("https://tag-entrypoint");
        entrypointEntity.setTarget(EntrypointEntity.Target.HTTP);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("https://tag-entrypoint/path");
    }

    @Test
    void shouldReturnEntrypointForV4NativeApiWithoutTags() {
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        var kafkaListener = new KafkaListener();
        kafkaListener.setHost("kafka-host");
        kafkaListener.setPort(9092);
        apiEntity.setListeners(List.of(kafkaListener));

        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        var apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("kafka-host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("kafka-host.kafka.domain:9092");
    }

    @Test
    void shouldReturnEntrypointForV4NativeApiWithMatchingTags() {
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("6666");

        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));

        var kafkaListener = new KafkaListener();
        kafkaListener.setHost("kafka-host");
        kafkaListener.setPort(9092);
        apiEntity.setListeners(List.of(kafkaListener));

        var entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(new String[] { "tag" });
        entrypointEntity.setValue("kafka-entrypoint:9042");
        entrypointEntity.setTarget(EntrypointEntity.Target.KAFKA);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        var apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("kafka-host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("kafka-host.kafka-entrypoint:9042");
    }

    @Test
    void shouldReturnDefaultEntrypointForV4NativeApiWithUnmatchingTags() {
        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));

        var kafkaListener = new KafkaListener();
        kafkaListener.setHost("kafka-host");
        kafkaListener.setPort(9092);
        apiEntity.setListeners(List.of(kafkaListener));

        var entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(new String[] { "unmatched-tag" });
        entrypointEntity.setValue("kafka-entrypoint");
        entrypointEntity.setTarget(EntrypointEntity.Target.KAFKA);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        var apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("kafka-host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("kafka-host.kafka.domain:9092");
    }

    @Test
    void shouldReturnMultipleEntrypointsForV4NativeApiWithMultipleKafkaListeners() {
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        // Arrange
        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        var kafkaListener1 = new KafkaListener();
        kafkaListener1.setHost("kafka-host1");
        kafkaListener1.setPort(9092);

        var kafkaListener2 = new KafkaListener();
        kafkaListener2.setHost("kafka-host2");
        kafkaListener2.setPort(9093);

        apiEntity.setListeners(List.of(kafkaListener1, kafkaListener2));

        var apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(2);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("kafka-host1");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("kafka-host1.kafka.domain:9092");
        assertThat(apiEntrypoints.get(1).getHost()).isEqualTo("kafka-host2");
        assertThat(apiEntrypoints.get(1).getTarget()).isEqualTo("kafka-host2.kafka.domain:9092");
    }

    @Test
    void shouldReturnEntrypointForV4NativeApiWithEmptyKafkaDomain() {
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        var kafkaListener = new KafkaListener();
        kafkaListener.setHost("kafka-host");
        kafkaListener.setPort(null);
        apiEntity.setListeners(List.of(kafkaListener));

        var apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        assertThat(apiEntrypoints.getFirst().getHost()).isEqualTo("kafka-host");
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("kafka-host:9092");
    }

    @Test
    void shouldAssignTagsToEntrypointForV4NativeApi() {
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("6666");

        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag1", "tag2"));

        var kafkaListener = new KafkaListener();
        kafkaListener.setHost("kafka-host");
        kafkaListener.setPort(9092);
        apiEntity.setListeners(List.of(kafkaListener));

        var entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(new String[] { "tag1", "tag2" });
        entrypointEntity.setValue("kafka-entrypoint:9042");
        entrypointEntity.setTarget(EntrypointEntity.Target.KAFKA);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));

        var apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        var entrypoint = apiEntrypoints.getFirst();
        assertThat(entrypoint.getHost()).isEqualTo("kafka-host");
        assertThat(entrypoint.getTarget()).isEqualTo("kafka-host.kafka-entrypoint:9042");
        assertThat(entrypoint.getTags()).containsExactlyInAnyOrder("tag1", "tag2");
    }

    @Test
    void shouldNotIncludeTagsWhenTagEntrypointsIsNull() {
        when(parameterService.find(any(), eq(Key.PORTAL_ENTRYPOINT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka://default-entrypoint"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        var kafkaListener = new KafkaListener();
        kafkaListener.setHost("kafka-host");
        kafkaListener.setPort(9092);
        apiEntity.setListeners(List.of(kafkaListener));

        var apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(1);
        var entrypoint = apiEntrypoints.getFirst();
        assertThat(entrypoint.getHost()).isEqualTo("kafka-host");
        assertThat(entrypoint.getTarget()).isEqualTo("kafka-host.kafka.domain:9092");
        assertThat(entrypoint.getTags()).isNull();
    }

    @Test
    void shouldReturnAccessPointEntrypointsForNativeV4Api() {
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("6666");

        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        var kafkaListener = new KafkaListener();
        kafkaListener.setHost("kafka-host");
        kafkaListener.setPort(9092);
        apiEntity.setListeners(List.of(kafkaListener));

        when(accessPointQueryService.getKafkaGatewayAccessPoints(any())).thenReturn(
            List.of(
                AccessPoint.builder().host("domain1:1234").target(AccessPoint.Target.KAFKA_GATEWAY).build(),
                AccessPoint.builder().host("domain2").target(AccessPoint.Target.KAFKA_GATEWAY).build(),
                AccessPoint.builder().host("{apiHost}-trial.domain3:1234").target(AccessPoint.Target.KAFKA_GATEWAY).build()
            )
        );

        var apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(apiEntrypoints).hasSize(3);
        assertThat(apiEntrypoints.getFirst().getTarget()).isEqualTo("kafka-host.domain1:1234");
        assertThat(apiEntrypoints.get(1).getTarget()).isEqualTo("kafka-host.domain2:9092");
        assertThat(apiEntrypoints.get(2).getTarget()).isEqualTo("kafka-host-trial.domain3:1234");
    }

    @Test
    void should_skip_entrypoint_for_v4_api_with_unsupported_listeners_only() {
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));
        SubscriptionListener unsupportedListener = SubscriptionListener.builder().build();
        apiEntity.setListeners(List.of(unsupportedListener));

        EntrypointEntity entrypointEntity = new EntrypointEntity();
        entrypointEntity.setTags(new String[] { "tag" });
        entrypointEntity.setValue("https://tag-entrypoint");
        entrypointEntity.setTarget(EntrypointEntity.Target.HTTP);
        when(entrypointService.findAll(any())).thenReturn(List.of(entrypointEntity));
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);
        assertThat(apiEntrypoints).isEmpty();
    }

    @Test
    void should_include_entrypoints_when_api_has_at_least_one_supported_listener() {
        when(parameterService.find(any(), eq(Key.PORTAL_TCP_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("4082");
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_DOMAIN), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(
            "kafka.domain"
        );
        when(parameterService.find(any(), eq(Key.PORTAL_KAFKA_PORT), any(), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn("9092");
        HttpListener httpListener = HttpListener.builder()
            .paths(List.of(Path.builder().host("my-host").path("/v1").overrideAccess(false).build()))
            .build();
        SubscriptionListener subscriptionListener = SubscriptionListener.builder().build();
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("public"));
        apiEntity.setListeners(List.of(httpListener, subscriptionListener));

        EntrypointEntity httpEntrypoint = new EntrypointEntity();
        httpEntrypoint.setTags(new String[] { "public" });
        httpEntrypoint.setValue("https://gateway.example.com");
        httpEntrypoint.setTarget(EntrypointEntity.Target.HTTP);

        when(entrypointService.findAll(any())).thenReturn(List.of(httpEntrypoint));

        List<ApiEntrypointEntity> entrypoints = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(entrypoints)
            .as("Expected entrypoints to be generated from supported HttpListener")
            .isNotEmpty()
            .anySatisfy(e -> assertThat(e.getTarget()).contains("https://gateway.example.com/v1"));
    }

    @Test
    void shouldMatchHttpEntrypointsWithSecureDomainFiltering() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag1", "tag2"));
        HttpListener httpListener = HttpListener.builder().paths(List.of(Path.builder().host("host").path("/api").build())).build();
        apiEntity.setListeners(List.of(httpListener));

        when(accessPointQueryService.getGatewayAccessPoints(any())).thenReturn(
            List.of(AccessPoint.builder().host("api.gateway.io").secured(true).build())
        );

        // Test multiple scenarios: exact match, subdomain, case-insensitive, with port
        EntrypointEntity exactMatchEntrypointEntity = new EntrypointEntity();
        exactMatchEntrypointEntity.setTags(Arrays.array("tag1", "tag2"));
        exactMatchEntrypointEntity.setValue("https://api.gateway.io");
        exactMatchEntrypointEntity.setTarget(EntrypointEntity.Target.HTTP);

        EntrypointEntity subdomainMatchEntrypointEntity = new EntrypointEntity();
        subdomainMatchEntrypointEntity.setTags(Arrays.array("tag1", "tag2"));
        subdomainMatchEntrypointEntity.setValue("https://api.gateway.io:8443");
        subdomainMatchEntrypointEntity.setTarget(EntrypointEntity.Target.HTTP);

        EntrypointEntity caseInsensitiveEntrypointEntity = new EntrypointEntity();
        caseInsensitiveEntrypointEntity.setTags(Arrays.array("tag1", "tag2"));
        caseInsensitiveEntrypointEntity.setValue("https://API.Gateway.IO");
        caseInsensitiveEntrypointEntity.setTarget(EntrypointEntity.Target.HTTP);

        when(entrypointService.findAll(any())).thenReturn(
            List.of(exactMatchEntrypointEntity, subdomainMatchEntrypointEntity, caseInsensitiveEntrypointEntity)
        );

        List<ApiEntrypointEntity> result = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(ep -> ep.getTarget().toLowerCase().contains("gateway.io"));
    }

    @Test
    void shouldMatchKafkaWithBidirectionalSubdomainCheck() {
        // Covers: Kafka bidirectional matching, extractHostname with Kafka format (host:port)
        var apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setTags(Set.of("tag"));

        var kafkaListener = new KafkaListener();
        kafkaListener.setHost("host");
        kafkaListener.setPort(9092);
        apiEntity.setListeners(List.of(kafkaListener));

        // Access point more specific than entrypoint domain
        when(accessPointQueryService.getKafkaGatewayAccessPoints(any())).thenReturn(
            List.of(AccessPoint.builder().host("broker.kafka.domain:9092").target(AccessPoint.Target.KAFKA_GATEWAY).build())
        );

        EntrypointEntity kafkaEntrypointEntity = new EntrypointEntity();
        kafkaEntrypointEntity.setTags(Arrays.array("tag"));
        kafkaEntrypointEntity.setValue("kafka.domain:9092");
        kafkaEntrypointEntity.setTarget(EntrypointEntity.Target.KAFKA);
        when(entrypointService.findAll(any())).thenReturn(List.of(kafkaEntrypointEntity));

        List<ApiEntrypointEntity> result = apiEntrypointService.getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(result).hasSize(1);
    }

    @Nested
    class createHttpApiEntrypointEntity {

        @Test
        void should_remove_trailing_slash() {
            String host = "https://localhost";
            String path = "/path1/";
            ApiEntrypointEntity apiEntrypoint = ((ApiEntrypointServiceImpl) apiEntrypointService).createHttpApiEntrypointEntity(
                null,
                host,
                path,
                null,
                null
            );
            assertThat(apiEntrypoint.getTarget()).isEqualTo("https://localhost/path1");
        }
    }
}
