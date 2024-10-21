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
package io.gravitee.apim.gateway.tests.sdk.container;

import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MessageStorage;
import io.gravitee.apim.gateway.tests.sdk.license.PermissiveLicenseManager;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.standalone.GatewayContainer;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.container.NodeFactory;
import io.gravitee.node.monitoring.spring.NodeMonitoringConfiguration;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.node.plugin.cache.standalone.StandaloneCacheManager;
import io.gravitee.node.plugin.cluster.standalone.StandaloneClusterManager;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.function.Consumer;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * This class allows to extend the {@link GatewayContainer} in order to override the {@link NodeFactory}
 * and provide a {@link GatewayTestNode}
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayTestContainer extends GatewayContainer {

    private final Consumer<ApplicationContext> onBootApplicationContextCreated;

    /**
     * Constructor with action to execute once the boot application context has been created.
     * This allows for setting up specific requirements for testing (e.g. customizing gravitee yaml configuration, registering boot plugins, ...)
     *
     * @param onBootApplicationContextCreated actions to execute once Spring context is created and before starting components.
     */
    public GatewayTestContainer(Consumer<ApplicationContext> onBootApplicationContextCreated) {
        this.onBootApplicationContextCreated = onBootApplicationContextCreated;
    }

    @Override
    protected List<Class<?>> annotatedClasses() {
        List<Class<?>> classes = super.annotatedClasses();
        classes.add(GatewayTestConfiguration.class);
        classes.remove(NodeMonitoringConfiguration.class);
        return classes;
    }

    @Override
    protected List<Class<?>> bootstrapClasses() {
        List<Class<?>> classes = super.bootstrapClasses();
        classes.add(GatewayTestNodeContainerConfiguration.class);
        return classes;
    }

    @Override
    protected void startBootstrapComponents(AnnotationConfigApplicationContext ctx) {
        // Execute operations before starting bootstrap components (e.g.: the boot application context has been created).
        onBootApplicationContextCreated.accept(ctx);
        super.startBootstrapComponents(ctx);
    }

    @Configuration
    public static class GatewayTestNodeContainerConfiguration {

        // Force use of the PermissiveLicenseManager
        @Primary
        @Bean
        public LicenseManager licenseManager() {
            return new PermissiveLicenseManager();
        }
    }

    @Configuration
    public static class GatewayTestConfiguration {

        @Bean
        public Node node() {
            return new GatewayTestNode();
        }

        @Bean
        public ClusterManager clusterManager(final Vertx vertx) {
            return new StandaloneClusterManager(vertx);
        }

        @Bean
        public CacheManager cacheManager() {
            return new StandaloneCacheManager();
        }

        @Bean
        public Tracer tracer() {
            return new Tracer(Vertx.vertx().getOrCreateContext(), new NoOpTracer());
        }

        @Bean
        public Reporter fakeReporter() {
            return new FakeReporter();
        }

        @Bean
        public InstallationRepository installationRepository() {
            return Mockito.mock(InstallationRepository.class);
        }

        @Bean
        public OrganizationRepository organizationRepository() {
            return Mockito.mock(OrganizationRepository.class);
        }

        @Bean
        public LicenseRepository licenseRepository() {
            return Mockito.mock(LicenseRepository.class);
        }

        @Bean
        public AccessPointRepository accessPointRepository() {
            return Mockito.mock(AccessPointRepository.class);
        }

        @Bean
        public EnvironmentRepository environmentRepository() {
            return Mockito.mock(EnvironmentRepository.class);
        }

        @Bean
        public SubscriptionRepository subscriptionRepository() {
            return Mockito.mock(SubscriptionRepository.class);
        }

        @Bean
        public EventRepository eventRepository() {
            return Mockito.mock(EventRepository.class);
        }

        @Bean
        public ApiKeyRepository apiKeyRepository() {
            return Mockito.mock(ApiKeyRepository.class);
        }

        @Bean
        public ApiKeyService apiKeyService() {
            return Mockito.mock(ApiKeyService.class);
        }

        @Bean
        public SubscriptionService subscriptionService() {
            return Mockito.mock(SubscriptionCacheService.class);
        }

        @Bean
        public MessageStorage messageStorage() {
            return new MessageStorage();
        }
    }
}
