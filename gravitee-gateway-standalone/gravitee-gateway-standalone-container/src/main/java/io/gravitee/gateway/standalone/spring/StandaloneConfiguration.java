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
package io.gravitee.gateway.standalone.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.common.node.Node;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.env.EnvironmentConfiguration;
import io.gravitee.gateway.handlers.api.spring.ApiHandlerConfiguration;
import io.gravitee.gateway.reactor.spring.ReactorConfiguration;
import io.gravitee.gateway.report.spring.ReporterConfiguration;
import io.gravitee.gateway.services.spring.ServiceConfiguration;
import io.gravitee.gateway.standalone.node.GatewayNode;
import io.gravitee.gateway.standalone.vertx.VertxConfiguration;
import io.gravitee.plugin.core.spring.PluginConfiguration;
import io.gravitee.plugin.policy.spring.PolicyPluginConfiguration;
import io.gravitee.plugin.resource.spring.ResourcePluginConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import({
        EnvironmentConfiguration.class,
        ReactorConfiguration.class,
        VertxConfiguration.class,
        ServiceConfiguration.class,
        PluginConfiguration.class,
        PolicyPluginConfiguration.class,
        ResourcePluginConfiguration.class,
        ReporterConfiguration.class,
        ApiHandlerConfiguration.class
})
public class StandaloneConfiguration {

    @Bean
    public EventManager eventManager() {
        return new EventManagerImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new GraviteeMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    public Node node() {
        return new GatewayNode();
    }
}
