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
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.apim.core.api.model.utils.MigrationResultUtils.get;
import static io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType.RANDOM;
import static io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType.ROUND_ROBIN;
import static io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType.WEIGHTED_RANDOM;
import static io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType.WEIGHTED_ROUND_ROBIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.json.jackson.JsonMapperFactory;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.LoadBalancer;
import io.gravitee.definition.model.LoadBalancerType;
import io.gravitee.definition.model.ProtocolVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheckRequest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckResponse;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.service.Service;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class V2ToV4MigrationOperatorTest {

    private V2toV4MigrationOperator mapper;

    @BeforeEach
    void setUp() {
        mapper = new V2toV4MigrationOperator(JsonMapperFactory.build());
    }

    @Nested
    class ApiMigratingTest {

        @Test
        void should_preserve_api_metadata_when_mapping() {
            // Given
            var originalApi = ApiFixtures.aProxyApiV2();
            originalApi
                .getApiDefinition()
                .getProxy()
                .getGroups()
                .forEach(group -> group.getEndpoints().forEach(e -> e.setInherit(false)));

            // When
            var result = get(mapper.mapApi(originalApi));

            // Then
            assertThat(result.getId()).isEqualTo(originalApi.getId());
            assertThat(result.getName()).isEqualTo(originalApi.getName());
            assertThat(result.getDescription()).isEqualTo(originalApi.getDescription());
            assertThat(result.getVersion()).isEqualTo(originalApi.getVersion());
            assertThat(result.getEnvironmentId()).isEqualTo(originalApi.getEnvironmentId());
            assertThat(result.getCrossId()).isEqualTo(originalApi.getCrossId());
            assertThat(result.getHrid()).isEqualTo(originalApi.getHrid());
            assertThat(result.getOriginContext()).isEqualTo(originalApi.getOriginContext());
            assertThat(result.getCreatedAt()).isEqualTo(originalApi.getCreatedAt());
            assertThat(result.getUpdatedAt()).isEqualTo(originalApi.getUpdatedAt());
            assertThat(result.getDeployedAt()).isEqualTo(originalApi.getDeployedAt());
            assertThat(result.getVisibility()).isEqualTo(originalApi.getVisibility());
            assertThat(result.getLifecycleState()).isEqualTo(originalApi.getLifecycleState());
            assertThat(result.getPicture()).isEqualTo(originalApi.getPicture());
            assertThat(result.getGroups()).isEqualTo(originalApi.getGroups());
            assertThat(result.getCategories()).isEqualTo(originalApi.getCategories());
            assertThat(result.getLabels()).isEqualTo(originalApi.getLabels());
            assertThat(result.isDisableMembershipNotifications()).isEqualTo(originalApi.isDisableMembershipNotifications());
            assertThat(result.getApiLifecycleState()).isEqualTo(originalApi.getApiLifecycleState());
            assertThat(result.getBackground()).isEqualTo(originalApi.getBackground());
        }

        @Test
        void should_upgrade_definition_version_to_v4() {
            var v2Api = ApiFixtures.aProxyApiV2().toBuilder().definitionVersion(DefinitionVersion.V2).build();
            v2Api
                .getApiDefinition()
                .getProxy()
                .getGroups()
                .forEach(group -> group.getEndpoints().forEach(e -> e.setInherit(false)));

            var result = get(mapper.mapApi(v2Api));

            assertThat(result.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
            assertThat(result.getType()).isEqualTo(ApiType.PROXY);
        }
    }

    @Nested
    class ListenerMigratingTest {

        @ParameterizedTest
        @ValueSource(strings = { "/api", "/v1/users", "/health/check", "/", "/api/", "/test/" })
        void path_should_end_with_slash(String inputPath) {
            // Given
            var virtualHost = new VirtualHost();
            virtualHost.setPath(inputPath);

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(virtualHost));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            // When
            var result = get(mapper.mapApi(api));

            // Then
            if (result.getApiDefinitionHttpV4().getListeners().getFirst() instanceof HttpListener httpListener) {
                assertThat(httpListener.getPaths().getFirst().getPath()).endsWith("/");
            } else {
                fail("Listener is not an HttpListener");
            }
        }

        @Test
        void should_preserve_virtual_host_entrypoint_property() {
            var virtualHost = new VirtualHost();
            virtualHost.setPath("/api");
            virtualHost.setHost("api.example.com");

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(virtualHost));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            var httpListener = (HttpListener) result.getApiDefinitionHttpV4().getListeners().getFirst();
            var path = httpListener.getPaths().getFirst();

            assertThat(path.getHost()).isEqualTo("api.example.com");
        }

        @Test
        void should_create_http_listener_with_correct_properties() {
            var virtualHost1 = new VirtualHost();
            virtualHost1.setPath("/api/v1");
            virtualHost1.setHost("api.example.com");

            var virtualHost2 = new VirtualHost();
            virtualHost2.setPath("/api/v2");
            virtualHost2.setHost("api2.example.com");

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(virtualHost1, virtualHost2));
            proxy.setServers(List.of("server1", "server2"));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            assertThat(result.getApiDefinitionHttpV4().getListeners()).hasSize(1);
            var listener = result.getApiDefinitionHttpV4().getListeners().getFirst();
            assertThat(listener.getType()).isEqualTo(ListenerType.HTTP);

            var httpListener = (HttpListener) listener;
            assertThat(httpListener.getPaths()).hasSize(2);
            assertThat(httpListener.getServers()).containsExactly("server1", "server2");
            assertThat(httpListener.getEntrypoints()).hasSize(1);
            assertThat(httpListener.getEntrypoints().getFirst().getType()).isEqualTo("http-proxy");
        }
    }

    @Nested
    class ApiDefinitionMigratingTest {

        @ParameterizedTest
        @MethodSource("loadBalancerTypeProvider")
        void should_map_load_balancer_types_correctly(
            LoadBalancerType v2Type,
            io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType expectedV4Type
        ) {
            var endpoint = new Endpoint();
            endpoint.setName("test-endpoint");
            endpoint.setTarget("http://example.com");
            endpoint.setInherit(false);
            var loadBalancer = new LoadBalancer();
            loadBalancer.setType(v2Type);

            var endpointGroup = new EndpointGroup();
            endpointGroup.setName("default-group");
            endpointGroup.setEndpoints(Set.of(endpoint));
            endpointGroup.setLoadBalancer(loadBalancer);

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(new VirtualHost()));
            proxy.setGroups(Set.of(endpointGroup));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            var mappedEndpointGroup = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
            if (expectedV4Type != null) {
                assertThat(mappedEndpointGroup.getLoadBalancer().getType()).isEqualTo(expectedV4Type);
            } else {
                assertThat(mappedEndpointGroup.getLoadBalancer()).isNull();
            }
        }

        private static Stream<Arguments> loadBalancerTypeProvider() {
            return Stream.of(
                arguments(LoadBalancerType.RANDOM, RANDOM),
                arguments(LoadBalancerType.ROUND_ROBIN, ROUND_ROBIN),
                arguments(LoadBalancerType.WEIGHTED_RANDOM, WEIGHTED_RANDOM),
                arguments(LoadBalancerType.WEIGHTED_ROUND_ROBIN, WEIGHTED_ROUND_ROBIN),
                arguments(null, null)
            );
        }

        @Test
        void should_set_api_properties_correctly() {
            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api-id");
            apiDef.setName("Test API Name");
            apiDef.setVersion("2.0.1");
            apiDef.setTags(Set.of("tag1", "tag2", "tag3"));
            apiDef.setProxy(new Proxy());

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));
            var v4Definition = result.getApiDefinitionHttpV4();

            assertThat(v4Definition.getId()).isEqualTo("test-api-id");
            assertThat(v4Definition.getName()).isEqualTo("Test API Name");
            assertThat(v4Definition.getApiVersion()).isEqualTo("2.0.1");
            assertThat(v4Definition.getTags()).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
            assertThat(v4Definition.getType()).isEqualTo(ApiType.PROXY);
            assertThat(v4Definition.getProperties()).isNull();
            assertThat(v4Definition.getResources()).isEmpty();
        }
    }

    @Nested
    class EndpointMigratingTest {

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_preserve_endpoint_backup_as_secondary_property(boolean isBackup) {
            var endpoint = new Endpoint();
            endpoint.setName("test-endpoint");
            endpoint.setTarget("http://example.com");
            endpoint.setBackup(isBackup);
            endpoint.setWeight(10);
            endpoint.setTenants(List.of("tenant1", "tenant2"));
            endpoint.setInherit(false);
            var endpointGroup = new EndpointGroup();
            endpointGroup.setName("default-group");
            endpointGroup.setEndpoints(Set.of(endpoint));

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(new VirtualHost()));
            proxy.setGroups(Set.of(endpointGroup));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            var mappedEndpoint = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst().getEndpoints().getFirst();
            assertThat(mappedEndpoint.isSecondary()).isEqualTo(isBackup);
            assertThat(mappedEndpoint.getWeight()).isEqualTo(10);
            assertThat(mappedEndpoint.getTenants()).containsExactlyInAnyOrder("tenant1", "tenant2");
        }

        @Test
        void should_preserve_endpoint_configuration() {
            var endpoint = new Endpoint();
            endpoint.setName("test-endpoint");
            endpoint.setTarget("http://example.com");
            endpoint.setType("http");
            endpoint.setConfiguration("{\"target\": \"http://example.com\", \"ssl\": {\"trustStore\": true}}");
            endpoint.setInherit(false);
            var endpointGroup = new EndpointGroup();
            endpointGroup.setName("default-group");
            endpointGroup.setEndpoints(Set.of(endpoint));

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(new VirtualHost()));
            proxy.setGroups(Set.of(endpointGroup));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            var mappedEndpoint = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst().getEndpoints().getFirst();
            assertSoftly(softly -> {
                softly.assertThat(mappedEndpoint.getName()).isEqualTo("test-endpoint");
                softly.assertThat(mappedEndpoint.getType()).isEqualTo("http-proxy");
                softly.assertThat(mappedEndpoint.getConfiguration()).contains("\"target\":\"http://example.com\"");
            });
        }

        @Test
        void should_migrate_endpoint_and_group() {
            // Setup Endpoint
            Endpoint v2Endpoint = new Endpoint();
            v2Endpoint.setName("endpoint-1");
            v2Endpoint.setBackup(true);
            v2Endpoint.setTenants(List.of("tenant-a"));
            v2Endpoint.setWeight(5);
            v2Endpoint.setInherit(false);
            v2Endpoint.setConfiguration("{\"target\":\"http://example.com\"}");

            // Setup EndpointGroup
            EndpointGroup v2Group = new EndpointGroup();
            v2Group.setName("default-group");
            Set<Endpoint> endpoints = new HashSet<>();
            endpoints.add(v2Endpoint);
            v2Group.setEndpoints(endpoints);

            HttpClientOptions httpClientOptions = new HttpClientOptions();
            httpClientOptions.setConnectTimeout(2000);
            httpClientOptions.setReadTimeout(3000);
            httpClientOptions.setVersion(ProtocolVersion.HTTP_1_1);
            httpClientOptions.setClearTextUpgrade(true);
            httpClientOptions.setUseCompression(true);
            httpClientOptions.setFollowRedirects(true);
            httpClientOptions.setIdleTimeout(1000);
            httpClientOptions.setKeepAlive(true);
            httpClientOptions.setMaxConcurrentConnections(5);
            httpClientOptions.setKeepAliveTimeout(5);
            httpClientOptions.setPipelining(false);
            httpClientOptions.setPropagateClientAcceptEncoding(true);
            v2Group.setHttpClientOptions(httpClientOptions);

            HttpClientSslOptions sslOptions = new HttpClientSslOptions();
            sslOptions.setTrustAll(false);

            PEMTrustStore trustStore = new PEMTrustStore();
            trustStore.setPath("/a/b/c");
            trustStore.setContent("abc");
            sslOptions.setTrustStore(trustStore);

            PEMKeyStore keyStore = new PEMKeyStore();
            keyStore.setKeyContent("abc");
            keyStore.setKeyPath("/a/b/c");
            sslOptions.setKeyStore(keyStore);

            v2Group.setHttpClientSslOptions(sslOptions);
            var headers = new ArrayList<HttpHeader>();
            headers.add(new HttpHeader("X-Test", "yes"));
            v2Group.setHeaders(headers);

            LoadBalancer lb = new LoadBalancer();
            lb.setType(LoadBalancerType.RANDOM);
            v2Group.setLoadBalancer(lb);

            // Setup Proxy
            Proxy proxy = new Proxy();
            proxy.setGroups(Set.of(v2Group));

            proxy.setVirtualHosts(List.of(new VirtualHost("localhost", "/api", false)));
            proxy.setCors(new Cors());
            proxy.setServers(List.of("localhost"));

            // Setup Api V2
            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);
            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            // Act
            var result = get(mapper.mapApi(api));

            // Check Endpoint Group
            var group = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
            String configJson =
                "{\"http\":{\"idleTimeout\":1000,\"keepAliveTimeout\":5,\"connectTimeout\":2000,\"keepAlive\":true,\"readTimeout\":3000,\"pipelining\":false,\"maxConcurrentConnections\":5,\"useCompression\":true,\"propagateClientAcceptEncoding\":true,\"propagateClientHost\":false,\"followRedirects\":true,\"version\":\"HTTP_1_1\"},\"ssl\":{\"trustAll\":false,\"hostnameVerifier\":false,\"trustStore\":{\"type\":\"PEM\",\"path\":\"/a/b/c\",\"content\":\"abc\"},\"keyStore\":{\"type\":\"PEM\",\"keyPath\":\"/a/b/c\",\"keyContent\":\"abc\"}},\"headers\":[{\"name\":\"X-Test\",\"value\":\"yes\"}]}";
            assertSoftly(softly -> {
                softly.assertThat(result.getApiDefinitionHttpV4().getEndpointGroups()).hasSize(1);
                softly.assertThat(group.getName()).isEqualTo("default-group");
                softly.assertThat(group.getType()).isEqualTo("http-proxy");
                softly.assertThat(group.getSharedConfiguration()).isNotNull();
                softly.assertThat(group.getSharedConfiguration()).contains(configJson);
            });

            // Check Endpoint
            var endpoint = group.getEndpoints().getFirst();
            assertSoftly(softly -> {
                softly.assertThat(endpoint.getType()).isEqualTo("http-proxy");
                softly.assertThat(endpoint.getName()).isEqualTo("endpoint-1");
                softly.assertThat(endpoint.getWeight()).isEqualTo(5);
                softly.assertThat(endpoint.getConfiguration()).isEqualTo("{\"target\":\"http://example.com\"}");
            });
        }

        @Test
        void should_migrate_endpoint_and_group_with_inherit() {
            // Setup Endpoint
            Endpoint v2Endpoint1 = new Endpoint();
            v2Endpoint1.setName("endpoint-1");
            v2Endpoint1.setBackup(true);
            v2Endpoint1.setTenants(List.of("tenant-a"));
            v2Endpoint1.setWeight(5);
            v2Endpoint1.setInherit(true);

            Endpoint v2Endpoint2 = new Endpoint();
            v2Endpoint2.setName("endpoint-2");
            v2Endpoint2.setBackup(true);
            v2Endpoint2.setTenants(List.of("tenant-b"));
            v2Endpoint2.setWeight(5);
            v2Endpoint2.setInherit(false);
            String configJsonForEndpoint =
                "{\"http\":{\"idleTimeout\":2000,\"keepAliveTimeout\":6,\"connectTimeout\":2000,\"keepAlive\":true,\"readTimeout\":3000,\"pipelining\":false,\"maxConcurrentConnections\":5,\"useCompression\":true,\"propagateClientAcceptEncoding\":false,\"propagateClientHost\":false,\"followRedirects\":true,\"version\":\"HTTP_1_1\"},\"ssl\":{\"trustAll\":false,\"hostnameVerifier\":false,\"trustStore\":{\"type\":\"PEM\",\"path\":\"/d/e/f\",\"content\":\"def\"},\"keyStore\":{\"type\":\"PEM\",\"keyPath\":\"/d/e/f\",\"keyContent\":\"def\",\"certPath\":null,\"certContent\":null}},\"headers\":[{\"name\":\"X-Test1\",\"value\":\"no\"}]}";
            v2Endpoint2.setConfiguration(configJsonForEndpoint);
            // Setup EndpointGroup
            EndpointGroup v2Group = new EndpointGroup();
            v2Group.setName("default-group");
            v2Group.setEndpoints(Set.of(v2Endpoint1, v2Endpoint2));

            HttpClientOptions httpClientOptions = new HttpClientOptions();
            httpClientOptions.setConnectTimeout(2000);
            httpClientOptions.setReadTimeout(3000);
            httpClientOptions.setVersion(ProtocolVersion.HTTP_1_1);
            httpClientOptions.setClearTextUpgrade(true);
            httpClientOptions.setUseCompression(true);
            httpClientOptions.setFollowRedirects(true);
            httpClientOptions.setIdleTimeout(1000);
            httpClientOptions.setKeepAlive(true);
            httpClientOptions.setMaxConcurrentConnections(5);
            httpClientOptions.setKeepAliveTimeout(5);
            httpClientOptions.setPipelining(false);
            httpClientOptions.setPropagateClientAcceptEncoding(true);
            v2Group.setHttpClientOptions(httpClientOptions);

            HttpClientSslOptions sslOptions = new HttpClientSslOptions();
            sslOptions.setTrustAll(false);

            PEMTrustStore trustStore = new PEMTrustStore();
            trustStore.setPath("/a/b/c");
            trustStore.setContent("abc");
            sslOptions.setTrustStore(trustStore);

            PEMKeyStore keyStore = new PEMKeyStore();
            keyStore.setKeyContent("abc");
            keyStore.setKeyPath("/a/b/c");
            sslOptions.setKeyStore(keyStore);

            v2Group.setHttpClientSslOptions(sslOptions);
            v2Group.setHeaders(List.of(new HttpHeader("X-Test", "yes")));

            LoadBalancer lb = new LoadBalancer();
            lb.setType(LoadBalancerType.RANDOM);
            v2Group.setLoadBalancer(lb);

            // Setup Proxy
            Proxy proxy = new Proxy();
            proxy.setGroups(Set.of(v2Group));

            proxy.setVirtualHosts(List.of(new VirtualHost("localhost", "/api", false)));
            proxy.setCors(new Cors());
            proxy.setServers(List.of("localhost"));

            // Setup Api V2
            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);
            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            // Act
            var result = get(mapper.mapApi(api));

            // Check Endpoint Group
            String configJson =
                "{\"http\":{\"idleTimeout\":1000,\"keepAliveTimeout\":5,\"connectTimeout\":2000,\"keepAlive\":true,\"readTimeout\":3000,\"pipelining\":false,\"maxConcurrentConnections\":5,\"useCompression\":true,\"propagateClientAcceptEncoding\":true,\"propagateClientHost\":false,\"followRedirects\":true,\"version\":\"HTTP_1_1\"},\"ssl\":{\"trustAll\":false,\"hostnameVerifier\":false,\"trustStore\":{\"type\":\"PEM\",\"path\":\"/a/b/c\",\"content\":\"abc\"},\"keyStore\":{\"type\":\"PEM\",\"keyPath\":\"/a/b/c\",\"keyContent\":\"abc\"}},\"headers\":[{\"name\":\"X-Test\",\"value\":\"yes\"}]}";
            assertThat(result.getApiDefinitionHttpV4().getEndpointGroups())
                .singleElement()
                .satisfies(group -> {
                    assertSoftly(softly -> {
                        softly.assertThat(result.getApiDefinitionHttpV4().getEndpointGroups()).hasSize(1);
                        softly.assertThat(group.getName()).isEqualTo("default-group");
                        softly.assertThat(group.getType()).isEqualTo("http-proxy");
                        softly.assertThat(group.getSharedConfiguration()).isNotNull();
                        softly.assertThat(group.getSharedConfiguration()).contains(configJson);
                    });

                    // Check Endpoint
                    assertThat(group.getEndpoints())
                        .filteredOn(e -> e.getName().equals("endpoint-1"))
                        .singleElement()
                        .satisfies(endpoint1 ->
                            assertSoftly(softly -> {
                                softly.assertThat(endpoint1.getType()).isEqualTo("http-proxy");
                                softly.assertThat(endpoint1.getName()).isEqualTo("endpoint-1");
                                softly.assertThat(endpoint1.getWeight()).isEqualTo(5);
                                softly.assertThat(endpoint1.isInheritConfiguration()).isTrue();
                            })
                        );

                    assertThat(group.getEndpoints())
                        .filteredOn(e -> e.getName().equals("endpoint-2"))
                        .singleElement()
                        .satisfies(endpoint2 ->
                            assertSoftly(softly -> {
                                softly.assertThat(endpoint2.getType()).isEqualTo("http-proxy");
                                softly.assertThat(endpoint2.getName()).isEqualTo("endpoint-2");
                                softly.assertThat(endpoint2.getWeight()).isEqualTo(5);
                                softly.assertThat(endpoint2.isInheritConfiguration()).isFalse();
                                softly.assertThat(endpoint2.getSharedConfigurationOverride()).isEqualTo(configJsonForEndpoint);
                            })
                        );
                });
        }
    }

    @Test
    void should_migrate_endpointgroup_hc() {
        // Setup Endpoint
        Endpoint v2Endpoint = new Endpoint();
        v2Endpoint.setName("endpoint-1");
        v2Endpoint.setBackup(true);
        v2Endpoint.setTenants(List.of("tenant-a"));
        v2Endpoint.setWeight(5);
        v2Endpoint.setInherit(false);
        v2Endpoint.setConfiguration("{\"target\":\"http://example.com\"}");

        // Setup EndpointGroup
        EndpointGroup v2Group = new EndpointGroup();
        v2Group.setName("default-group");
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(v2Endpoint);
        v2Group.setEndpoints(endpoints);

        ArrayList<HttpHeader> headers = new ArrayList<HttpHeader>();
        headers.add(new HttpHeader("X-Test", "yes"));
        v2Group.setHeaders(headers);

        LoadBalancer lb = new LoadBalancer();
        lb.setType(LoadBalancerType.RANDOM);
        v2Group.setLoadBalancer(lb);

        // Setup Proxy
        Proxy proxy = new Proxy();
        Set<EndpointGroup> endpointGroups = new HashSet<>();
        endpointGroups.add(v2Group);
        proxy.setGroups(endpointGroups);

        proxy.setVirtualHosts(List.of(new VirtualHost("localhost", "/api", false)));
        proxy.setCors(new Cors());
        proxy.setServers(List.of("localhost"));

        // Setup Api V2
        var apiDef = new io.gravitee.definition.model.Api();
        apiDef.setId("test-api");
        apiDef.setName("Test API");
        apiDef.setVersion("1.0");
        apiDef.setProxy(proxy);
        HealthCheckStep step = new HealthCheckStep();
        step.setName("hc-step");

        HealthCheckRequest request = new HealthCheckRequest();
        request.setPath("/hc");
        request.setMethod(HttpMethod.POST);
        step.setRequest(request);
        // Configure the expected response
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(java.util.List.of("#status == 200"));
        step.setResponse(response);
        HealthCheckService healthCheckService = HealthCheckService.builder()
            .enabled(true) // comes from ScheduledService (superclass)
            .schedule("*/30 * * * * *") // run every 30s, for example
            .steps(List.of(step))
            .build();
        Services services = new Services();
        services.setHealthCheckService(healthCheckService);
        apiDef.setServices(services);
        var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

        // Act
        var result = get(mapper.mapApi(api));

        // Check Endpoint Group
        var group = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
        String configJson =
            "{\"schedule\":\"*/30 * * * * *\",\"failureThreshold\":2,\"successThreshold\":2,\"headers\":[],\"method\":\"POST\",\"target\":\"/hc\",\"assertion\":\"{#status == 200}\",\"overrideEndpointPath\":false}";
        assertSoftly(softly -> {
            softly.assertThat(result.getApiDefinitionHttpV4().getEndpointGroups()).hasSize(1);
            softly.assertThat(group.getName()).isEqualTo("default-group");
            softly.assertThat(group.getType()).isEqualTo("http-proxy");
            softly.assertThat(group.getSharedConfiguration()).isNotNull();
            softly.assertThat(group.getServices().getHealthCheck()).isNotNull();
            softly.assertThat(group.getServices().getHealthCheck().getConfiguration()).isEqualTo(configJson);
        });

        // Check Endpoint
        var endpoint = group.getEndpoints().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(endpoint.getType()).isEqualTo("http-proxy");
            softly.assertThat(endpoint.getName()).isEqualTo("endpoint-1");
            softly.assertThat(endpoint.getWeight()).isEqualTo(5);
            softly.assertThat(endpoint.getConfiguration()).isEqualTo("{\"target\":\"http://example.com\"}");
        });
    }

    @Test
    void should_migrate_endpoint_hc() {
        // Setup Endpoint
        Endpoint v2Endpoint = new Endpoint();
        v2Endpoint.setName("endpoint-1");
        v2Endpoint.setBackup(true);
        v2Endpoint.setTenants(List.of("tenant-a"));
        v2Endpoint.setWeight(5);
        v2Endpoint.setInherit(false);
        v2Endpoint.setConfiguration(
            "{\"name\":\"default\",\"target\":\"http://test\",\"weight\":1,\"backup\":false,\"status\":\"UP\",\"tenants\":[],\"type\":\"http\",\"inherit\":true,\"headers\":[],\"healthcheck\":{\"schedule\":\"0 */1 * * * *\",\"steps\":[{\"name\":\"default-step\",\"request\":{\"path\":\"/hc3\",\"method\":\"GET\",\"headers\":[],\"fromRoot\":false},\"response\":{\"assertions\":[\"#response.status == 202\"]}}],\"enabled\":true,\"inherit\":false}}"
        );
        // Setup EndpointGroup
        EndpointGroup v2Group = new EndpointGroup();
        v2Group.setName("default-group");
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(v2Endpoint);
        v2Group.setEndpoints(endpoints);

        ArrayList<HttpHeader> headers = new ArrayList<HttpHeader>();
        headers.add(new HttpHeader("X-Test", "yes"));
        v2Group.setHeaders(headers);

        LoadBalancer lb = new LoadBalancer();
        lb.setType(LoadBalancerType.RANDOM);
        v2Group.setLoadBalancer(lb);

        // Setup Proxy
        Proxy proxy = new Proxy();
        Set<EndpointGroup> endpointGroups = new HashSet<>();
        endpointGroups.add(v2Group);
        proxy.setGroups(endpointGroups);

        proxy.setVirtualHosts(List.of(new VirtualHost("localhost", "/api", false)));
        proxy.setCors(new Cors());
        proxy.setServers(List.of("localhost"));

        // Setup Api V2
        var apiDef = new io.gravitee.definition.model.Api();
        apiDef.setId("test-api");
        apiDef.setName("Test API");
        apiDef.setVersion("1.0");
        apiDef.setProxy(proxy);

        var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

        // Act
        var result = get(mapper.mapApi(api));

        // Check Endpoint Group
        var group = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(result.getApiDefinitionHttpV4().getEndpointGroups()).hasSize(1);
            softly.assertThat(group.getName()).isEqualTo("default-group");
            softly.assertThat(group.getType()).isEqualTo("http-proxy");
        });

        // Check Endpoint
        var endpoint = group.getEndpoints().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(endpoint.getType()).isEqualTo("http-proxy");
            softly.assertThat(endpoint.getName()).isEqualTo("endpoint-1");
            softly.assertThat(endpoint.getWeight()).isEqualTo(5);
            softly
                .assertThat(endpoint.getServices().getHealthCheck().getConfiguration())
                .isEqualTo(
                    "{\"schedule\":\"0 */1 * * * *\",\"failureThreshold\":2,\"successThreshold\":2,\"headers\":[],\"method\":\"GET\",\"target\":\"/hc3\",\"assertion\":\"{#response.status == 202}\",\"overrideEndpointPath\":false}"
                );
        });
    }

    @Test
    void should_migrate_endpoint_group_with_non_existing_healthcheck_steps() {
        // Setup Endpoint
        Endpoint v2Endpoint = new Endpoint();
        v2Endpoint.setName("endpoint-1");
        v2Endpoint.setBackup(true);
        v2Endpoint.setTenants(List.of("tenant-a"));
        v2Endpoint.setWeight(5);
        v2Endpoint.setInherit(false);
        v2Endpoint.setConfiguration(
            """
            {
              "name": "default",
              "target": "http://test",
              "weight": 1,
              "backup": false,
              "status": "UP",
              "tenants": [],
              "type": "http",
              "inherit": true,
              "headers": [],
              "healthcheck": {
                "enabled": false,
                "inherit": false
              }
            }
            """
        );
        // Setup EndpointGroup
        EndpointGroup v2Group = new EndpointGroup();
        v2Group.setName("default-group");
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(v2Endpoint);
        v2Group.setEndpoints(endpoints);

        ArrayList<HttpHeader> headers = new ArrayList<HttpHeader>();
        headers.add(new HttpHeader("X-Test", "yes"));
        v2Group.setHeaders(headers);

        LoadBalancer lb = new LoadBalancer();
        lb.setType(LoadBalancerType.RANDOM);
        v2Group.setLoadBalancer(lb);

        // Setup Proxy
        Proxy proxy = new Proxy();
        Set<EndpointGroup> endpointGroups = new HashSet<>();
        endpointGroups.add(v2Group);
        proxy.setGroups(endpointGroups);

        proxy.setVirtualHosts(List.of(new VirtualHost("localhost", "/api", false)));
        proxy.setCors(new Cors());
        proxy.setServers(List.of("localhost"));

        // Setup Api V2
        var apiDef = new io.gravitee.definition.model.Api();
        apiDef.setId("test-api");
        apiDef.setName("Test API");
        apiDef.setVersion("1.0");
        apiDef.setProxy(proxy);

        var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

        // Act
        var result = get(mapper.mapApi(api));

        // Check Endpoint Group
        var group = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(result.getApiDefinitionHttpV4().getEndpointGroups()).hasSize(1);
            softly.assertThat(group.getName()).isEqualTo("default-group");
            softly.assertThat(group.getType()).isEqualTo("http-proxy");
        });

        // Check Endpoint
        var endpoint = group.getEndpoints().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(endpoint.getType()).isEqualTo("http-proxy");
            softly.assertThat(endpoint.getName()).isEqualTo("endpoint-1");
            softly.assertThat(endpoint.getWeight()).isEqualTo(5);
            softly.assertThat(endpoint.getServices().getHealthCheck()).isNull();
        });
    }

    @Test
    void should_not_migrate_endpoint_hc_when_inherited() {
        // Setup Endpoint
        Endpoint v2Endpoint = new Endpoint();
        v2Endpoint.setName("endpoint-1");
        v2Endpoint.setInherit(false);
        v2Endpoint.setConfiguration(
            """
            {
              "name": "default",
              "target": "http://test",
              "weight": 1,
              "backup": false,
              "status": "UP",
              "tenants": [],
              "type": "http",
              "inherit": true,
              "headers": [],
              "healthcheck": {
                "schedule": "0 */1 * * * *",
                "steps": [],
                "enabled": true,
                "inherit": true
              }
            }
            """
        );
        // Setup EndpointGroup
        EndpointGroup v2Group = new EndpointGroup();
        v2Group.setName("default-group");
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(v2Endpoint);
        v2Group.setEndpoints(endpoints);

        // Setup Proxy
        Proxy proxy = new Proxy();
        Set<EndpointGroup> endpointGroups = new HashSet<>();
        endpointGroups.add(v2Group);
        proxy.setGroups(endpointGroups);

        proxy.setVirtualHosts(List.of(new VirtualHost("localhost", "/api", false)));

        // Setup Api V2
        var apiDef = new io.gravitee.definition.model.Api();
        apiDef.setId("test-api");
        apiDef.setName("Test API");
        apiDef.setVersion("1.0");
        apiDef.setProxy(proxy);

        var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

        // Act
        var result = get(mapper.mapApi(api));

        // Check Endpoint Group
        var group = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(result.getApiDefinitionHttpV4().getEndpointGroups()).hasSize(1);
            softly.assertThat(group.getName()).isEqualTo("default-group");
            softly.assertThat(group.getType()).isEqualTo("http-proxy");
        });

        // Check Endpoint
        var endpoint = group.getEndpoints().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(endpoint.getServices().getHealthCheck()).isNull();
        });
    }

    @Nested
    class ScheduleNullGuardTest {

        @Test
        void should_not_migrate_endpointgroup_hc_when_schedule_is_null() {
            // Setup Endpoint
            Endpoint v2Endpoint = new Endpoint();
            v2Endpoint.setName("endpoint-1");
            v2Endpoint.setInherit(false);
            v2Endpoint.setConfiguration("{\"target\":\"http://example.com\"}");

            // Setup EndpointGroup
            EndpointGroup v2Group = new EndpointGroup();
            v2Group.setName("default-group");
            v2Group.setEndpoints(Set.of(v2Endpoint));

            LoadBalancer lb = new LoadBalancer();
            lb.setType(LoadBalancerType.ROUND_ROBIN);
            v2Group.setLoadBalancer(lb);

            // Health check service with schedule = null (should be filtered out)
            HealthCheckService healthCheckService = HealthCheckService.builder()
                .enabled(true)
                .schedule(null) // null schedule → must be filtered
                .steps(List.of())
                .build();
            Services services = new Services();
            services.setHealthCheckService(healthCheckService);

            // Setup Proxy
            Proxy proxy = new Proxy();
            proxy.setGroups(Set.of(v2Group));
            proxy.setVirtualHosts(List.of(new VirtualHost()));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);
            apiDef.setServices(services);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            // Act
            var result = get(mapper.mapApi(api));

            // Assert: healthCheck must not have been migrated because schedule is null
            var group = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
            assertThat(group.getServices()).isNotNull();
            assertThat(group.getServices().getHealthCheck()).isNull();
        }

        @Test
        void should_not_migrate_endpoint_hc_when_schedule_is_null() {
            // Endpoint HC with schedule=null and inherit=false → not migrated
            Endpoint v2Endpoint = new Endpoint();
            v2Endpoint.setName("endpoint-1");
            v2Endpoint.setInherit(false);
            v2Endpoint.setConfiguration(
                """
                {
                  "name": "default",
                  "target": "http://test",
                  "type": "http",
                  "healthcheck": {
                    "steps": [],
                    "enabled": true,
                    "inherit": false
                  }
                }
                """
            );

            EndpointGroup v2Group = new EndpointGroup();
            v2Group.setName("default-group");
            v2Group.setEndpoints(Set.of(v2Endpoint));

            LoadBalancer lb = new LoadBalancer();
            lb.setType(LoadBalancerType.ROUND_ROBIN);
            v2Group.setLoadBalancer(lb);

            Proxy proxy = new Proxy();
            proxy.setGroups(Set.of(v2Group));
            proxy.setVirtualHosts(List.of(new VirtualHost()));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            // Act
            var result = get(mapper.mapApi(api));

            // Assert: endpoint HC must not be migrated since schedule is null
            var group = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
            var endpoint = group.getEndpoints().getFirst();
            assertThat(endpoint.getServices()).isNotNull();
            assertThat(endpoint.getServices().getHealthCheck()).isNull();
        }
    }

    @Nested
    class PlanMigratingTest {

        @ParameterizedTest
        @MethodSource("planStatusProvider")
        void should_map_plan_status_correctly(String v2Status, PlanStatus expectedV4Status) {
            var planDef = new io.gravitee.definition.model.Plan();
            planDef.setId("plan-id");
            planDef.setName("Test Plan");
            planDef.setStatus(v2Status);
            planDef.setSecurity("api-key");

            var plan = PlanFixtures.aPlanV2().toBuilder().id("plan-id").name("Test Plan").planDefinitionV2(planDef).build();

            var result = get(mapper.mapPlan(plan));

            assertSoftly(softly -> {
                softly.assertThat(result.getPlanDefinitionHttpV4().getStatus()).isEqualTo(expectedV4Status);
                softly.assertThat(result.getPlanDefinitionHttpV4().getMode()).isEqualTo(PlanMode.STANDARD);
                softly.assertThat(result.getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo("api-key");
                softly.assertThat(result.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                softly.assertThat(result.getApiType()).isEqualTo(ApiType.PROXY);
            });
        }

        private static Stream<Arguments> planStatusProvider() {
            return Stream.of(
                Arguments.of("STAGING", PlanStatus.STAGING),
                Arguments.of("PUBLISHED", PlanStatus.PUBLISHED),
                Arguments.of("CLOSED", PlanStatus.CLOSED),
                Arguments.of("DEPRECATED", PlanStatus.DEPRECATED)
            );
        }

        @ParameterizedTest
        @CsvSource({ "api-key, api-key", "oauth2, oauth2", "keyless, keyless", "jwt, jwt" })
        void should_preserve_plan_security_type(String v2SecurityType, String expectedV4SecurityType) {
            var planDef = new io.gravitee.definition.model.Plan();
            planDef.setId("plan-id");
            planDef.setName("Test Plan");
            planDef.setStatus("PUBLISHED");
            planDef.setSecurity(v2SecurityType);
            planDef.setSecurityDefinition("{\"config\": \"value\"}");

            var plan = PlanFixtures.aPlanV2().toBuilder().planDefinitionV2(planDef).build();

            var result = get(mapper.mapPlan(plan));

            assertThat(result.getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo(expectedV4SecurityType);
            assertThat(result.getPlanDefinitionHttpV4().getSecurity().getConfiguration()).isEqualTo("{\"config\": \"value\"}");
        }

        @Test
        void should_preserve_plan_properties() {
            var planDef = new io.gravitee.definition.model.Plan();
            planDef.setId("plan-id");
            planDef.setName("Test Plan");
            planDef.setStatus("PUBLISHED");
            planDef.setSecurity("api-key");
            planDef.setSelectionRule("rule");
            planDef.setTags(Set.of("tag1", "tag2"));

            var now = TimeProvider.now();
            var plan = PlanFixtures.aPlanV2()
                .toBuilder()
                .id("plan-id")
                .name("Test Plan")
                .description("Plan Description")
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .closedAt(now)
                .order(5)
                .characteristics(List.of("char1", "char2"))
                .excludedGroups(List.of("group1"))
                .planDefinitionV2(planDef)
                .validation(Plan.PlanValidationType.MANUAL)
                .build();

            var result = get(mapper.mapPlan(plan));

            assertSoftly(softly -> {
                softly.assertThat(result.getId()).isEqualTo("plan-id");
                softly.assertThat(result.getName()).isEqualTo("Test Plan");
                softly.assertThat(result.getDescription()).isEqualTo("Plan Description");
                softly.assertThat(result.getCreatedAt()).isEqualTo(now);
                softly.assertThat(result.getUpdatedAt()).isEqualTo(now);
                softly.assertThat(result.getPublishedAt()).isEqualTo(now);
                softly.assertThat(result.getClosedAt()).isEqualTo(now);
                softly.assertThat(result.getOrder()).isEqualTo(5);
                softly.assertThat(result.getCharacteristics()).containsExactly("char1", "char2");
                softly.assertThat(result.getExcludedGroups()).containsExactly("group1");
                softly.assertThat(result.getValidation()).isEqualTo(Plan.PlanValidationType.MANUAL);
                softly.assertThat(result.getPlanDefinitionHttpV4().getSelectionRule()).isEqualTo("rule");
                softly.assertThat(result.getPlanDefinitionHttpV4().getTags()).containsExactlyInAnyOrder("tag1", "tag2");
                softly.assertThat(result.isCommentRequired()).isFalse();
            });
        }

        @ParameterizedTest
        @MethodSource("planValidationTypeProvider")
        void should_map_plan_validation_type_correctly(Plan.PlanValidationType v2Validation, Plan.PlanValidationType expectedValidation) {
            var plan = PlanFixtures.aPlanV2().toBuilder().validation(v2Validation).build();

            var result = get(mapper.mapPlan(plan));

            assertThat(result.getValidation()).isEqualTo(expectedValidation);
        }

        private static Stream<Arguments> planValidationTypeProvider() {
            return Stream.of(
                Arguments.of(Plan.PlanValidationType.AUTO, Plan.PlanValidationType.AUTO),
                Arguments.of(Plan.PlanValidationType.MANUAL, Plan.PlanValidationType.MANUAL)
            );
        }

        @Test
        void should_set_needRedeployAt_for_plan_mapping() {
            // Given
            var now = TimeProvider.now();
            var plan = PlanFixtures.aPlanV2();

            // When
            var result = get(mapper.mapPlan(plan));

            // Then
            assertThat(result.getNeedRedeployAt()).isNotNull();
            assertThat(result.getNeedRedeployAt().toInstant()).isAfter(now.toInstant().minusSeconds(1));
            assertThat(result.getNeedRedeployAt().toInstant()).isBefore(now.toInstant().plusSeconds(10));
        }

        @Test
        void shouldMapPlanWithPlanDefinitionV2() {
            io.gravitee.definition.model.Plan planDefinitionV2 = new io.gravitee.definition.model.Plan();
            planDefinitionV2.setId("plan-id");
            planDefinitionV2.setName("Test Plan");
            planDefinitionV2.setSecurity("API_KEY");
            planDefinitionV2.setSecurityDefinition("ApiKey definition");
            planDefinitionV2.setSelectionRule("selection-rule");
            planDefinitionV2.setTags(Set.of("tag1", "tag2"));
            planDefinitionV2.setStatus("PUBLISHED");

            Plan plan = Plan.builder().planDefinitionV2(planDefinitionV2).build();
            var migratedPlanResult = get(mapper.mapPlan(plan));

            assertThat(migratedPlanResult).isNotNull();
            assertThat(migratedPlanResult.getPlanDefinitionHttpV4().getId()).isEqualTo("plan-id");
            assertThat(migratedPlanResult.getPlanDefinitionHttpV4().getName()).isEqualTo("Test Plan");
            assertThat(migratedPlanResult.getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo("API_KEY");
            assertThat(migratedPlanResult.getPlanDefinitionHttpV4().getSecurity().getConfiguration()).isEqualTo("ApiKey definition");
            assertThat(migratedPlanResult.getPlanDefinitionHttpV4().getSelectionRule()).isEqualTo("selection-rule");
            assertThat(migratedPlanResult.getPlanDefinitionHttpV4().getTags()).containsExactlyInAnyOrder("tag1", "tag2");
        }

        @Test
        void shouldMapPlanWithoutPlanDefinitionV2() {
            Plan plan = Plan.builder().planDefinitionV2(null).build();

            var migratedPlanResult = get(mapper.mapPlan(plan));
            assertThat(migratedPlanResult).isNotNull();
            assertThat(migratedPlanResult.getPlanDefinitionHttpV4()).isNull();
        }
    }
}
