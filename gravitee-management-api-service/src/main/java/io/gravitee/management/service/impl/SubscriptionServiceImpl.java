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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.common.Pageable;
import io.gravitee.management.model.pagedresult.Metadata;
import io.gravitee.management.model.subscription.SubscriptionQuery;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.notification.ApiHook;
import io.gravitee.management.service.notification.ApplicationHook;
import io.gravitee.management.service.notification.NotificationParamsBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.API;
import static io.gravitee.repository.management.model.Audit.AuditProperties.APPLICATION;
import static io.gravitee.repository.management.model.Subscription.AuditEvent.*;

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

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotifierService notifierService;

    @Override
    public SubscriptionEntity findById(String subscription) {
        try {
            logger.debug("Find subscription by id : {}", subscription);

            Optional<Subscription> optSubscription = subscriptionRepository.findById(subscription);

            if (!optSubscription.isPresent()) {
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
    public Collection<SubscriptionEntity> findByApplicationAndPlan(String application, String plan) {
        logger.debug("Find subscriptions by application {} and plan {}", application, plan);

        SubscriptionQuery query = new SubscriptionQuery();
        if (plan != null) {
            query.setPlan(plan);
        }

        if (application != null && !application.trim().isEmpty()) {
            query.setApplication(application);
        } else if (isAuthenticated()) {
            Set<ApplicationEntity> applications = applicationService.findByUser(getAuthenticatedUsername());
            query.setApplications(applications.stream().map(ApplicationEntity::getId).collect(Collectors.toList()));
        }

        return search(query);
    }

    @Override
    public Collection<SubscriptionEntity> findByApi(String api) {
        logger.debug("Find subscriptions by api {}", api);

        SubscriptionQuery query = new SubscriptionQuery();
        query.setApi(api);

        return search(query);
    }

    @Override
    public Collection<SubscriptionEntity> findByPlan(String plan) {
        logger.debug("Find subscriptions by plan {}", plan);

        SubscriptionQuery query = new SubscriptionQuery();
        query.setPlan(plan);

        return search(query);
    }

    @Override
    public SubscriptionEntity create(NewSubscriptionEntity newSubscriptionEntity) {
        String plan = newSubscriptionEntity.getPlan();
        String application = newSubscriptionEntity.getApplication();
        try {
            logger.debug("Create a new subscription for plan {} and application {}", plan, application);

            PlanEntity planEntity = planService.findById(plan);

            if (planEntity.getStatus() == PlanStatus.DEPRECATED) {
                throw new PlanNotSubscribableException(plan);
            }

            if (planEntity.getStatus() == PlanStatus.CLOSED) {
                throw new PlanAlreadyClosedException(plan);
            }

            if (planEntity.getStatus() == PlanStatus.STAGING) {
                throw new PlanNotYetPublishedException(plan);
            }

            if (planEntity.getSecurity() == PlanSecurityType.KEY_LESS) {
                throw new PlanNotSubscribableException("A key_less plan is not subscribable !");
            }

            ApplicationEntity applicationEntity = applicationService.findById(application);

            // Check existing subscriptions
            List<Subscription> subscriptions = subscriptionRepository.search(
                    new SubscriptionCriteria.Builder()
                            .applications(Collections.singleton(application))
                            .apis(planEntity.getApis())
                            .build());

            if (! subscriptions.isEmpty()) {
                Predicate<Subscription> onlyValidSubs =
                        subscription ->
                                subscription.getStatus() != Subscription.Status.REJECTED &&
                                subscription.getStatus() != Subscription.Status.CLOSED;

                // First, check that there is no subscription to the same plan
                long subscriptionCount = subscriptions.stream()
                        .filter(onlyValidSubs)
                        .filter(subscription -> subscription.getPlan().equals(plan))
                        .count();

                if (subscriptionCount > 0) {
                    throw new PlanAlreadySubscribedException(plan);
                }

                // Then, if user is subscribing to an oauth2 or jwt plan.
                // Check that there is no existing subscription based on an OAuth2 or JWT plan
                if (planEntity.getSecurity() == PlanSecurityType.OAUTH2 ||
                        planEntity.getSecurity() == PlanSecurityType.JWT) {
                    long count = subscriptions.stream()
                            .filter(onlyValidSubs)
                            .map(Subscription::getPlan)
                            .distinct()
                            .map(plan1 -> planService.findById(plan1))
                            .filter(subPlan ->
                                    subPlan.getSecurity() == PlanSecurityType.OAUTH2
                                            || subPlan.getSecurity() == PlanSecurityType.JWT
                            )
                            .count();

                    if (count > 0) {
                        throw new PlanNotSubscribableException(
                                "An other OAuth2 or JWT plan is already subscribed by the same application.");
                    }
                }
            }

            if (planEntity.getSecurity() == PlanSecurityType.OAUTH2 ||
                    planEntity.getSecurity() == PlanSecurityType.JWT) {
                // Check that the application contains a client_id
                if (applicationEntity.getClientId() == null || applicationEntity.getClientId().trim().isEmpty()) {
                    throw new PlanNotSubscribableException(
                            "A client_id is required to subscribe to an OAuth2 or JWT plan.");
                }
            }

            Subscription subscription = new Subscription();
            subscription.setPlan(plan);
            subscription.setId(UUID.toString(UUID.random()));
            subscription.setApplication(application);
            subscription.setCreatedAt(new Date());
            subscription.setUpdatedAt(subscription.getCreatedAt());
            subscription.setStatus(Subscription.Status.PENDING);
            subscription.setRequest(newSubscriptionEntity.getRequest());
            subscription.setSubscribedBy(getAuthenticatedUser().getUsername());
            subscription.setClientId(applicationEntity.getClientId());
            String apiId = planEntity.getApis().iterator().next();
            subscription.setApi(apiId);
            subscription = subscriptionRepository.create(subscription);

            createAudit(apiId, application, SUBSCRIPTION_CREATED, subscription.getCreatedAt(), null, subscription);

            final ApiModelEntity api = apiService.findByIdForTemplates(apiId);
            final PrimaryOwnerEntity apiOwner = api.getPrimaryOwner();
            //final PrimaryOwnerEntity appOwner = applicationEntity.getPrimaryOwner();


            String portalUrl = environment.getProperty("portalURL");

            String subscriptionsUrl = "";

            if (portalUrl != null) {
                if (portalUrl.endsWith("/")) {
                    portalUrl = portalUrl.substring(0, portalUrl.length() - 1);
                }
                subscriptionsUrl = portalUrl + "/#!/management/apis/" + api.getId() + "/subscriptions/" + subscription.getId();
            }

            final Map<String, Object> params = new NotificationParamsBuilder()
                    .api(api)
                    .plan(planEntity)
                    .application(applicationEntity)
                    .owner(apiOwner)
                    .subscription(convert(subscription))
                    .subscriptionsUrl(subscriptionsUrl)
                    .build();

            if (PlanValidationType.AUTO == planEntity.getValidation()) {
                ProcessSubscriptionEntity process = new ProcessSubscriptionEntity();
                process.setId(subscription.getId());
                process.setAccepted(true);
                process.setStartingAt(new Date());
                // Do process
                return process(process, SUBSCRIPTION_SYSTEM_VALIDATOR);
            } else {
                notifierService.trigger(ApiHook.SUBSCRIPTION_NEW, apiId, params);
                notifierService.trigger(ApplicationHook.SUBSCRIPTION_NEW, application, params);
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
        return update(updateSubscription, null);
    }

    @Override
    public SubscriptionEntity update(UpdateSubscriptionEntity updateSubscription, String clientId) {
        try {
            logger.debug("Update subscription {}", updateSubscription.getId());

            Optional<Subscription> optSubscription = subscriptionRepository.findById(updateSubscription.getId());
            if (!optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(updateSubscription.getId());
            }

            Subscription subscription = optSubscription.get();

            if (subscription.getStatus() == Subscription.Status.ACCEPTED) {
                Subscription previousSubscription = new Subscription(subscription);
                subscription.setUpdatedAt(new Date());
                subscription.setStartingAt(updateSubscription.getStartingAt());
                subscription.setEndingAt(updateSubscription.getEndingAt());

                if (clientId != null) {
                    subscription.setClientId(clientId);
                }

                subscription = subscriptionRepository.update(subscription);
                final PlanEntity plan = planService.findById(subscription.getPlan());
                createAudit(
                        plan.getApis().iterator().next(),
                        subscription.getApplication(),
                        SUBSCRIPTION_UPDATED,
                        subscription.getUpdatedAt(),
                        previousSubscription,
                        subscription);

                // Update the expiration date for not yet revoked api-keys relative to this subscription
                Date endingAt = subscription.getEndingAt();
                if (plan.getSecurity() == PlanSecurityType.API_KEY && endingAt != null) {
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
    public SubscriptionEntity process(ProcessSubscriptionEntity processSubscription, String userId) {
        try {
            logger.debug("Subscription {} processed by {}", processSubscription.getId(), userId);

            Optional<Subscription> optSubscription = subscriptionRepository.findById(processSubscription.getId());
            if (!optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(processSubscription.getId());
            }

            Subscription subscription = optSubscription.get();
            Subscription previousSubscription = new Subscription(subscription);
            if (subscription.getStatus() != Subscription.Status.PENDING) {
                throw new SubscriptionAlreadyProcessedException(subscription.getId());
            }

            PlanEntity planEntity = planService.findById(subscription.getPlan());

            if (planEntity.getStatus() == PlanStatus.CLOSED) {
                throw new PlanAlreadyClosedException(planEntity.getId());
            }

            subscription.setProcessedBy(userId);
            subscription.setProcessedAt(new Date());

            if (processSubscription.isAccepted()) {
                subscription.setStatus(Subscription.Status.ACCEPTED);
                subscription.setStartingAt((processSubscription.getStartingAt() != null) ?
                        processSubscription.getStartingAt() : new Date());
                subscription.setEndingAt(processSubscription.getEndingAt());
                subscription.setReason(processSubscription.getReason());
            } else {
                subscription.setStatus(Subscription.Status.REJECTED);
                subscription.setReason(processSubscription.getReason());
                subscription.setClosedAt(new Date());
            }

            subscription = subscriptionRepository.update(subscription);

            final ApplicationEntity application = applicationService.findById(subscription.getApplication());
            final PlanEntity plan = planService.findById(subscription.getPlan());
            final String apiId = plan.getApis().iterator().next();
            final ApiModelEntity api = apiService.findByIdForTemplates(apiId);
            final PrimaryOwnerEntity owner = application.getPrimaryOwner();
            createAudit(
                    apiId,
                    subscription.getApplication(),
                    SUBSCRIPTION_UPDATED,
                    subscription.getUpdatedAt(),
                    previousSubscription,
                    subscription);

            SubscriptionEntity subscriptionEntity = convert(subscription);

            final Map<String, Object> params = new NotificationParamsBuilder()
                    .owner(owner)
                    .application(application)
                    .api(api)
                    .plan(plan)
                    .subscription(subscriptionEntity)
                    .build();
            if (subscription.getStatus() == Subscription.Status.ACCEPTED) {
                notifierService.trigger(ApiHook.SUBSCRIPTION_ACCEPTED, apiId, params);
                notifierService.trigger(ApplicationHook.SUBSCRIPTION_ACCEPTED, application.getId(), params);
            } else {
                notifierService.trigger(ApiHook.SUBSCRIPTION_REJECTED, apiId, params);
                notifierService.trigger(ApplicationHook.SUBSCRIPTION_REJECTED, application.getId(), params);
            }

            if (plan.getSecurity() == PlanSecurityType.API_KEY &&
                    subscription.getStatus() == Subscription.Status.ACCEPTED) {
                apiKeyService.generate(subscription.getId());
            }

            return subscriptionEntity;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to process subscription {} by {}",
                    processSubscription.getId(), userId, ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to process subscription %s by %s",
                    processSubscription.getId(), userId), ex);
        }
    }

    @Override
    public SubscriptionEntity close(String subscriptionId) {
        try {
            logger.debug("Close subscription {}", subscriptionId);

            Optional<Subscription> optSubscription = subscriptionRepository.findById(subscriptionId);
            if (!optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(subscriptionId);
            }

            Subscription subscription = optSubscription.get();

            if (subscription.getStatus() == Subscription.Status.ACCEPTED) {
                Subscription previousSubscription = new Subscription(subscription);
                final Date now = new Date();
                subscription.setUpdatedAt(now);
                subscription.setStatus(Subscription.Status.CLOSED);

                subscription.setClosedAt(new Date());

                subscription = subscriptionRepository.update(subscription);

                // Send an email to subscriber
                final ApplicationEntity application = applicationService.findById(subscription.getApplication());
                final PlanEntity plan = planService.findById(subscription.getPlan());
                String apiId = plan.getApis().iterator().next();
                final ApiModelEntity api = apiService.findByIdForTemplates(apiId);
                final PrimaryOwnerEntity owner = application.getPrimaryOwner();
                final Map<String, Object> params = new NotificationParamsBuilder()
                        .owner(owner)
                        .api(api)
                        .plan(plan)
                        .application(application)
                        .build();

                notifierService.trigger(ApiHook.SUBSCRIPTION_CLOSED, apiId, params);
                notifierService.trigger(ApplicationHook.SUBSCRIPTION_CLOSED, application.getId(), params);
                createAudit(
                        apiId,
                        subscription.getApplication(),
                        SUBSCRIPTION_CLOSED,
                        subscription.getUpdatedAt(),
                        previousSubscription,
                        subscription);

                // API Keys are automatically revoked
                Set<ApiKeyEntity> apiKeys = apiKeyService.findBySubscription(subscription.getId());
                for (ApiKeyEntity apiKey : apiKeys) {
                    Date expireAt = apiKey.getExpireAt();
                    if (!apiKey.isRevoked() && (expireAt == null || expireAt.equals(now) || expireAt.before(now))) {
                        apiKey.setExpireAt(now);
                        apiKey.setRevokedAt(now);
                        apiKey.setRevoked(true);
                        apiKeyService.revoke(apiKey.getKey(), false);
                    }
                }

                return convert(subscription);
            }

            throw new SubscriptionNotClosableException(subscription);
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
            if (!optSubscription.isPresent()) {
                throw new SubscriptionNotFoundException(subscriptionId);
            }
            Subscription subscription = optSubscription.get();

            // Delete API Keys
            apiKeyService.findBySubscription(subscriptionId)
                    .forEach(apiKey -> apiKeyService.delete(apiKey.getKey()));

            // Delete subscription
            subscriptionRepository.delete(subscriptionId);
            createAudit(
                    planService.findById(subscription.getPlan()).getApis().iterator().next(),
                    subscription.getApplication(),
                    SUBSCRIPTION_DELETED,
                    subscription.getUpdatedAt(),
                    subscription,
                    null);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete subscription: {}", subscriptionId, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to delete subscription: %s", subscriptionId), ex);
        }
    }

    @Override
    public Collection<SubscriptionEntity> search(SubscriptionQuery query) {
        try {
            logger.debug("Search subscriptions {}", query);

            SubscriptionCriteria.Builder builder = new SubscriptionCriteria.Builder()
                    .apis(query.getApis())
                    .applications(query.getApplications())
                    .plans(query.getPlans())
                    .from(query.getFrom())
                    .to(query.getTo());

            if (query.getStatuses() != null) {
                builder.statuses(
                query.getStatuses().stream()
                        .map(subscriptionStatus -> Subscription.Status.valueOf(subscriptionStatus.name()))
                        .collect(Collectors.toSet()));
            }

            List<SubscriptionEntity> subscriptions = subscriptionRepository.search(builder.build())
                    .stream().map(this::convert).collect(Collectors.toList());
            return subscriptions;

        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search for subscriptions: {}", query, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to search for subscriptions: %s", query), ex);
        }
    }

    @Override
    public Page<SubscriptionEntity> search(SubscriptionQuery query, Pageable pageable) {
        try {
            logger.debug("Search pageable subscriptions {}", query);

            SubscriptionCriteria.Builder builder = new SubscriptionCriteria.Builder()
                    .apis(query.getApis())
                    .applications(query.getApplications())
                    .plans(query.getPlans())
                    .from(query.getFrom())
                    .to(query.getTo());

            if (query.getStatuses() != null) {
                builder.statuses(
                        query.getStatuses().stream()
                                .map(subscriptionStatus -> Subscription.Status.valueOf(subscriptionStatus.name()))
                                .collect(Collectors.toSet()));
            }

            Page<Subscription> pageSubscription = subscriptionRepository
                    .search(builder.build(),
                            new PageableBuilder()
                                    .pageNumber(pageable.getPageNumber() - 1)
                                    .pageSize(pageable.getPageSize())
                                    .build());

            List<SubscriptionEntity> content = pageSubscription.getContent()
                    .stream().map(this::convert).collect(Collectors.toList());

            return new Page<>(content, pageSubscription.getPageNumber() + 1,
                    (int) pageSubscription.getPageElements(), pageSubscription.getTotalElements());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search for pageable subscriptions: {}", query, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to search for pageable subscriptions: %s", query), ex);
        }
    }

    public Metadata getMetadata(List<SubscriptionEntity> subscriptions) {
        Metadata metadata = new Metadata();

        subscriptions.forEach( subscription -> {
            if (!metadata.containsKey(subscription.getApplication())) {
                ApplicationEntity applicationEntity = applicationService.findById(subscription.getApplication());
                metadata.put(subscription.getApplication(), "name", applicationEntity.getName());
            }

            if (!metadata.containsKey(subscription.getPlan())) {
                PlanEntity planEntity = planService.findById(subscription.getPlan());
                metadata.put(subscription.getPlan(), "name", planEntity.getName());
            }

            if (!metadata.containsKey(subscription.getApi())) {
                ApiEntity api = apiService.findById(subscription.getApi());
                metadata.put(subscription.getApi(), "name", api.getName());
            }
        });

        return metadata;
    }

    private SubscriptionEntity convert(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();

        entity.setId(subscription.getId());
        entity.setApi(subscription.getApi());
        entity.setPlan(subscription.getPlan());
        entity.setProcessedAt(subscription.getProcessedAt());
        entity.setStatus(SubscriptionStatus.valueOf(subscription.getStatus().name()));
        entity.setProcessedBy(subscription.getProcessedBy());
        entity.setRequest(subscription.getRequest());
        entity.setReason(subscription.getReason());
        entity.setApplication(subscription.getApplication());
        entity.setStartingAt(subscription.getStartingAt());
        entity.setEndingAt(subscription.getEndingAt());
        entity.setCreatedAt(subscription.getCreatedAt());
        entity.setUpdatedAt(subscription.getUpdatedAt());
        entity.setSubscribedBy(subscription.getSubscribedBy());
        entity.setClosedAt(subscription.getClosedAt());
        entity.setClientId(subscription.getClientId());

        return entity;
    }

    private void createAudit(String apiId, String applicationId, Audit.AuditEvent event, Date createdAt,
                             Subscription oldValue, Subscription newValue) {
        auditService.createApiAuditLog(
                apiId,
                Collections.singletonMap(APPLICATION, applicationId),
                event,
                createdAt,
                oldValue,
                newValue);
        auditService.createApplicationAuditLog(
                applicationId,
                Collections.singletonMap(API, apiId),
                event,
                createdAt,
                oldValue,
                newValue);
    }
}
