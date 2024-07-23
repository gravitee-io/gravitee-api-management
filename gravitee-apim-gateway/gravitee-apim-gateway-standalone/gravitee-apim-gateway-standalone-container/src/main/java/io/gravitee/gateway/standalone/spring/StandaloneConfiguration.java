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
package io.gravitee.gateway.standalone.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.el.ExpressionLanguageInitializer;
import io.gravitee.gateway.connector.spring.ConnectorConfiguration;
import io.gravitee.gateway.dictionary.spring.DictionaryConfiguration;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.TimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.spring.AccessPointConfiguration;
import io.gravitee.gateway.handlers.api.spring.ApiHandlerConfiguration;
import io.gravitee.gateway.handlers.sharedpolicygroup.spring.SharedPolicyGroupConfiguration;
import io.gravitee.gateway.platform.spring.PlatformConfiguration;
import io.gravitee.gateway.policy.spring.PolicyConfiguration;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactor.spring.ReactorConfiguration;
import io.gravitee.gateway.report.spring.ReporterConfiguration;
import io.gravitee.gateway.repository.plugins.GatewayRepositoryScopeProvider;
import io.gravitee.gateway.standalone.node.GatewayNode;
import io.gravitee.gateway.standalone.node.GatewayNodeMetadataResolver;
import io.gravitee.gateway.standalone.vertx.VertxReactorConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.NodeMetadataResolver;
import io.gravitee.platform.repository.api.RepositoryScopeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import(
    {
        ReactorConfiguration.class,
        VertxReactorConfiguration.class,
        ReporterConfiguration.class,
        ApiHandlerConfiguration.class,
        AccessPointConfiguration.class,
        SharedPolicyGroupConfiguration.class,
        DictionaryConfiguration.class,
        PolicyConfiguration.class,
        PlatformConfiguration.class,
        ConnectorConfiguration.class,
        TimeoutConfiguration.class,
    }
)
public class StandaloneConfiguration {

    @Bean
    public Node node() {
        return new GatewayNode();
    }

    @Bean
    public static GatewayConfiguration gatewayConfiguration() {
        return new GatewayConfiguration();
    }

    @Bean
    public EventManager eventManager() {
        return new EventManagerImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new GraviteeMapper(false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    public PluginConfigurationHelper connectorHelper(
        final io.gravitee.node.api.configuration.Configuration configuration,
        final ObjectMapper objectMapper
    ) {
        return new PluginConfigurationHelper(configuration, objectMapper);
    }

    @Bean
    public ExpressionLanguageInitializer expressionLanguageInitializer() {
        return new ExpressionLanguageInitializer();
    }

    @Bean
    public RepositoryScopeProvider repositoryScopeProvider() {
        return new GatewayRepositoryScopeProvider();
    }

    @Bean
    public NodeMetadataResolver nodeMetadataResolver() {
        return new GatewayNodeMetadataResolver();
    }
}
