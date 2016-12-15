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
package io.gravitee.management.service.impl;

import com.google.common.collect.ImmutableMap;
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SubscriptionServiceImpl extends AbstractService implements SubscriptionService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private static final String SUBSCRIPTION_SYSTEM_VALIDATOR = "system";

    @Autowired
    private PlanService planService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private EmailService emailService;

    @Override
    public SubscriptionEntity findById(String subscription) {
        try {
            logger.debug("Find subscription by id : {}", subscription);

            Optional<Subscription> optSubscription = subscriptionRepository.findById(subscription);

            if (! optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(subscription);
            }

            return convert(optSubscription.get());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find a subscription using its ID: {}", subscription, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to find a subscription using its ID: %s", subscription), ex);
        }
    }

    @Override
    public Set<SubscriptionEntity> findByApplicationAndPlan(String application, String plan) {
        try {
            logger.debug("Find subscriptions by application {} and plan {}", application, plan);

            Set<Subscription> subscriptions = null;

            if (application != null && ! application.trim().isEmpty()) {
                subscriptions = subscriptionRepository.findByApplication(application);
            } else if (isAuthenticated()){
                Set<ApplicationEntity> applications = applicationService.findByUser(getAuthenticatedUsername());
                subscriptions = applications.stream().flatMap(new Function<ApplicationEntity, Stream<Subscription>>() {
                    @Override
                    public Stream<Subscription> apply(ApplicationEntity applicationEntity) {
                        try {
                            return subscriptionRepository.findByApplication(applicationEntity.getId()).stream();
                        } catch (TechnicalException e) {
                            e.printStackTrace();
                        }
                        return Stream.empty();
                    }
                }).collect(Collectors.toSet());
            }

            if (subscriptions != null) {
                return subscriptions
                        .stream()
                        .filter(subscription -> plan == null || plan.equals(subscription.getPlan()))
                        .map(this::convert)
                        .collect(Collectors.toSet());
            }

            return Collections.emptySet();
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find subscriptions by application: {}", application, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to find subscriptions by application: %s", application), ex);
        }
    }

    @Override
    public Set<SubscriptionEntity> findByApi(String api) {
        logger.debug("Find subscriptions by api {}", api);

        Set<PlanEntity> plans = planService.findByApi(api);
        Set<Subscription> subscriptions = plans.stream().flatMap(plan -> {
            try {
                return subscriptionRepository.findByPlan(plan.getId()).stream();
            } catch (TechnicalException te) {
                logger.error("An error occurs while searching for subscriptions by plan: {}", plan.getId(), te);
            }
            return Stream.empty();
        }).collect(Collectors.toSet());

        if (subscriptions != null) {
            return subscriptions
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    @Override
    public Set<SubscriptionEntity> findByPlan(String plan) {
        try {
            logger.debug("Find subscriptions by plan : {}", plan);
            Set<Subscription> subscriptions = subscriptionRepository.findByPlan(plan);
            return subscriptions.stream().map(this::convert).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find subscriptions by plan: {}", plan, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to find subscriptions by plan: %s", plan), ex);
        }
    }

    @Override
    public SubscriptionEntity create(String plan, String application) {
        try {
            logger.debug("Create a new subscription for plan {} and application {}", plan, application);

            PlanEntity planEntity = planService.findById(plan);

            if (planEntity.getStatus() == PlanStatus.CLOSED) {
                throw new PlanAlreadyClosedException(plan);
            }

            if (planEntity.getStatus() == PlanStatus.STAGING) {
                throw new PlanNotYetPublishedException(plan);
            }

            if (planEntity.getSecurity() == PlanSecurityType.KEY_LESS) {
                throw new PlanNotSubscribableException(plan);
            }

            ApplicationEntity applicationEntity = applicationService.findById(application);

            long subscriptionCount = subscriptionRepository.findByApplication(application)
                    .stream()
                    .filter(subscription ->
                            subscription.getPlan().equals(plan) &&
                                    subscription.getStatus() != Subscription.Status.REJECTED)
                    .count();

            if (subscriptionCount > 0) {
                throw new PlanAlreadySubscribedException(plan);
            }

            Subscription subscription = new Subscription();
            subscription.setPlan(plan);
            subscription.setId(UUID.toString(UUID.random()));
            subscription.setApplication(application);
            subscription.setCreatedAt(new Date());
            subscription.setUpdatedAt(subscription.getCreatedAt());
            subscription.setStatus(Subscription.Status.PENDING);
            subscription.setSubscribedBy(getAuthenticatedUser().getUsername());

            subscription = subscriptionRepository.create(subscription);

            final ApiEntity api = apiService.findById(planEntity.getApis().iterator().next());
            final PrimaryOwnerEntity apiOwner = api.getPrimaryOwner();
            final PrimaryOwnerEntity appOwner = applicationEntity.getPrimaryOwner();

            // Send a notification to the primary owner of the API
            if (apiOwner != null && apiOwner.getEmail() != null && !apiOwner.getEmail().isEmpty()) {
                emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                        .to(apiOwner.getEmail())
                        .subject("New subscription for " + api.getName() + " with plan " + planEntity.getName())
                        .template(EmailNotificationBuilder.EmailTemplate.NEW_SUBSCRIPTION)
                        .params(ImmutableMap.of(
                                "owner", appOwner,
                                "api", api,
                                "plan", plan,
                                "application", application))
                        .build());
            }

            if (PlanValidationType.AUTO == planEntity.getValidation()) {
                ProcessSubscriptionEntity process = new ProcessSubscriptionEntity();
                process.setId(subscription.getId());
                process.setAccepted(true);
                process.setStartingAt(new Date());

                // Do process
                return process(process, SUBSCRIPTION_SYSTEM_VALIDATOR);
            } else {
                return convert(subscription);
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to subscribe to the plan {}", plan, ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to subscribe to the plan %s", plan), ex);
        }
    }

    @Override
    public SubscriptionEntity update(UpdateSubscriptionEntity updateSubscription) {
        try {
            logger.debug("Update subscription {}", updateSubscription.getId());

            Optional<Subscription> optSubscription = subscriptionRepository.findById(updateSubscription.getId());
            if (! optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(updateSubscription.getId());
            }

            Subscription subscription = optSubscription.get();

            if (subscription.getStatus() == Subscription.Status.ACCEPTED) {
                subscription.setUpdatedAt(new Date());
                subscription.setStartingAt(updateSubscription.getStartingAt());
                subscription.setEndingAt(updateSubscription.getEndingAt());

                subscription = subscriptionRepository.update(subscription);

                // Update the expiration date for not yet revoked api-keys relative to this subscription
                Date endingAt = subscription.getEndingAt();
                if (endingAt != null) {

                    Set<ApiKeyEntity> apiKeys = apiKeyService.findBySubscription(subscription.getId());
                    Date now = new Date();
                    for (ApiKeyEntity apiKey : apiKeys) {
                        Date expireAt = apiKey.getExpireAt();
                        if (!apiKey.isRevoked() && (expireAt == null || expireAt.equals(now) || expireAt.before(now))) {
                            apiKey.setExpireAt(endingAt);
                            apiKeyService.update(apiKey);
                        }
                    }
                }

                return convert(subscription);
            }

            throw new SubscriptionNotUpdatableException(updateSubscription.getId());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to update subscription {}", updateSubscription.getId(), ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to update subscription %s", updateSubscription.getId()), ex);
        }
    }

    @Override
    public SubscriptionEntity process(ProcessSubscriptionEntity processSubscription, String validator) {
        try {
            logger.debug("Subscription {} processed by {}", processSubscription.getId(), validator);

            Optional<Subscription> optSubscription = subscriptionRepository.findById(processSubscription.getId());
            if (! optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(processSubscription.getId());
            }

            Subscription subscription = optSubscription.get();
            if (subscription.getStatus() != Subscription.Status.PENDING) {
                throw new SubscriptionAlreadyProcessedException(subscription.getId());
            }

            PlanEntity planEntity = planService.findById(subscription.getPlan());

            if (planEntity.getStatus() == PlanStatus.CLOSED) {
                throw new PlanAlreadyClosedException(planEntity.getId());
            }

            subscription.setProcessedBy(validator);
            subscription.setProcessedAt(new Date());

            if (processSubscription.isAccepted()) {
                subscription.setStatus(Subscription.Status.ACCEPTED);
                subscription.setStartingAt((processSubscription.getStartingAt() != null) ?
                        processSubscription.getStartingAt() : new Date());
                subscription.setEndingAt(processSubscription.getEndingAt());
            } else {
                subscription.setStatus(Subscription.Status.REJECTED);
                subscription.setReason(processSubscription.getReason());
            }

            subscription = subscriptionRepository.update(subscription);

            final ApplicationEntity application = applicationService.findById(subscription.getApplication());
            final PlanEntity plan = planService.findById(subscription.getPlan());
            final ApiEntity api = apiService.findById(plan.getApis().iterator().next());
            final PrimaryOwnerEntity owner = application.getPrimaryOwner();

            if (owner != null && owner.getEmail() != null && !owner.getEmail().isEmpty()) {
                if (subscription.getStatus() == Subscription.Status.ACCEPTED) {
                    emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                            .to(owner.getEmail())
                            .subject("Your subscription to " + api.getName() + " with plan " + plan.getName() +
                                    " has been approved")
                            .template(EmailNotificationBuilder.EmailTemplate.APPROVE_SUBSCRIPTION)
                            .params(ImmutableMap.of(
                                    "owner", owner,
                                    "api", api,
                                    "plan", plan,
                                    "application", application))
                            .build());
                } else {
                    emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                            .to(owner.getEmail())
                            .subject("Your subscription to " + api.getName() + " with plan " + plan.getName() +
                                    " has been rejected")
                            .template(EmailNotificationBuilder.EmailTemplate.REJECT_SUBSCRIPTION)
                            .params(ImmutableMap.of(
                                    "owner", owner,
                                    "api", api,
                                    "plan", plan,
                                    "application", application))
                            .build());
                }
            }

            if (subscription.getStatus() == Subscription.Status.ACCEPTED) {
                apiKeyService.generate(subscription.getId());
            }

            return convert(subscription);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to process subscription {} by {}",
                    processSubscription.getId(), validator, ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to process subscription %s by %s",
                    processSubscription.getId(), validator), ex);
        }
    }

    @Override
    public SubscriptionEntity close(String subscriptionId) {
        try {
            logger.debug("Close subscription {}", subscriptionId);

            Optional<Subscription> optSubscription = subscriptionRepository.findById(subscriptionId);
            if (! optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(subscriptionId);
            }

            Subscription subscription = optSubscription.get();

            if (subscription.getStatus() == Subscription.Status.ACCEPTED) {
                subscription.setUpdatedAt(new Date());
                subscription.setStatus(Subscription.Status.CLOSED);

                subscription = subscriptionRepository.update(subscription);

                // API Keys are automatically revoked
                Date endingAt = subscription.getEndingAt();
                if (endingAt != null) {

                    Set<ApiKeyEntity> apiKeys = apiKeyService.findBySubscription(subscription.getId());
                    Date now = new Date();
                    for (ApiKeyEntity apiKey : apiKeys) {
                        Date expireAt = apiKey.getExpireAt();
                        if (!apiKey.isRevoked() && (expireAt == null || expireAt.equals(now) || expireAt.before(now))) {
                            apiKey.setExpireAt(endingAt);
                            apiKey.setRevokedAt(endingAt);
                            apiKey.setRevoked(true);
                            apiKeyService.update(apiKey);
                        }
                    }
                }

                return convert(subscription);
            }

            throw new SubscriptionNotClosableException(subscriptionId);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to close subscription {}", subscriptionId, ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to close subscription %s", subscriptionId), ex);
        }
    }

    @Override
    public void delete(String subscriptionId) {
        try {
            logger.debug("Delete subscription {}", subscriptionId);

            Optional<Subscription> optSubscription = subscriptionRepository.findById(subscriptionId);
            if (! optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(subscriptionId);
            }

            // Delete API Keys
            apiKeyService.findBySubscription(subscriptionId)
                    .forEach(apiKey -> apiKeyService.delete(apiKey.getKey()));

            // Delete subscription
            subscriptionRepository.delete(subscriptionId);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete subscription: {}", subscriptionId, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to delete subscription: %s", subscriptionId), ex);
        }
    }

    private SubscriptionEntity convert(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();

        entity.setId(subscription.getId());
        entity.setPlan(subscription.getPlan());
        entity.setProcessedAt(subscription.getProcessedAt());
        entity.setStatus(SubscriptionStatus.valueOf(subscription.getStatus().name()));
        entity.setProcessedBy(subscription.getProcessedBy());
        entity.setReason(subscription.getReason());
        entity.setApplication(subscription.getApplication());
        entity.setStartingAt(subscription.getStartingAt());
        entity.setEndingAt(subscription.getEndingAt());
        entity.setCreatedAt(subscription.getCreatedAt());
        entity.setUpdatedAt(subscription.getUpdatedAt());
        entity.setSubscribedBy(subscription.getSubscribedBy());

        return entity;
    }
}
