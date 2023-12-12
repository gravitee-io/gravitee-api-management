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

package io.gravitee.apim.infra.domain_service.integration;

import static io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService.APIM_INTEGRATION;

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.domain_service.IntegrationDomainService;
import io.gravitee.apim.core.integration.model.IntegrationEntity;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.common.service.AbstractService;
import io.gravitee.exchange.api.command.Command;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.exchange.api.command.Reply;
import io.gravitee.exchange.api.command.ReplyHandler;
import io.gravitee.exchange.api.connector.ExchangeConnectorManager;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.exchange.connector.embedded.EmbeddedExchangeConnector;
import io.gravitee.exchange.controller.embedded.channel.EmbeddedChannel;
import io.gravitee.integration.api.Entity;
import io.gravitee.integration.api.IntegrationProvider;
import io.gravitee.integration.api.IntegrationProviderFactory;
import io.gravitee.integration.api.command.fetch.FetchCommand;
import io.gravitee.integration.api.command.fetch.FetchCommandPayload;
import io.gravitee.integration.api.command.fetch.FetchReply;
import io.gravitee.integration.api.command.list.ListCommand;
import io.gravitee.integration.api.command.list.ListReply;
import io.gravitee.integration.api.model.Integration;
import io.gravitee.integration.connector.command.IntegrationConnectorCommandContext;
import io.gravitee.integration.connector.command.IntegrationConnectorCommandHandlerFactory;
import io.gravitee.plugin.integrationprovider.IntegrationProviderPluginManager;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationDomainServiceImpl extends AbstractService<IntegrationDomainService> implements IntegrationDomainService {

    private final GraviteeLicenseDomainService graviteeLicenseDomainService;
    private final ExchangeConnectorManager exchangeConnectorManager;
    private final ExchangeController exchangeController;
    private final IntegrationConnectorCommandHandlerFactory connectorCommandHandlersFactory;
    private final IntegrationProviderPluginManager integrationProviderPluginManager;
    private final IntegrationCrudService integrationCrudService;

    // TODO To be removed when the license is up to date
    private final boolean FORCE_INTEGRATION = true;

    @Override
    public void doStart() throws Exception {
        super.doStart();
        if (graviteeLicenseDomainService.isFeatureEnabled(APIM_INTEGRATION) || FORCE_INTEGRATION) {
            exchangeController.start();

            integrationCrudService.findAll().forEach(this::startIntegration);
            log.info("Integrations started.");
        } else {
            log.warn("License doesn't contain Integrations feature.");
        }
    }

    @Override
    public void startIntegration(Integration integration) {
        if (integration.getDeploymentType() == Integration.DeploymentType.EMBEDDED) {
            try {
                IntegrationProviderFactory<?> integrationProviderFactory = integrationProviderPluginManager.getIntegrationProviderFactory(
                    integration.getProvider().toLowerCase()
                );

                if (integrationProviderFactory == null) {
                    log.warn("Integration provider {} cannot be instantiated (no factory found). Skipped.", integration.getProvider());
                    return;
                }

                IntegrationProvider integrationProvider = integrationProviderFactory.createIntegrationProvider(
                    integration.getId(),
                    integration.getConfiguration()
                );

                if (integrationProvider == null) {
                    log.warn("Integration provider {} cannot be started. Skipped.", integration.getProvider());
                    return;
                }

                integrationProvider.start();

                IntegrationConnectorCommandContext integrationConnectorCommandContext = new IntegrationConnectorCommandContext(
                    integration.getProvider(),
                    integration.getId(),
                    integration.getEnvironmentId(),
                    integrationProvider
                );
                Map<String, CommandHandler<? extends Command<?>, ? extends Reply<?>>> connectorCommandHandlers =
                    connectorCommandHandlersFactory.buildCommandHandlers(integrationConnectorCommandContext);
                Map<String, ReplyHandler<? extends Command<?>, ? extends Command<?>, ? extends Reply<?>>> connectorReplyHandlers =
                    connectorCommandHandlersFactory.buildReplyHandlers(integrationConnectorCommandContext);
                EmbeddedChannel embeddedChannel = EmbeddedChannel
                    .builder()
                    .targetId(integration.getId())
                    .commandHandlers(connectorCommandHandlers)
                    .replyHandlers(connectorReplyHandlers)
                    .build();
                exchangeController
                    .register(embeddedChannel)
                    .andThen(
                        exchangeConnectorManager.register(EmbeddedExchangeConnector.builder().connectorChannel(embeddedChannel).build())
                    )
                    .blockingAwait();
            } catch (Exception e) {
                log.warn("Unable to properly start the integration provider {}: {}. Skipped.", integration.getProvider(), e.getMessage());
            }
        }
    }

    @Override
    public Flowable<IntegrationEntity> getIntegrationEntities(Integration integration) {
        ListCommand listCommand = new ListCommand();
        String targetId = integration.getDeploymentType() == Integration.DeploymentType.EMBEDDED
            ? integration.getId()
            : integration.getRemoteId();
        return sendListCommand(listCommand, targetId)
            .flatMapPublisher(listReply -> {
                if (listReply.getCommandStatus() == CommandStatus.SUCCEEDED) {
                    List<IntegrationEntity> integrationEntities = listReply
                        .getPayload()
                        .entities()
                        .stream()
                        .map(IntegrationAdapter.INSTANCE::toEntity)
                        .toList();
                    return Flowable.fromIterable(integrationEntities);
                }
                return Flowable.empty();
            });
    }

    @Override
    public Flowable<IntegrationEntity> fetchEntities(Integration integration, List<IntegrationEntity> integrationEntities) {
        List<Entity> entities = integrationEntities.stream().map(IntegrationAdapter.INSTANCE::toEntityApi).toList();

        FetchCommandPayload fetchCommandPayload = new FetchCommandPayload(entities);
        FetchCommand fetchCommand = new FetchCommand(fetchCommandPayload);
        String targetId = integration.getDeploymentType() == Integration.DeploymentType.EMBEDDED
            ? integration.getId()
            : integration.getRemoteId();
        return sendFetchCommand(fetchCommand, targetId)
            .toFlowable()
            .flatMap(fetchReply -> {
                if (fetchReply.getCommandStatus() == CommandStatus.SUCCEEDED) {
                    List<IntegrationEntity> fetchEntities = fetchReply
                        .getPayload()
                        .entities()
                        .stream()
                        .map(IntegrationAdapter.INSTANCE::toEntity)
                        .toList();
                    return Flowable.fromIterable(fetchEntities);
                }
                return Flowable.empty();
            });
    }

    private Single<ListReply> sendListCommand(ListCommand listCommand, String integrationId) {
        return exchangeController
            .sendCommand(listCommand, integrationId)
            .cast(ListReply.class)
            .defaultIfEmpty(new ListReply(listCommand.getId(), "Command received no reply"))
            .onErrorReturn(throwable -> new ListReply(listCommand.getId(), throwable.getMessage()));
    }

    private Single<FetchReply> sendFetchCommand(FetchCommand fetchCommand, String integrationId) {
        return exchangeController
            .sendCommand(fetchCommand, integrationId)
            .cast(FetchReply.class)
            .defaultIfEmpty(new FetchReply(fetchCommand.getId(), "Command received no reply"))
            .onErrorReturn(throwable -> new FetchReply(fetchCommand.getId(), throwable.getMessage()));
    }
}
