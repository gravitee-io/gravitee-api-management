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
package io.gravitee.gateway.reactive.core.v4.endpoint;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.el.TemplateContext;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.plugin.endpoint.internal.DefaultEndpointConnectorPluginManager;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultEndpointManagerTest {

    private static final String ENDPOINT_TYPE = "test";
    private static final String ENDPOINT_GROUP_SHARED_CONFIG = "{ \"groupSharedConfig\": \"something in the shared config\"}";
    private static final String ENDPOINT_CONFIG = "{ \"config\": \"something\"}";
    private static final String ENDPOINT_SHARED_CONFIG_OVERRIDE =
        "{ \"overriddenSharedConfig\": \"something overridden for the endpoint\"}";
    private static final String MOCK_EXCEPTION = "Mock exception";
    private static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    private static final ApiType SUPPORTED_API_TYPE = ApiType.MESSAGE;
    private static final String TENANT_1 = "tenant-1";
    private static final String TENANT_2 = "tenant-2";

    @Mock
    private ExecutionContext ctx;

    @Mock
    private DefaultEndpointConnectorPluginManager pluginManager;

    @Mock
    private EntrypointConnector entrypointConnector;

    @Mock
    private EndpointConnectorFactory connectorFactory;

    @Mock
    private DeploymentContext deploymentContext;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @BeforeEach
    void init() {
        lenient().when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        lenient().when(entrypointConnector.supportedModes()).thenReturn(SUPPORTED_MODES);
        lenient().when(entrypointConnector.supportedApi()).thenReturn(SUPPORTED_API_TYPE);
        lenient().when(pluginManager.getFactoryById(ENDPOINT_TYPE)).thenReturn(connectorFactory);
    }

    @Nested
    class Start {

        @Test
        void should_start_all_endpoints_with_overridden_shared_configuration() throws Exception {
            final Api api = buildApi(() -> anEndpointWithSharedConfigurationOverride(ENDPOINT_SHARED_CONFIG_OVERRIDE));

            // 2 groups with 2 endpoints each. 4 connectors to instantiate.
            final EndpointConnector connector1 = mock(EndpointConnector.class);
            final EndpointConnector connector2 = mock(EndpointConnector.class);
            final EndpointConnector connector3 = mock(EndpointConnector.class);
            final EndpointConnector connector4 = mock(EndpointConnector.class);

            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_SHARED_CONFIG_OVERRIDE)).thenReturn(
                connector1,
                connector2,
                connector3,
                connector4
            );
            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();

            verify(pluginManager, times(4)).getFactoryById(ENDPOINT_TYPE);
            verify(connectorFactory, times(4)).createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_SHARED_CONFIG_OVERRIDE);
            verify(connector1).start();
            verify(connector2).start();
            verify(connector3).start();
            verify(connector4).start();
        }

        @Test
        void should_start_all_endpoints_with_inherited_group_configuration() throws Exception {
            final Api api = buildApi();

            // 2 groups with 2 endpoints each. 4 connectors to instantiate.
            final EndpointConnector connector1 = mock(EndpointConnector.class);
            final EndpointConnector connector2 = mock(EndpointConnector.class);
            final EndpointConnector connector3 = mock(EndpointConnector.class);
            final EndpointConnector connector4 = mock(EndpointConnector.class);

            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(
                connector1,
                connector2,
                connector3,
                connector4
            );
            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();

            verify(pluginManager, times(4)).getFactoryById(ENDPOINT_TYPE);
            verify(connectorFactory, times(4)).createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG);
            verify(connector1).start();
            verify(connector2).start();
            verify(connector3).start();
            verify(connector4).start();
        }

        @Test
        void should_provide_endpoints_template_variable() throws Exception {
            final TemplateContext templateContext = mock(TemplateContext.class);
            final Api api = buildApi();

            // Rename groups and endpoint to ease assertions.
            final AtomicInteger i = new AtomicInteger(0);

            api.getEndpointGroups().forEach(g -> g.setName("group" + i.incrementAndGet()));

            i.set(0);
            Stream.concat(
                api.getEndpointGroups().get(0).getEndpoints().stream(),
                api.getEndpointGroups().get(1).getEndpoints().stream()
            ).forEachOrdered(e -> e.setName("endpoint" + i.incrementAndGet()));

            final EndpointConnector connector = mock(EndpointConnector.class);

            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);
            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);

            cut.provide(templateContext);
            verify(templateContext).setVariable("endpoints", Collections.emptyMap());
            reset(templateContext);

            cut.start();
            cut.provide(templateContext);

            final ArgumentCaptor<Map<String, String>> endpointsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(templateContext).setVariable(eq("endpoints"), endpointsCaptor.capture());

            assertThat(endpointsCaptor.getValue())
                .containsEntry("group1", "group1:")
                .containsEntry("endpoint1", "endpoint1:")
                .containsEntry("endpoint2", "endpoint2:")
                .containsEntry("group2", "group2:")
                .containsEntry("endpoint3", "endpoint3:")
                .containsEntry("endpoint4", "endpoint4:");
        }

        @Test
        void should_start_endpoint_using_group_shared_configuration() throws Exception {
            final Api api = buildApi(() -> anEndpointWithSharedConfigurationOverride(ENDPOINT_SHARED_CONFIG_OVERRIDE));

            // Make one endpoint of each group inherits the group configuration.
            api.getEndpointGroups().get(0).getEndpoints().get(0).setInheritConfiguration(true);
            api.getEndpointGroups().get(1).getEndpoints().get(0).setInheritConfiguration(true);

            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(eq(deploymentContext), anyString(), anyString())).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next();

            assertThat(next).isNotNull();

            // 2 connectors have been created with endpoint overriding shared configuration
            verify(connectorFactory, times(2)).createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_SHARED_CONFIG_OVERRIDE);
            // 2 connectors have been created with endpoint group shared configuration, cause endpoint2 inherits group shared configuration
            verify(connectorFactory, times(2)).createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG);
        }

        @Test
        void should_start_endpoint_using_configured_tenant() throws Exception {
            when(gatewayConfiguration.tenant()).thenReturn(Optional.of(TENANT_1));
            final Api api = buildApiWithEndpoints(() ->
                List.of(
                    anEndpointWithInheritedConfig().toBuilder().tenants(List.of(TENANT_1)).build(),
                    anEndpointWithInheritedConfig().toBuilder().tenants(List.of(TENANT_2)).build(),
                    anEndpointWithInheritedConfig().toBuilder().tenants(Collections.emptyList()).build()
                )
            );

            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(eq(deploymentContext), anyString(), anyString())).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();

            final ManagedEndpoint next = cut.next();
            assertThat(next).isNotNull();

            // Should deploy the enpoint with tenant-1 and the one without any tenant
            verify(connectorFactory, times(2)).createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG);
        }

        @Test
        void should_start_all_endpoints_when_gateway_does_not_have_any_tenant_configured() throws Exception {
            when(gatewayConfiguration.tenant()).thenReturn(Optional.empty());
            final Api api = buildApiWithEndpoints(() ->
                List.of(
                    anEndpointWithInheritedConfig().toBuilder().tenants(List.of(TENANT_1)).build(),
                    anEndpointWithInheritedConfig().toBuilder().tenants(List.of(TENANT_2)).build(),
                    anEndpointWithInheritedConfig().toBuilder().tenants(Collections.emptyList()).build()
                )
            );

            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(eq(deploymentContext), anyString(), anyString())).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();

            final ManagedEndpoint next = cut.next();
            assertThat(next).isNotNull();

            verify(connectorFactory, times(3)).createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG);
        }
    }

    @Nested
    class Stop {

        @Test
        void should_ignore_error_when_pre_stop_endpoint_connectors() throws Exception {
            final Api api = buildApi();

            final EndpointConnector connector1 = mock(EndpointConnector.class);
            final EndpointConnector connector2 = mock(EndpointConnector.class);
            final EndpointConnector connector3 = mock(EndpointConnector.class);
            final EndpointConnector connector4 = mock(EndpointConnector.class);

            when(connector2.preStop()).thenThrow(new Exception(MOCK_EXCEPTION));
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(
                connector1,
                connector2,
                connector3,
                connector4
            );

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            cut.preStop();

            verify(connector1).preStop();
            verify(connector2).preStop();
            verify(connector3).preStop();
            verify(connector4).preStop();
        }

        @Test
        void should_ignore_error_when_stop_endpoint_connectors() throws Exception {
            final Api api = buildApi();

            final EndpointConnector connector1 = mock(EndpointConnector.class);
            final EndpointConnector connector2 = mock(EndpointConnector.class);
            final EndpointConnector connector3 = mock(EndpointConnector.class);
            final EndpointConnector connector4 = mock(EndpointConnector.class);

            when(connector2.stop()).thenThrow(new Exception(MOCK_EXCEPTION));
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(
                connector1,
                connector2,
                connector3,
                connector4
            );

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            cut.stop();

            verify(connector1).stop();
            verify(connector2).stop();
            verify(connector3).stop();
            verify(connector4).stop();
        }
    }

    @Nested
    @ExtendWith(VertxExtension.class)
    class AddOrUpdateEndpoint {

        @Test
        void should_provide_updated_endpoints_template_variable_when_endpoint_is_added() throws Exception {
            var templateContext = mock(TemplateContext.class);
            var api = buildApi();
            var groupName = api.getEndpointGroups().get(0).getName();
            var newEndpoint = anEndpointWithInheritedConfig();

            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(eq(deploymentContext), anyString(), anyString())).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            cut.provide(templateContext);

            final ArgumentCaptor<Map<String, String>> endpointsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(templateContext).setVariable(eq("endpoints"), endpointsCaptor.capture());

            assertThat(endpointsCaptor.getValue()).hasSize(6);
            reset(templateContext);

            cut.addOrUpdateEndpoint(groupName, newEndpoint);
            cut.provide(templateContext);

            verify(templateContext).setVariable(eq("endpoints"), endpointsCaptor.capture());
            assertThat(endpointsCaptor.getValue()).hasSize(7).containsKey(newEndpoint.getName());
        }

        @Test
        void should_notify_listeners_when_endpoint_is_added(VertxTestContext context) throws Exception {
            var api = buildApi();
            var groupName = api.getEndpointGroups().get(0).getName();
            var newEndpoint = anEndpointWithInheritedConfig();

            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(eq(deploymentContext), anyString(), anyString())).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();

            cut.addListener((event, endpoint) -> {
                assertThat(event).isEqualTo(EndpointManager.Event.ADD);
                assertThat(endpoint.getDefinition().getName()).isEqualTo(newEndpoint.getName());
                context.completeNow();
            });
            cut.addOrUpdateEndpoint(groupName, newEndpoint);
        }

        @Test
        void should_notify_listeners_when_endpoint_is_updated(VertxTestContext context) throws Exception {
            var checkpointEndpointRemoved = context.checkpoint();
            var checkpointEndpointAdded = context.checkpoint();

            var api = buildApi();
            var groupName = api.getEndpointGroups().get(0).getName();
            var endpointToUpdate = api.getEndpointGroups().get(0).getEndpoints().get(0);
            var updatedEndpoint = Endpoint.builder()
                .name(endpointToUpdate.getName())
                .type(endpointToUpdate.getType())
                .configuration("updated")
                .inheritConfiguration(true)
                .build();

            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(eq(deploymentContext), anyString(), anyString())).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();

            var endpointRemoved = new AtomicBoolean(false);
            cut.addListener((event, endpoint) -> {
                if (event.equals(EndpointManager.Event.REMOVE)) {
                    checkpointEndpointRemoved.flag();
                    assertThat(endpoint.getDefinition().getName()).isEqualTo(updatedEndpoint.getName());
                    endpointRemoved.set(true);
                }

                if (event.equals(EndpointManager.Event.ADD)) {
                    checkpointEndpointAdded.flag();
                    assertThat(endpoint.getDefinition().getName()).isEqualTo(updatedEndpoint.getName());
                    assertThat(endpointRemoved.get()).describedAs("Endpoint should have been removed first").isTrue();
                }
            });
            cut.addOrUpdateEndpoint(groupName, updatedEndpoint);
        }
    }

    @Nested
    @ExtendWith(VertxExtension.class)
    class RemoveEndpoint {

        @Test
        void should_provide_updated_endpoints_template_variable_when_endpoint_is_removed() throws Exception {
            final TemplateContext templateContext = mock(TemplateContext.class);
            final Api api = buildApi();

            final EndpointConnector connector = mock(EndpointConnector.class);

            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);
            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);

            cut.start();
            cut.provide(templateContext);

            final ArgumentCaptor<Map<String, String>> endpointsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(templateContext).setVariable(eq("endpoints"), endpointsCaptor.capture());

            final String endpointToRemove = api.getEndpointGroups().get(0).getEndpoints().get(0).getName();
            assertThat(endpointsCaptor.getValue()).hasSize(6).containsKey(endpointToRemove);

            reset(templateContext);
            cut.removeEndpoint(endpointToRemove);
            cut.provide(templateContext);

            verify(templateContext).setVariable(eq("endpoints"), endpointsCaptor.capture());
            assertThat(endpointsCaptor.getValue()).hasSize(5).doesNotContainKey(endpointToRemove);
        }

        @Test
        void should_notify_listeners(VertxTestContext context) throws Exception {
            final Api api = buildApi();
            final String endpointToRemove = api.getEndpointGroups().get(0).getEndpoints().get(0).getName();

            final EndpointConnector connector1 = mock(EndpointConnector.class);
            final EndpointConnector connector2 = mock(EndpointConnector.class);
            final EndpointConnector connector3 = mock(EndpointConnector.class);
            final EndpointConnector connector4 = mock(EndpointConnector.class);

            when(connectorFactory.createConnector(eq(deploymentContext), anyString(), anyString())).thenReturn(
                connector1,
                connector2,
                connector3,
                connector4
            );

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();

            cut.addListener((event, endpoint) -> {
                assertThat(event).isEqualTo(EndpointManager.Event.REMOVE);
                assertThat(endpoint.getDefinition().getName()).isEqualTo(endpointToRemove);
                context.completeNow();
            });

            cut.removeEndpoint(endpointToRemove);
        }
    }

    @Nested
    class Next {

        @Test
        void should_return_null_managed_endpoint_when_not_started() {
            final Api api = buildApi();

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            assertThat(cut.next()).isNull();
        }

        @Test
        void should_return_next_managed_endpoint() throws Exception {
            final Api api = buildApi();

            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next();

            assertThat(next).isNotNull();
            assertThat(next.getDefinition()).isEqualTo(api.getEndpointGroups().get(0).getEndpoints().get(0));
            assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
            assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
            assertThat(next.getGroup()).isNotNull();
            assertThat(next.getGroup().getDefinition()).isEqualTo(api.getEndpointGroups().get(0));
        }

        @Test
        void should_return_next_managed_endpoint_by_name() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
            final String endpointName = expectedEndpoint.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next(new EndpointCriteria(endpointName, null, null));

            assertThat(next).isNotNull();
            assertThat(next.getDefinition()).isEqualTo(expectedEndpoint);
            assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
            assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
            assertThat(next.getGroup()).isNotNull();
            assertThat(next.getGroup().getDefinition()).isEqualTo(expectedEndpointGroup);
        }

        @Test
        void should_return_next_managed_endpoint_by_group_name() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final String groupName = expectedEndpointGroup.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next(new EndpointCriteria(groupName, null, null));

            assertThat(next).isNotNull();
            assertThat(next.getDefinition()).isEqualTo(expectedEndpointGroup.getEndpoints().get(0)); // First endpoint of the group is expected.
            assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
            assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
            assertThat(next.getGroup()).isNotNull();
            assertThat(next.getGroup().getDefinition()).isEqualTo(expectedEndpointGroup);
        }

        @Test
        void should_return_null_when_endpoint_by_name_is_not_available() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
            final String endpointName = expectedEndpoint.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            ManagedEndpoint next = cut.next(new EndpointCriteria(endpointName, null, null));

            assertThat(next).isNotNull();
            cut.disable(next);

            next = cut.next(new EndpointCriteria(endpointName, null, null));
            assertThat(next).isNull();
        }

        @Test
        void should_return_next_managed_endpoint_when_endpoint_by_name_available_again() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
            final String endpointName = expectedEndpoint.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            final EndpointCriteria criteria = new EndpointCriteria(endpointName, null, null);

            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            ManagedEndpoint next = cut.next(criteria);
            cut.disable(next);

            // Should be null because disabled.
            assertThat(cut.next(criteria)).isNull();

            cut.enable(next);
            assertThat(cut.next(criteria)).isNotNull();
        }

        @Test
        void should_return_null_when_name_not_found() throws Exception {
            final Api api = buildApi();
            final EndpointConnector connector = mock(EndpointConnector.class);

            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            ManagedEndpoint next = cut.next(new EndpointCriteria("UNKNOWN", null, null));

            assertThat(next).isNull();
        }

        @Test
        void should_return_null_endpoint_when_no_connector_factory_found() throws Exception {
            final Api api = buildApi();

            // Simulate no connector factory available.
            when(pluginManager.getFactoryById(ENDPOINT_TYPE)).thenReturn(null);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            ManagedEndpoint next = cut.next();

            assertThat(next).isNull();
        }

        @Test
        void should_return_null_endpoint_when_exception_occurred() throws Exception {
            final Api api = buildApi();

            // Simulate an unexpected exception.
            when(pluginManager.getFactoryById(ENDPOINT_TYPE)).thenThrow(new RuntimeException(MOCK_EXCEPTION));

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            ManagedEndpoint next = cut.next();

            assertThat(next).isNull();
        }

        @Test
        void should_return_null_endpoint_when_no_connector_created() throws Exception {
            final Api api = buildApi();

            // Simulate connector factory returns null connector.
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(null);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            ManagedEndpoint next = cut.next();

            assertThat(next).isNull();
        }

        @Test
        void should_return_null_when_not_supporting_modes() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final String groupName = expectedEndpointGroup.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connector.supportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH));
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next(
                new EndpointCriteria(groupName, null, Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE))
            );

            assertThat(next).isNull();
        }

        @Test
        void should_return_null_managed_endpoint_by_name_when_not_supporting_mode() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
            final String endpointName = expectedEndpoint.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connector.supportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH));
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next(
                new EndpointCriteria(endpointName, null, Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE))
            );

            assertThat(next).isNull();
        }

        @Test
        void should_return_managed_endpoint_when_supporting_modes() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final String groupName = expectedEndpointGroup.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connector.supportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE));
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next(
                new EndpointCriteria(groupName, null, Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE))
            );

            assertThat(next).isNotNull();
            assertThat(next.getDefinition()).isEqualTo(expectedEndpointGroup.getEndpoints().get(0)); // First endpoint of the group is expected.
            assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
            assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
            assertThat(next.getGroup()).isNotNull();
            assertThat(next.getGroup().getDefinition()).isEqualTo(expectedEndpointGroup);
        }

        @Test
        void should_return_null_when_not_supporting_api_type() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final String groupName = expectedEndpointGroup.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connector.supportedApi()).thenReturn(ApiType.MESSAGE);
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next(new EndpointCriteria(groupName, ApiType.PROXY, null));

            assertThat(next).isNull();
        }

        @Test
        void should_return_null_managed_endpoint_by_name_when_not_supporting_api_type() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final Endpoint expectedEndpoint = expectedEndpointGroup.getEndpoints().get(1);
            final String endpointName = expectedEndpoint.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connector.supportedApi()).thenReturn(ApiType.MESSAGE);
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();
            final ManagedEndpoint next = cut.next(new EndpointCriteria(endpointName, ApiType.PROXY, null));

            assertThat(next).isNull();
        }

        @Test
        void should_return_managed_endpoint_when_supporting_api_type() throws Exception {
            final Api api = buildApi();

            final EndpointGroup expectedEndpointGroup = api.getEndpointGroups().get(1);
            final String groupName = expectedEndpointGroup.getName();
            final EndpointConnector connector = mock(EndpointConnector.class);
            when(connector.supportedApi()).thenReturn(ApiType.MESSAGE);
            when(connectorFactory.createConnector(deploymentContext, ENDPOINT_CONFIG, ENDPOINT_GROUP_SHARED_CONFIG)).thenReturn(connector);

            final DefaultEndpointManager cut = new DefaultEndpointManager(api, pluginManager, deploymentContext, gatewayConfiguration);
            cut.start();

            final ManagedEndpoint next = cut.next(new EndpointCriteria(groupName, ApiType.MESSAGE, null));

            assertThat(next).isNotNull();
            assertThat(next.getDefinition()).isEqualTo(expectedEndpointGroup.getEndpoints().get(0)); // First endpoint of the group is expected.
            assertThat((EndpointConnector) next.getConnector()).isEqualTo(connector);
            assertThat(next.getStatus()).isEqualTo(ManagedEndpoint.Status.UP);
            assertThat(next.getGroup()).isNotNull();
            assertThat(next.getGroup().getDefinition()).isEqualTo(expectedEndpointGroup);
        }
    }

    private Api buildApi() {
        return buildApi(this::anEndpointWithInheritedConfig);
    }

    private Api buildApi(Supplier<Endpoint> endpointSupplier) {
        final Api api = new Api();
        final ArrayList<EndpointGroup> endpointGroups = new ArrayList<>();
        api.setEndpointGroups(endpointGroups);

        endpointGroups.add(anEndpointGroup(endpointSupplier));
        endpointGroups.add(anEndpointGroup(endpointSupplier));
        return api;
    }

    private EndpointGroup anEndpointGroup(Supplier<Endpoint> endpointSupplier) {
        return EndpointGroup.builder()
            .name(randomUUID().toString())
            .type(ENDPOINT_TYPE)
            .sharedConfiguration(ENDPOINT_GROUP_SHARED_CONFIG)
            .endpoints(List.of(endpointSupplier.get(), endpointSupplier.get()))
            .build();
    }

    private Api buildApiWithEndpoints(Supplier<List<Endpoint>> endpointsSupplier) {
        final Api api = new Api();
        final ArrayList<EndpointGroup> endpointGroups = new ArrayList<>();
        api.setEndpointGroups(endpointGroups);

        endpointGroups.add(anEndpointGroupFromEndpoints(endpointsSupplier));
        return api;
    }

    private EndpointGroup anEndpointGroupFromEndpoints(Supplier<List<Endpoint>> endpointsSupplier) {
        return EndpointGroup.builder()
            .name(randomUUID().toString())
            .type(ENDPOINT_TYPE)
            .sharedConfiguration(ENDPOINT_GROUP_SHARED_CONFIG)
            .endpoints(endpointsSupplier.get())
            .build();
    }

    Endpoint anEndpointWithSharedConfigurationOverride(String configuration) {
        return Endpoint.builder()
            .name(randomUUID().toString())
            .type(ENDPOINT_TYPE)
            .configuration(ENDPOINT_CONFIG)
            .inheritConfiguration(false)
            .sharedConfigurationOverride(configuration)
            .build();
    }

    Endpoint anEndpointWithInheritedConfig() {
        return Endpoint.builder()
            .name(randomUUID().toString())
            .type(ENDPOINT_TYPE)
            .configuration(ENDPOINT_CONFIG)
            .inheritConfiguration(true)
            .build();
    }
}
