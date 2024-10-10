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
package io.gravitee.rest.api.services.subscriptionpreexpirationnotif;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.common.service.AbstractService;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.key.ApiKeyQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class ScheduledSubscriptionPreExpirationNotificationService extends AbstractService implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(ScheduledSubscriptionPreExpirationNotificationService.class);
    // For debugging purposes you can change the trigger to "0 */1 * * * *" and the cronPeriodInMs to 60 * 1000
    private final String cronTrigger = "0 0 */1 * * *";
    private final int cronPeriodInMs = 60 * 60 * 1000;
    private final AtomicLong counter = new AtomicLong(0);

    @Autowired
    private ApiSearchService apiSearchService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PlanSearchService planSearchService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("subscriptionPreExpirationTaskScheduler")
    private TaskScheduler scheduler;

    @Value("#{'${services.subscription.pre-expiration-notification-schedule:90,45,30}'.split(',')}")
    private List<Integer> configPreExpirationNotificationSchedule;

    @Value("${services.subscription.enabled:true}")
    private boolean enabled;

    private List<Integer> notificationDays;

    @Override
    protected String name() {
        return "Subscription Pre Expiration Notification service";
    }

    @Override
    protected void doStart() {
        if (enabled) {
            notificationDays = getCleanedNotificationDays(configPreExpirationNotificationSchedule);

            logger.info("Subscription Pre Expiration Notification service has been initialized with cron [{}]", cronTrigger);
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } else {
            logger.warn("Subscription Pre Expiration Notification service has been disabled");
        }
    }

    @Override
    public void run() {
        logger.debug("Subscription Pre Expiration Notification #{} started at {}", counter.incrementAndGet(), Instant.now().toString());

        Instant now = Instant.now();

        notificationDays.forEach(daysToExpiration -> {
            Set<String> notifiedSubscriptionIds = notifySubscriptionsExpirations(now, daysToExpiration);
            notifyApiKeysExpirations(now, daysToExpiration, notifiedSubscriptionIds);
        });

        logger.debug("Subscription Pre Expiration Notification #{} ended at {}", counter.get(), Instant.now().toString());
    }

    private void notifyApiKeysExpirations(Instant now, Integer daysToExpiration, Set<String> notifiedSubscriptionIds) {
        Collection<ApiKeyEntity> apiKeyExpirationsToNotify = findApiKeyExpirationsToNotify(now, daysToExpiration);
        apiKeyExpirationsToNotify
            .stream()
            // Remove the ones for which an email has already been sent (could happen in case of restart or concurrent processing with multiple instance of APIM)
            .filter(apiKey ->
                apiKey.getDaysToExpirationOnLastNotification() == null || apiKey.getDaysToExpirationOnLastNotification() > daysToExpiration
            )
            .forEach(apiKey -> notifyApiKeyExpiration(daysToExpiration, apiKey, notifiedSubscriptionIds));
    }

    private void notifyApiKeyExpiration(Integer daysToExpiration, ApiKeyEntity apiKey, Set<String> notifiedSubscriptionIds) {
        ApplicationEntity application = apiKey.getApplication();

        apiKey
            .getSubscriptions()
            .stream()
            .filter(subscription -> !notifiedSubscriptionIds.contains(subscription.getId()))
            .forEach(subscription -> {
                GenericApiEntity api = apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), subscription.getApi());
                GenericPlanEntity plan = planSearchService.findById(GraviteeContext.getExecutionContext(), subscription.getPlan());

                findEmailsToNotify(subscription, application)
                    .forEach(email -> this.sendEmail(email, daysToExpiration, api, plan, application, apiKey));
            });

        apiKeyService.updateDaysToExpirationOnLastNotification(GraviteeContext.getExecutionContext(), apiKey, daysToExpiration);
    }

    private Set<String> notifySubscriptionsExpirations(Instant now, Integer daysToExpiration) {
        Collection<SubscriptionEntity> subscriptionExpirationsToNotify = findSubscriptionExpirationsToNotify(now, daysToExpiration);

        findSubscriptionExpirationsToNotify(now, daysToExpiration)
            .stream()
            .filter(subscription -> // Remove the ones for which an email has already been sent (could happen in case of restart or concurrent processing with multiple instance of APIM)
                subscription.getDaysToExpirationOnLastNotification() == null ||
                subscription.getDaysToExpirationOnLastNotification() > daysToExpiration
            )
            .forEach(subscription -> notifySubscriptionExpiration(daysToExpiration, subscription));

        return subscriptionExpirationsToNotify.stream().map(SubscriptionEntity::getId).collect(Collectors.toSet());
    }

    private void notifySubscriptionExpiration(Integer daysToExpiration, SubscriptionEntity subscription) {
        GenericApiEntity api = apiSearchService.findById(GraviteeContext.getExecutionContext(), subscription.getApi());
        GenericPlanEntity plan = planSearchService.findById(GraviteeContext.getExecutionContext(), subscription.getPlan());

        ApplicationEntity application = applicationService.findById(GraviteeContext.getExecutionContext(), subscription.getApplication());

        findEmailsToNotify(subscription, application)
            .forEach(email -> this.sendEmail(email, daysToExpiration, api, plan, application, null));

        subscriptionService.updateDaysToExpirationOnLastNotification(subscription.getId(), daysToExpiration);
    }

    @VisibleForTesting
    List<Integer> getCleanedNotificationDays(List<Integer> inputDays) {
        int min = 1;
        int max = 366;

        Predicate<Integer> isDayValid = day -> min <= day && day <= max;

        List<Integer> invalidValues = inputDays.stream().filter(day -> !isDayValid.test(day)).collect(Collectors.toList());

        if (!invalidValues.isEmpty()) {
            logger.warn(
                "The configuration key `services.subscription.pre-pollInterval-notification-schedule` contains some invalid values: {}. Values should be between {} and {} (days).",
                invalidValues.stream().map(Object::toString).collect(Collectors.joining(", ")),
                min,
                max
            );
        }

        return inputDays.stream().filter(isDayValid).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    @VisibleForTesting
    Collection<SubscriptionEntity> findSubscriptionExpirationsToNotify(Instant now, Integer daysToExpiration) {
        long expirationStartingTime = now.plus(Duration.ofDays((long) daysToExpiration)).getEpochSecond() * 1000;

        SubscriptionQuery query = new SubscriptionQuery();
        query.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        query.setEndingAtAfter(expirationStartingTime);
        query.setEndingAtBefore(expirationStartingTime + cronPeriodInMs);

        return subscriptionService.search(GraviteeContext.getExecutionContext(), query);
    }

    @VisibleForTesting
    Collection<ApiKeyEntity> findApiKeyExpirationsToNotify(Instant now, Integer daysToExpiration) {
        long expirationStartingTime = now.plus(Duration.ofDays((long) daysToExpiration)).getEpochSecond() * 1000;

        ApiKeyQuery query = new ApiKeyQuery();
        query.setIncludeRevoked(false);
        query.setIncludeFederated(true);
        query.setExpireAfter(expirationStartingTime);
        query.setExpireBefore(expirationStartingTime + cronPeriodInMs);

        return apiKeyService.search(GraviteeContext.getExecutionContext(), query);
    }

    @VisibleForTesting
    Set<String> findEmailsToNotify(SubscriptionEntity subscription, ApplicationEntity application) {
        Set<String> emails = new HashSet<>();
        emails.add(userService.findById(GraviteeContext.getExecutionContext(), subscription.getSubscribedBy()).getEmail());
        emails.add(application.getPrimaryOwner().getEmail());

        // Email can be null, in that case we can't send a notification so just remove it
        return emails.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @VisibleForTesting
    void sendEmail(
        String subscriberEmail,
        int day,
        GenericApiEntity api,
        GenericPlanEntity plan,
        ApplicationEntity application,
        ApiKeyEntity apiKey
    ) {
        EmailNotification emailNotification = new EmailNotificationBuilder()
            .to(subscriberEmail)
            .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_SUBSCRIPTION_PRE_EXPIRATION)
            .param(NotificationParamsBuilder.PARAM_EXPIRATION_DELAY, day)
            .param(NotificationParamsBuilder.PARAM_PLAN, plan)
            .param(NotificationParamsBuilder.PARAM_API, api)
            .param(NotificationParamsBuilder.PARAM_APPLICATION, application)
            .param(NotificationParamsBuilder.PARAM_API_KEY, apiKey)
            .build();

        emailService.sendAsyncEmailNotification(GraviteeContext.getExecutionContext(), emailNotification);
    }
}
