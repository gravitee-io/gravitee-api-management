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

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.domain_service.IntegrationDomainService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationEntity;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.common.service.AbstractService;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.exchange.controller.embedded.channel.EmbeddedChannel;
import io.gravitee.integration.api.DeploymentType;
import io.gravitee.integration.api.Entity;
import io.gravitee.integration.api.IntegrationProvider;
import io.gravitee.integration.api.IntegrationProviderFactory;
import io.gravitee.integration.api.command.IntegrationProviderCommandHandlerFactory;
import io.gravitee.integration.api.command.fetch.FetchCommand;
import io.gravitee.integration.api.command.fetch.FetchCommandPayload;
import io.gravitee.integration.api.command.fetch.FetchReply;
import io.gravitee.integration.api.command.list.ListCommand;
import io.gravitee.integration.api.command.list.ListReply;
import io.gravitee.plugin.integrationprovider.internal.DefaultIntegrationProviderPluginManager;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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

    private final ExchangeController exchangeController;
    private final DefaultIntegrationProviderPluginManager integrationProviderPluginManager;
    private final IntegrationCrudService integrationCrudService;

    @Override
    public void doStart() throws Exception {
        super.doStart();

        integrationCrudService.findAll().forEach(this::startIntegration);
        log.info("Integrations started");
    }

    @Override
    public void startIntegration(Integration integration) {
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
                DeploymentType.valueOf(integration.getDeploymentType().name()),
                integration.getConfiguration()
            );

            if (integrationProvider == null) {
                log.warn("Integration provider {} cannot be started. Skipped.", integration.getProvider());
                return;
            }

            integrationProvider.start();

            exchangeController
                .register(
                    EmbeddedChannel
                        .builder()
                        .targetId(integration.getId())
                        .commandHandlers(IntegrationProviderCommandHandlerFactory.buildHandlers(integrationProvider))
                        .build()
                )
                .blockingAwait();
        } catch (Exception e) {
            log.warn("Unable to properly start the integration provider {}: {}. Skipped.", integration.getProvider(), e.getMessage());
        }
    }

    @Override
    public Single<List<IntegrationEntity>> getIntegrationEntities(Integration integration) {
        ListCommand listCommand = new ListCommand();

        return sendListCommand(listCommand, integration.getId())
            .flatMap(listReply -> {
                if (listReply.getCommandStatus() == CommandStatus.SUCCEEDED) {
                    return Single.just(listReply.getPayload().entities().stream().map(IntegrationAdapter.INSTANCE::toEntity).toList());
                }
                return Single.just(Collections.emptyList());
            });
    }

    @Override
    public Single<List<IntegrationEntity>> fetchEntities(Integration integration, List<IntegrationEntity> integrationEntities) {
        List<Entity> entities = integrationEntities.stream().map(IntegrationAdapter.INSTANCE::toEntityApi).toList();

        FetchCommandPayload fetchCommandPayload = new FetchCommandPayload(entities);
        FetchCommand fetchCommand = new FetchCommand(fetchCommandPayload);

        return sendFetchCommand(fetchCommand, integration.getId())
            .flatMap(fetchReply -> {
                if (fetchReply.getCommandStatus() == CommandStatus.SUCCEEDED) {
                    return Single.just(fetchReply.getPayload().entities().stream().map(IntegrationAdapter.INSTANCE::toEntity).toList());
                }
                return Single.just(Collections.emptyList());
            });
    }

    @Override
    public void importEntities(List<String> entitiesId) {}

    private Single<ListReply> sendListCommand(ListCommand listCommand, String integrationId) {
        return exchangeController
            .sendCommand(listCommand, integrationId)
            .cast(ListReply.class)
            .switchIfEmpty(
                Single.defer(() ->
                    Single.just(
                        ListReply
                            .builder()
                            .commandId(listCommand.getId())
                            .commandStatus(CommandStatus.ERROR)
                            .errorDetails("Command received no reply")
                            .build()
                    )
                )
            )
            .onErrorReturn(throwable ->
                ListReply
                    .builder()
                    .commandId(listCommand.getId())
                    .commandStatus(CommandStatus.ERROR)
                    .errorDetails(throwable.getMessage())
                    .build()
            );
    }

    private Single<FetchReply> sendFetchCommand(FetchCommand fetchCommand, String integrationId) {
        return exchangeController
            .sendCommand(fetchCommand, integrationId)
            .cast(FetchReply.class)
            .switchIfEmpty(
                Single.defer(() ->
                    Single.just(
                        FetchReply
                            .builder()
                            .commandId(fetchCommand.getId())
                            .commandStatus(CommandStatus.ERROR)
                            .errorDetails("Command received no reply")
                            .build()
                    )
                )
            )
            .onErrorReturn(throwable ->
                FetchReply
                    .builder()
                    .commandId(fetchCommand.getId())
                    .commandStatus(CommandStatus.ERROR)
                    .errorDetails(throwable.getMessage())
                    .build()
            );
    }
}
