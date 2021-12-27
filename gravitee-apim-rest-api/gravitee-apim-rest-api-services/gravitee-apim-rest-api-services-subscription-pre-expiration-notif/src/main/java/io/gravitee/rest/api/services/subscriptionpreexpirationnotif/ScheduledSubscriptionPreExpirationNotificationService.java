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
package io.gravitee.rest.api.services.subscriptionpreexpirationnotif;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.common.service.AbstractService;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.key.ApiKeyQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class ScheduledSubscriptionPreExpirationNotificationService extends AbstractService implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(ScheduledSubscriptionPreExpirationNotificationService.class);

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PlanService planService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskScheduler scheduler;

    @Value("#{'${services.subscription.pre-expiration-notification-schedule:90,45,30}'.split(',')}")
    private List<Integer> configPreExpirationNotificationSchedule;

    @Value("${services.subscription.enabled:true}")
    private boolean enabled;

    // For debugging purposes you can change the trigger to "0 */1 * * * *" and the cronPeriodInMs to 60 * 1000
    private final String cronTrigger = "0 0 */1 * * *";
    private final int cronPeriodInMs = 60 * 60 * 1000;

    private List<Integer> notificationDays;

    private final AtomicLong counter = new AtomicLong(0);

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

        notificationDays.forEach(
            daysToExpiration -> {
                Collection<SubscriptionEntity> subscriptionExpirationsToNotify = findSubscriptionExpirationsToNotify(now, daysToExpiration);
                subscriptionExpirationsToNotify
                    .stream()
                    .filter(
                        // Remove the ones for which an email has already been sent (could happen in case of restart or concurrent processing with multiple instance of APIM)
                        subscription ->
                            subscription.getDaysToExpirationOnLastNotification() == null ||
                            subscription.getDaysToExpirationOnLastNotification() > daysToExpiration
                    )
                    .forEach(subscription -> notifySubscription(daysToExpiration, subscription));

                List<String> notifiedSubscriptionIds = subscriptionExpirationsToNotify
                    .stream()
                    .map(SubscriptionEntity::getId)
                    .collect(Collectors.toList());

                Collection<ApiKeyEntity> apiKeyExpirationsToNotify = findApiKeyExpirationsToNotify(now, daysToExpiration);
                apiKeyExpirationsToNotify
                    .stream()
                    // Remove the ones for which an email has already been sent (could happen in case of restart or concurrent processing with multiple instance of APIM)
                    .filter(
                        apiKey ->
                            apiKey.getDaysToExpirationOnLastNotification() == null ||
                            apiKey.getDaysToExpirationOnLastNotification() > daysToExpiration
                    )
                    // Remove the ones related to a subscription for which an email was just sent
                    .filter(apiKey -> !notifiedSubscriptionIds.contains(apiKey.getSubscription()))
                    .forEach(apiKey -> notificationApiKeyExpiration(daysToExpiration, apiKey));
            }
        );

        logger.debug("Subscription Pre Expiration Notification #{} ended at {}", counter.get(), Instant.now().toString());
    }

    private ApiKeyEntity notificationApiKeyExpiration(Integer daysToExpiration, ApiKeyEntity apiKey) {
        SubscriptionEntity subscription = subscriptionService.findById(apiKey.getSubscription());
        ApiEntity api = apiService.findById(subscription.getApi());
        PlanEntity plan = planService.findById(subscription.getPlan());
        ApplicationEntity application = applicationService.findById(GraviteeContext.getCurrentEnvironment(), subscription.getApplication());

        findEmailsToNotify(subscription, application)
            .forEach(email -> this.sendEmail(email, daysToExpiration, api, plan, application, apiKey));

        return apiKeyService.updateDaysToExpirationOnLastNotification(apiKey, daysToExpiration);
    }

    private SubscriptionEntity notifySubscription(Integer daysToExpiration, SubscriptionEntity subscription) {
        ApiEntity api = apiService.findById(subscription.getApi());
        PlanEntity plan = planService.findById(subscription.getPlan());

        ApplicationEntity application = applicationService.findById(GraviteeContext.getCurrentEnvironment(), subscription.getApplication());

        findEmailsToNotify(subscription, application)
            .forEach(email -> this.sendEmail(email, daysToExpiration, api, plan, application, null));

        return subscriptionService.updateDaysToExpirationOnLastNotification(subscription.getId(), daysToExpiration);
    }

    @VisibleForTesting
    List<Integer> getCleanedNotificationDays(List<Integer> inputDays) {
        int min = 1;
        int max = 366;

        Predicate<Integer> isDayValid = day -> min <= day && day <= max;

        List<Integer> invalidValues = inputDays.stream().filter(day -> !isDayValid.test(day)).collect(Collectors.toList());

        if (!invalidValues.isEmpty()) {
            logger.warn(
                "The configuration key `services.subscription.pre-expiration-notification-schedule` contains some invalid values: {}. Values should be between {} and {} (days).",
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

        return subscriptionService.search(query);
    }

    @VisibleForTesting
    Collection<ApiKeyEntity> findApiKeyExpirationsToNotify(Instant now, Integer daysToExpiration) {
        long expirationStartingTime = now.plus(Duration.ofDays((long) daysToExpiration)).getEpochSecond() * 1000;

        ApiKeyQuery query = new ApiKeyQuery();
        query.setIncludeRevoked(false);
        query.setExpireAfter(expirationStartingTime);
        query.setExpireBefore(expirationStartingTime + cronPeriodInMs);

        return apiKeyService.search(query);
    }

    @VisibleForTesting
    Set<String> findEmailsToNotify(SubscriptionEntity subscription, ApplicationEntity application) {
        Set<String> emails = new HashSet<>();
        emails.add(userService.findById(subscription.getSubscribedBy()).getEmail());
        emails.add(application.getPrimaryOwner().getEmail());

        // Email can be null, in that case we can't send a notification so just remove it
        return emails.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @VisibleForTesting
    void sendEmail(String subscriberEmail, int day, ApiEntity api, PlanEntity plan, ApplicationEntity application, ApiKeyEntity apiKey) {
        GraviteeContext.ReferenceContext context = new GraviteeContext.ReferenceContext(
            api.getReferenceId(),
            GraviteeContext.ReferenceContextType.ENVIRONMENT
        );

        EmailNotification emailNotification = new EmailNotificationBuilder()
            .to(subscriberEmail)
            .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_SUBSCRIPTION_PRE_EXPIRATION)
            .param(NotificationParamsBuilder.PARAM_EXPIRATION_DELAY, day)
            .param(NotificationParamsBuilder.PARAM_PLAN, plan)
            .param(NotificationParamsBuilder.PARAM_API, api)
            .param(NotificationParamsBuilder.PARAM_APPLICATION, application)
            .param(NotificationParamsBuilder.PARAM_API_KEY, apiKey)
            .build();

        emailService.sendAsyncEmailNotification(emailNotification, context);
    }
}
