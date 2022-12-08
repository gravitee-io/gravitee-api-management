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
package io.gravitee.gateway.handlers.api.services;

import static io.gravitee.gateway.jupiter.api.policy.SecurityToken.TokenType.API_KEY;
import static io.gravitee.gateway.jupiter.api.policy.SecurityToken.TokenType.CLIENT_ID;
import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.command.SubscriptionFailureCommand;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.policy.SecurityToken;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.CommandTags;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.MessageRecipient;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionService implements io.gravitee.gateway.api.service.SubscriptionService {

    public static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

    private static final String CACHE_NAME_BY_API_AND_CLIENT_ID = "SUBSCRIPTIONS_BY_API_AND_CLIENT_ID";
    private static final String CACHE_NAME_BY_SUBSCRIPTION_ID = "SUBSCRIPTIONS_BY_ID";
    private static final String CACHE_NAME_DISPATCHABLE_BY_API = "DISPATCHABLE_SUBSCRIPTIONS_BY_API";

    // Caches only contains active subscriptions
    private final Cache<String, Subscription> cacheByApiClientId;
    private final Cache<String, Subscription> cacheBySubscriptionId;
    private final Cache<String, List<Subscription>> dispatchableSubscriptionsByApi;

    private final ApiKeyService apiKeyService;
    private final SubscriptionDispatcher subscriptionDispatcher;

    private final CommandRepository commandRepository;
    private final Node node;

    private ObjectMapper objectMapper;

    public SubscriptionService(
        CacheManager cacheManager,
        ApiKeyService apiKeyService,
        SubscriptionDispatcher subscriptionDispatcher,
        CommandRepository commandRepository,
        Node node,
        ObjectMapper objectMapper
    ) {
        cacheByApiClientId = cacheManager.getOrCreateCache(CACHE_NAME_BY_API_AND_CLIENT_ID);
        cacheBySubscriptionId = cacheManager.getOrCreateCache(CACHE_NAME_BY_SUBSCRIPTION_ID);
        dispatchableSubscriptionsByApi = cacheManager.getOrCreateCache(CACHE_NAME_DISPATCHABLE_BY_API);
        this.apiKeyService = apiKeyService;
        this.subscriptionDispatcher = subscriptionDispatcher;
        this.commandRepository = commandRepository;
        this.node = node;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Subscription> getByApiAndSecurityToken(String api, SecurityToken securityToken, String plan) {
        if (securityToken.getTokenType().equals(API_KEY.name())) {
            return apiKeyService.getByApiAndKey(api, securityToken.getTokenValue()).flatMap(apiKey -> getById(apiKey.getSubscription()));
        }
        if (securityToken.getTokenType().equals(CLIENT_ID.name())) {
            return getByApiAndClientIdAndPlan(api, securityToken.getTokenValue(), plan);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Subscription> getByApiAndClientIdAndPlan(String api, String clientId, String plan) {
        return Optional.ofNullable(cacheByApiClientId.get(buildClientIdCacheKey(api, clientId, plan)));
    }

    @Override
    public Optional<Subscription> getById(String subscriptionId) {
        return Optional.ofNullable(cacheBySubscriptionId.get(subscriptionId));
    }

    @Override
    public void save(Subscription subscription) {
        if (Subscription.Type.STANDARD == subscription.getType()) {
            cacheStandardSubscription(subscription);
        } else if (Subscription.Type.SUBSCRIPTION == subscription.getType()) {
            cacheDispatchableSubscription(subscription);
        }
    }

    @Override
    public void saveOrDispatch(Subscription subscription) {
        if (Subscription.Type.STANDARD == subscription.getType()) {
            cacheStandardSubscription(subscription);
        } else if (Subscription.Type.SUBSCRIPTION == subscription.getType()) {
            dispatch(subscription);
        }
    }

    @Override
    public void dispatchFor(List<String> apis) {
        apis.forEach(
            api -> {
                final List<Subscription> subscriptions = dispatchableSubscriptionsByApi.evict(api);
                if (subscriptions != null) {
                    subscriptions.forEach(this::dispatch);
                }
            }
        );
    }

    private void cacheStandardSubscription(Subscription subscription) {
        if (subscription.getClientId() != null) {
            saveInCacheByApiAndClientId(subscription);
        }
        saveInCacheBySubscriptionId(subscription);
    }

    private void cacheDispatchableSubscription(Subscription subscription) {
        final List<Subscription> subscriptions = dispatchableSubscriptionsByApi.get(subscription.getApi());
        List<Subscription> subs = subscriptions;
        if (subscriptions == null) {
            subs = new ArrayList<>();
        }
        subs.add(subscription);
        dispatchableSubscriptionsByApi.put(subscription.getApi(), subs);
    }

    private void dispatch(Subscription subscription) {
        subscriptionDispatcher
            .dispatch(subscription)
            .doOnComplete(() -> LOGGER.debug("Subscription [{}] has been dispatched", subscription.getId()))
            .onErrorResumeNext(
                t -> {
                    LOGGER.error("Subscription [{}] failed with cause: {}", subscription.getId(), t);
                    return sendFailureCommand(subscription, t);
                }
            )
            .subscribe();
    }

    private Completable sendFailureCommand(Subscription subscription, Throwable throwable) {
        return Completable
            .fromRunnable(
                () -> {
                    final Command command = new Command();
                    Instant now = Instant.now();
                    command.setId(UUID.random().toString());
                    command.setFrom(node.id());
                    command.setTo(MessageRecipient.MANAGEMENT_APIS.name());
                    command.setTags(List.of(CommandTags.SUBSCRIPTION_FAILURE.name()));
                    command.setCreatedAt(Date.from(now));
                    command.setUpdatedAt(Date.from(now));

                    convertSubscriptionCommand(subscription, command, throwable.getMessage());

                    saveCommand(subscription, command);
                }
            )
            .subscribeOn(Schedulers.io());
    }

    private void convertSubscriptionCommand(Subscription subscription, Command command, String errorMessage) {
        try {
            command.setContent(objectMapper.writeValueAsString(new SubscriptionFailureCommand(subscription.getId(), errorMessage)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert subscription command [{}] to string", subscription.getId(), e);
            JsonObject json = new JsonObject();
            json.put("subscriptionId", subscription.getId()).put("failureCause", errorMessage);
            command.setContent(json.encode());
        }
    }

    private void saveCommand(Subscription subscription, Command command) {
        try {
            commandRepository.create(command);
        } catch (TechnicalException e) {
            LOGGER.error("Failed to create subscription command [{}]", subscription.getId(), e);
        }
    }

    private void saveInCacheBySubscriptionId(Subscription subscription) {
        if (ACCEPTED.name().equals(subscription.getStatus())) {
            cacheBySubscriptionId.put(subscription.getId(), subscription);
        } else {
            cacheBySubscriptionId.evict(subscription.getId());
        }
    }

    private void saveInCacheByApiAndClientId(Subscription subscription) {
        Subscription cachedSubscription = cacheBySubscriptionId.get(subscription.getId());
        String key = buildClientIdCacheKey(subscription);

        // Index the subscription without plan id to allow search without plan criteria.
        final String keyWithoutPlan = buildClientIdCacheKey(subscription.getApi(), subscription.getClientId(), null);

        // remove subscription from cache if its client_id changed
        if (
            cachedSubscription != null &&
            cachedSubscription.getClientId() != null &&
            !cachedSubscription.getClientId().equals(subscription.getClientId())
        ) {
            cacheByApiClientId.evict(buildClientIdCacheKey(cachedSubscription));
        }

        // put or remove subscription from cache according to its status
        if (ACCEPTED.name().equals(subscription.getStatus())) {
            cacheByApiClientId.put(key, subscription);
            cacheByApiClientId.put(keyWithoutPlan, subscription);
        } else if (null != cachedSubscription) {
            Subscription existingSubscription = cacheByApiClientId.get(key);
            if (null != existingSubscription && cachedSubscription.getId().equals(existingSubscription.getId())) {
                // The cache only contains active subscriptions.
                // Here we check that the subscription is evicted when the ids match.
                // The key is not unique in the database as for an API, a plan and an application multiple subscriptions can be open and close.
                cacheByApiClientId.evict(key);
                cacheByApiClientId.evict(keyWithoutPlan);
            }
        }
    }

    private String buildClientIdCacheKey(Subscription subscription) {
        return buildClientIdCacheKey(subscription.getApi(), subscription.getClientId(), subscription.getPlan());
    }

    private String buildClientIdCacheKey(String api, String clientId, String plan) {
        return String.format("%s.%s.%s", api, clientId, plan);
    }
}
