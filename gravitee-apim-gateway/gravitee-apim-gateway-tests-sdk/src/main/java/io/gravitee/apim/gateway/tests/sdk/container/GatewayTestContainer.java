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
package io.gravitee.apim.gateway.tests.sdk.container;

import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.gateway.tests.sdk.tracer.NoOpTracer;
import io.gravitee.gateway.standalone.GatewayContainer;
import io.gravitee.node.container.NodeFactory;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.tracing.api.Tracer;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class allows to extends the {@link GatewayContainer} in order to be able to override the {@link NodeFactory}
 * and provide a {@link GatewayTestNode}
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayTestContainer extends GatewayContainer {

    @Override
    protected List<Class<?>> annotatedClasses() {
        List<Class<?>> classes = super.annotatedClasses();
        classes.add(GatewayTestConfiguration.class);
        return classes;
    }

    @Configuration
    public static class GatewayTestConfiguration {

        @Bean
        public NodeFactory node() {
            return new NodeFactory(GatewayTestNode.class);
        }

        @Bean
        public Tracer tracer() {
            return new NoOpTracer();
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
    }
}
