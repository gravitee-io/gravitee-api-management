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
package io.gravitee.apim.infra.integration;

import static io.gravitee.apim.core.integration.model.IntegrationSubscription.apiKey;
import static io.gravitee.apim.core.integration.model.IntegrationSubscription.oAuth;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.integration.exception.IntegrationDiscoveryException;
import io.gravitee.apim.core.integration.exception.IntegrationIngestionException;
import io.gravitee.apim.core.integration.exception.IntegrationSubscriptionException;
import io.gravitee.apim.core.integration.model.DiscoveredApis;
import io.gravitee.apim.core.integration.model.IngestStarted;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.model.IntegrationSubscription;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.federation.SubscriptionParameter;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.exchange.api.controller.metrics.ChannelMetric;
import io.gravitee.integration.api.command.discover.DiscoverCommand;
import io.gravitee.integration.api.command.discover.DiscoverReply;
import io.gravitee.integration.api.command.ingest.StartIngestCommand;
import io.gravitee.integration.api.command.ingest.StartIngestReply;
import io.gravitee.integration.api.command.subscribe.SubscribeCommand;
import io.gravitee.integration.api.command.subscribe.SubscribeReply;
import io.gravitee.integration.api.command.unsubscribe.UnsubscribeCommand;
import io.gravitee.integration.api.command.unsubscribe.UnsubscribeReply;
import io.gravitee.integration.api.model.Subscription;
import io.gravitee.integration.api.model.SubscriptionType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IntegrationAgentImpl implements IntegrationAgent {

    private final Optional<ExchangeController> exchangeController;

    public IntegrationAgentImpl(@Qualifier("integrationExchangeController") Optional<ExchangeController> exchangeController) {
        this.exchangeController = exchangeController;
    }

    @Override
    public Single<Status> getAgentStatusFor(String integrationId) {
        return Maybe.fromOptional(exchangeController)
            .flatMap(controller -> controller.channelsMetricsForTarget(integrationId).toList().toMaybe())
            .map(metrics -> metrics != null && metrics.stream().anyMatch(ChannelMetric::active) ? Status.CONNECTED : Status.DISCONNECTED)
            .switchIfEmpty(Single.just(Status.DISCONNECTED));
    }

    @Override
    public Single<IngestStarted> startIngest(String integrationId, String ingestJobId, List<String> apiIds) {
        var command = new StartIngestCommand(ingestJobId, apiIds);

        log.debug("Start ingestion for [integrationId={}]", integrationId);
        return sendStartIngestCommand(command, integrationId)
            .flatMap(reply ->
                reply.getCommandStatus() != CommandStatus.SUCCEEDED
                    ? Single.error(new IntegrationIngestionException(reply.getErrorDetails()))
                    : Single.just(new IngestStarted(ingestJobId, reply.getPayload().total()))
            )
            .doOnSuccess(started ->
                log.debug("APIs ingestion for [integrationId={}] has started [total={}]", integrationId, started.total())
            );
    }

    @Override
    public Single<IntegrationSubscription> subscribe(
        String integrationId,
        FederatedApi api,
        SubscriptionParameter subscriptionParameter,
        String subscriptionId,
        BaseApplicationEntity application,
        Map<String, String> providerMetadata
    ) {
        Map<String, String> metadata = api.getServer() != null ? new HashMap<>(api.getServer()) : new HashMap<>();
        metadata.putAll(providerMetadata);
        SubscriptionType type;
        if (subscriptionParameter instanceof SubscriptionParameter.ApiKey apiKeyParams) {
            type = SubscriptionType.API_KEY;
            metadata.put(Subscription.METADATA_PLAN_ID, apiKeyParams.plan().getProviderId());
        } else if (subscriptionParameter instanceof SubscriptionParameter.OAuth oauthParams) {
            type = SubscriptionType.OAUTH;
            metadata.putAll(
                Map.of(
                    Subscription.METADATA_PLAN_ID,
                    oauthParams.plan().getProviderId(),
                    Subscription.METADATA_CONSUMER_KEY,
                    oauthParams.clientId()
                )
            );
        } else {
            return Single.error(
                new IntegrationIngestionException("Unsupported subscription type: " + subscriptionParameter.plan().getSecurity().getType())
            );
        }
        var payload = new SubscribeCommand.Payload(
            api.getProviderId(),
            Subscription.builder()
                .graviteeSubscriptionId(subscriptionId)
                .graviteeApplicationId(application.getId())
                .graviteeApplicationName(application.getName())
                .type(type)
                .metadata(metadata)
                .build()
        );

        return sendSubscribeCommand(new SubscribeCommand(payload), integrationId).flatMap(reply -> {
            if (reply.getCommandStatus() == CommandStatus.ERROR) {
                return Single.error(new IntegrationSubscriptionException(reply.getErrorDetails()));
            }
            var subscriptionResult = reply.getPayload().subscription();
            return switch (payload.subscription().type()) {
                case API_KEY -> Single.just(apiKey(integrationId, subscriptionResult.apiKey(), subscriptionResult.metadata()));
                case OAUTH -> Single.just(oAuth(integrationId, subscriptionResult.metadata()));
                default -> Single.error(
                    new IntegrationSubscriptionException("Unsupported subscription type: " + payload.subscription().type())
                );
            };
        });
    }

    @Override
    public Completable unsubscribe(String integrationId, FederatedApi api, SubscriptionEntity subscription) {
        var metadata = new HashMap<String, String>();
        if (api.getServer() != null) {
            metadata.putAll(api.getServer());
        }
        if (subscription.getClientId() != null) {
            metadata.put(Subscription.METADATA_CONSUMER_KEY, subscription.getClientId());
        }
        // let subscription metadata override previous metadata
        if (subscription.getMetadata() != null) {
            metadata.putAll(subscription.getMetadata());
        }
        var payload = new UnsubscribeCommand.Payload(
            api.getProviderId(),
            Subscription.builder().graviteeSubscriptionId(subscription.getId()).metadata(metadata).build()
        );

        return sendUnsubscribeCommand(new UnsubscribeCommand(payload), integrationId).flatMapCompletable(reply ->
            reply.getCommandStatus() == CommandStatus.ERROR
                ? Completable.error(new IntegrationSubscriptionException(reply.getErrorDetails()))
                : Completable.complete()
        );
    }

    @Override
    public Single<DiscoveredApis> discoverApis(String integrationId) {
        var command = new DiscoverCommand();

        log.debug("Discover all assets for [integrationId={}]", integrationId);
        return sendDiscoverCommand(command, integrationId).flatMap(discoverReply -> {
            if (discoverReply.getCommandStatus() == CommandStatus.ERROR) {
                return Single.error(new IntegrationDiscoveryException(discoverReply.getErrorDetails()));
            }
            boolean isPartialDiscovery = discoverReply.getPayload().isPartialDiscovery();

            List<IntegrationApi> integrationApis = discoverReply
                .getPayload()
                .apis()
                .stream()
                .map(api -> IntegrationAdapter.INSTANCE.map(api, integrationId))
                .toList();

            log.debug(
                "Discovered APIs for [integrationId={}] total: [{}], partial discovery: [{}]",
                integrationId,
                integrationApis.size(),
                isPartialDiscovery
            );
            return Single.just(new DiscoveredApis(integrationApis, isPartialDiscovery));
        });
    }

    private Single<StartIngestReply> sendStartIngestCommand(StartIngestCommand startIngestCommand, String integrationId) {
        return exchangeController
            .map(controller ->
                controller
                    .sendCommand(startIngestCommand, integrationId)
                    .cast(StartIngestReply.class)
                    .onErrorReturn(throwable -> new StartIngestReply(startIngestCommand.getId(), throwable.getMessage()))
            )
            .orElse(Single.error(new TechnicalDomainException("Federation feature not enabled")));
    }

    private Single<SubscribeReply> sendSubscribeCommand(SubscribeCommand subscribeCommand, String integrationId) {
        return exchangeController
            .map(controller ->
                controller
                    .sendCommand(subscribeCommand, integrationId)
                    .cast(SubscribeReply.class)
                    .onErrorReturn(throwable -> new SubscribeReply(subscribeCommand.getId(), throwable.getMessage()))
            )
            .orElse(Single.error(new TechnicalDomainException("Federation feature not enabled")));
    }

    private Single<UnsubscribeReply> sendUnsubscribeCommand(UnsubscribeCommand command, String integrationId) {
        return exchangeController
            .map(controller ->
                controller
                    .sendCommand(command, integrationId)
                    .cast(UnsubscribeReply.class)
                    .onErrorReturn(throwable -> new UnsubscribeReply(command.getId(), throwable.getMessage()))
            )
            .orElse(Single.error(new TechnicalDomainException("Federation feature not enabled")));
    }

    private Single<DiscoverReply> sendDiscoverCommand(DiscoverCommand command, String integrationId) {
        return exchangeController
            .map(controller ->
                controller
                    .sendCommand(command, integrationId)
                    .cast(DiscoverReply.class)
                    .onErrorReturn(throwable -> new DiscoverReply(command.getId(), throwable.getMessage()))
            )
            .orElse(Single.error(new TechnicalDomainException("Federation feature not enabled")));
    }
}
