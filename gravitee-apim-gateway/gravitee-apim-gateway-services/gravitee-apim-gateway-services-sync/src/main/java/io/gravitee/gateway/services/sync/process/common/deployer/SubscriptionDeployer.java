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
package io.gravitee.gateway.services.sync.process.common.deployer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.command.SubscriptionFailureCommand;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.services.sync.process.common.model.SubscriptionDeployable;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.node.api.Node;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class SubscriptionDeployer implements Deployer<SubscriptionDeployable> {

    private final SubscriptionService subscriptionService;
    private final SubscriptionDispatcher subscriptionDispatcher;
    private final CommandRepository commandRepository;
    private final Node node;
    private final ObjectMapper objectMapper;
    private final DistributedSyncService distributedSyncService;

    private final Map<String, List<Subscription>> dispatchableSubscription = new ConcurrentHashMap<>();

    @Override
    public Completable deploy(final SubscriptionDeployable deployable) {
        return Completable.fromRunnable(() -> {
            if (deployable.subscriptions() != null) {
                deployable
                    .subscriptions()
                    .stream()
                    .filter(subscription -> deployable.subscribablePlans().contains(subscription.getPlan()))
                    .forEach(subscription -> {
                        try {
                            if (Subscription.Type.PUSH == subscription.getType()) {
                                dispatchableSubscription.compute(
                                    subscription.getApi(),
                                    (apiId, subscriptions) -> {
                                        if (subscriptions == null) {
                                            subscriptions = new ArrayList<>();
                                        }
                                        subscriptions.add(subscription);
                                        return subscriptions;
                                    }
                                );
                            }
                            subscriptionService.register(subscription);
                            log.debug("Subscription [{}] deployed for api [{}] ", subscription.getId(), subscription.getApi());
                        } catch (Exception e) {
                            log.warn("An error occurred when trying to deploy subscription [{}].", subscription.getId(), e);
                        }
                    });
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final SubscriptionDeployable deployable) {
        return Completable.defer(() -> {
            // Dispatch subscription
            List<Subscription> subscriptions = dispatchableSubscription.remove(deployable.apiId());
            if (subscriptions != null) {
                subscriptions.forEach(s -> this.dispatchSubscription(s).subscribe());
            }
            return distributeIfNeeded(deployable);
        });
    }

    @Override
    public Completable undeploy(final SubscriptionDeployable deployable) {
        return Completable.defer(() -> {
            if (deployable instanceof ApiReactorDeployable) {
                return undeployForApi((ApiReactorDeployable) deployable);
            } else if (deployable instanceof SingleSubscriptionDeployable) {
                return undeploySingleSubscription((SingleSubscriptionDeployable) deployable);
            }
            return Completable.complete();
        });
    }

    @Override
    public Completable doAfterUndeployment(final SubscriptionDeployable deployable) {
        return distributeIfNeeded(deployable);
    }

    private Completable distributeIfNeeded(final SubscriptionDeployable deployable) {
        return Completable.defer(() -> {
            if (deployable instanceof SingleSubscriptionDeployable) {
                SingleSubscriptionDeployable singleSubscriptionDeployable = (SingleSubscriptionDeployable) deployable;
                return distributedSyncService.distributeIfNeeded(singleSubscriptionDeployable);
            }
            return Completable.complete();
        });
    }

    private Completable undeployForApi(final ApiReactorDeployable apiReactorDeployable) {
        try {
            subscriptionService.unregisterByApiId(apiReactorDeployable.apiId());
            log.debug("Subscriptions undeployed for api [{}] ", apiReactorDeployable.apiId());
        } catch (Exception e) {
            log.warn("An error occurred when trying to undeploy subscriptions from api [{}].", apiReactorDeployable.apiId(), e);
        }
        return Completable.complete();
    }

    private Completable undeploySingleSubscription(final SingleSubscriptionDeployable subscriptionDeployable) {
        try {
            Subscription subscription = subscriptionDeployable.subscription();
            subscriptionService.unregister(subscription);
            log.debug("Subscription [{}] undeployed for api [{}] ", subscriptionDeployable.id(), subscriptionDeployable.apiId());

            if (Subscription.Type.PUSH == subscription.getType()) {
                return dispatchSubscription(subscription);
            }
        } catch (Exception e) {
            log.warn("An error occurred when trying to undeploy subscriptions [{}].", subscriptionDeployable.id(), e);
        }
        return Completable.complete();
    }

    private Completable dispatchSubscription(final Subscription subscription) {
        return subscriptionDispatcher
            .dispatch(subscription)
            .doOnComplete(() -> log.debug("Subscription [{}] has been dispatched", subscription.getId()))
            .onErrorResumeNext(t -> {
                log.error("Subscription [{}] failed", subscription.getId(), t);
                return sendFailureCommand(subscription, t).onErrorComplete();
            });
    }

    private Completable sendFailureCommand(Subscription subscription, Throwable throwable) {
        return Completable
            .fromRunnable(() -> {
                final Command command = new Command();
                Instant now = Instant.now();
                command.setId(UUID.random().toString());
                command.setFrom(node.id());
                command.setTo(MessageRecipient.MANAGEMENT_APIS.name());
                command.setTags(List.of(CommandTags.SUBSCRIPTION_FAILURE.name()));
                command.setCreatedAt(Date.from(now));
                command.setUpdatedAt(Date.from(now));
                command.setEnvironmentId(subscription.getEnvironmentId());

                convertSubscriptionCommand(subscription, command, throwable.getMessage());

                saveCommand(subscription, command);
            })
            .subscribeOn(Schedulers.io());
    }

    private void convertSubscriptionCommand(Subscription subscription, Command command, String errorMessage) {
        try {
            command.setContent(objectMapper.writeValueAsString(new SubscriptionFailureCommand(subscription.getId(), errorMessage)));
        } catch (JsonProcessingException e) {
            log.error("Failed to convert subscription command [{}] to string", subscription.getId(), e);
            JsonObject json = new JsonObject();
            json.put("subscriptionId", subscription.getId()).put("failureCause", errorMessage);
            command.setContent(json.encode());
        }
    }

    private void saveCommand(Subscription subscription, Command command) {
        try {
            commandRepository.create(command);
        } catch (TechnicalException e) {
            log.error("Failed to create subscription command [{}]", subscription.getId(), e);
        }
    }
}
