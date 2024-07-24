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
package io.gravitee.integration.controller.spring;

import static io.gravitee.integration.controller.spring.IntegrationControllerCondition.INTEGRATION_IDENTIFIER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.gravitee.apim.core.integration.use_case.UpdateAgentStatusUseCase;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.exchange.api.configuration.IdentifyConfiguration;
import io.gravitee.exchange.api.controller.ControllerCommandHandlersFactory;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.exchange.api.websocket.command.ExchangeSerDe;
import io.gravitee.exchange.controller.websocket.WebSocketExchangeController;
import io.gravitee.exchange.controller.websocket.auth.WebSocketControllerAuthentication;
import io.gravitee.integration.api.websocket.command.IntegrationExchangeSerDe;
import io.gravitee.integration.controller.command.IntegrationControllerCommandHandlerFactory;
import io.gravitee.integration.controller.listener.ControllerEventListener;
import io.gravitee.integration.controller.websocket.auth.IntegrationWebsocketControllerAuthentication;
import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.rest.api.service.TokenService;
import io.vertx.rxjava3.core.Vertx;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

@Configuration
@Conditional(IntegrationControllerCondition.class)
public class IntegrationControllerConfiguration {

    @Bean("integrationWebsocketControllerAuthentication")
    public IntegrationWebsocketControllerAuthentication integrationWebsocketControllerAuthentication(
        final TokenService tokenService,
        final UserCrudService userCrudService,
        final LicenseDomainService licenseDomainService
    ) {
        return new IntegrationWebsocketControllerAuthentication(tokenService, userCrudService, licenseDomainService);
    }

    @Bean("integrationExchangeSerDe")
    public IntegrationExchangeSerDe integrationExchangeSerDe() {
        return new IntegrationExchangeSerDe(objectMapper());
    }

    @Bean("integrationIdentifyConfiguration")
    public IdentifyConfiguration integrationPrefixConfiguration(final Environment environment) {
        return new IdentifyConfiguration(environment, INTEGRATION_IDENTIFIER);
    }

    @Bean("integrationControllerCommandHandlerFactory")
    public IntegrationControllerCommandHandlerFactory integrationControllerCommandHandlerFactory(
        final UpdateAgentStatusUseCase updateAgentStatusUseCase
    ) {
        return new IntegrationControllerCommandHandlerFactory(updateAgentStatusUseCase);
    }

    @Bean("integrationControllerEventListener")
    ControllerEventListener controllerEventListener(UpdateAgentStatusUseCase updateAgentStatusUseCase) {
        return new ControllerEventListener(updateAgentStatusUseCase);
    }

    @Bean("integrationExchangeController")
    public ExchangeController integrationExchangeController(
        final @Lazy ClusterManager clusterManager,
        final @Lazy CacheManager cacheManager,
        final @Lazy ManagementEndpointManager managementEndpointManager,
        final Vertx vertx,
        final KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        final KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry,
        final @Qualifier("integrationIdentifyConfiguration") IdentifyConfiguration identifyConfiguration,
        final @Qualifier(
            "integrationWebsocketControllerAuthentication"
        ) WebSocketControllerAuthentication<?> integrationWebsocketControllerAuthentication,
        final @Qualifier(
            "integrationControllerCommandHandlerFactory"
        ) ControllerCommandHandlersFactory integrationControllerCommandHandlerFactory,
        final @Qualifier("integrationExchangeSerDe") ExchangeSerDe integrationExchangeSerDe,
        final @Qualifier("integrationControllerEventListener") ControllerEventListener eventListener
    ) {
        return new WebSocketExchangeController(
            identifyConfiguration,
            clusterManager,
            cacheManager,
            managementEndpointManager,
            vertx,
            keyStoreLoaderFactoryRegistry,
            trustStoreLoaderFactoryRegistry,
            integrationWebsocketControllerAuthentication,
            integrationControllerCommandHandlerFactory,
            integrationExchangeSerDe
        )
            .addListener(eventListener);
    }

    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
