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
import io.gravitee.apim.core.subscription.model.ExpiringSubscription;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.common.service.AbstractService;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.key.ApiKeyQuery;
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
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@CustomLog
public class ScheduledSubscriptionPreExpirationNotificationService extends AbstractService implements Runnable {

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
    private SubscriptionQueryService subscriptionQueryService;

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

            log.info("Subscription Pre Expiration Notification service has been initialized with cron [{}]", cronTrigger);
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } else {
            log.warn("Subscription Pre Expiration Notification service has been disabled");
        }
    }

    @Override
    public void run() {
        log.debug("Subscription Pre Expiration Notification #{} started at {}", counter.incrementAndGet(), Instant.now().toString());

        Instant now = Instant.now();
        Map<Integer, List<ExpiringSubscription>> subscriptionsByBucket = bucketExpiringSubscriptions(now);

        // Iterate buckets in descending day order. Within a single tick this only affects ordering of side-effects,
        // since windows are disjoint (1h cron slot vs day-granularity offsets). Across ticks it keeps the
        // daysToExpirationOnLastNotification idempotency filter monotonic: a later tick covering a smaller D
        // will not overwrite a previous notification with a smaller-than-current value.
        subscriptionsByBucket.forEach((daysToExpiration, subs) -> {
            Set<String> notifiedSubscriptionIds = notifySubscriptionsExpirations(daysToExpiration, subs);
            notifyApiKeysExpirations(now, daysToExpiration, notifiedSubscriptionIds);
        });

        log.debug("Subscription Pre Expiration Notification #{} ended at {}", counter.get(), Instant.now().toString());
    }

    @VisibleForTesting
    Map<Integer, List<ExpiringSubscription>> bucketExpiringSubscriptions(Instant now) {
        List<ExpiringSubscription> expiring = subscriptionQueryService.findExpiringSubscriptions(
            now,
            notificationDays,
            cronPeriodInMs,
            List.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED)
        );
        Map<Integer, List<ExpiringSubscription>> buckets = new LinkedHashMap<>();
        long oneDayMs = Duration.ofDays(1).toMillis();
        for (Integer d : notificationDays) {
            long lo = now.toEpochMilli() + d * oneDayMs;
            long hi = lo + cronPeriodInMs;
            List<ExpiringSubscription> inBucket = expiring
                .stream()
                .filter(s -> {
                    ZonedDateTime ending = s.endingAt();
                    if (ending == null) {
                        return false;
                    }
                    long endingMs = ending.toInstant().toEpochMilli();
                    return endingMs >= lo && endingMs < hi;
                })
                .toList();
            buckets.put(d, inBucket);
        }
        return buckets;
    }

    private void notifyApiKeysExpirations(Instant now, Integer daysToExpiration, Set<String> notifiedSubscriptionIds) {
        Collection<ApiKeyEntity> apiKeyExpirationsToNotify = findApiKeyExpirationsToNotify(now, daysToExpiration);
        apiKeyExpirationsToNotify
            .stream()
            // Remove the ones for which an email has already been sent (could happen in case of restart or concurrent processing with multiple instance of APIM)
            .filter(
                apiKey ->
                    apiKey.getDaysToExpirationOnLastNotification() == null ||
                    apiKey.getDaysToExpirationOnLastNotification() > daysToExpiration
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
                GenericApiEntity api = apiSearchService.findGenericById(
                    GraviteeContext.getExecutionContext(),
                    subscription.getApi(),
                    false,
                    false,
                    false
                );
                GenericPlanEntity plan = planSearchService.findById(GraviteeContext.getExecutionContext(), subscription.getPlan());

                findEmailsToNotify(subscription.getSubscribedBy(), application).forEach(email ->
                    this.sendEmail(email, daysToExpiration, api, plan, application, apiKey)
                );
            });

        apiKeyService.updateDaysToExpirationOnLastNotification(GraviteeContext.getExecutionContext(), apiKey, daysToExpiration);
    }

    private Set<String> notifySubscriptionsExpirations(Integer daysToExpiration, List<ExpiringSubscription> subscriptions) {
        subscriptions
            .stream()
            .filter(
                // Remove the ones for which an email has already been sent (could happen in case of restart or concurrent processing with multiple instance of APIM)
                subscription ->
                    subscription.daysToExpirationOnLastNotification() == null ||
                    subscription.daysToExpirationOnLastNotification() > daysToExpiration
            )
            .forEach(subscription -> notifySubscriptionExpiration(daysToExpiration, subscription));

        return subscriptions.stream().map(ExpiringSubscription::id).collect(Collectors.toSet());
    }

    private void notifySubscriptionExpiration(Integer daysToExpiration, ExpiringSubscription subscription) {
        GenericApiEntity api = apiSearchService.findById(GraviteeContext.getExecutionContext(), subscription.apiId());
        GenericPlanEntity plan = planSearchService.findById(GraviteeContext.getExecutionContext(), subscription.planId());

        ApplicationEntity application = applicationService.findById(GraviteeContext.getExecutionContext(), subscription.applicationId());

        findEmailsToNotify(subscription.subscribedBy(), application).forEach(email ->
            this.sendEmail(email, daysToExpiration, api, plan, application, null)
        );

        subscriptionService.updateDaysToExpirationOnLastNotification(subscription.id(), daysToExpiration);
    }

    @VisibleForTesting
    List<Integer> getCleanedNotificationDays(List<Integer> inputDays) {
        int min = 1;
        int max = 366;

        Predicate<Integer> isDayValid = day -> min <= day && day <= max;

        List<Integer> invalidValues = inputDays
            .stream()
            .filter(day -> !isDayValid.test(day))
            .collect(Collectors.toList());

        if (!invalidValues.isEmpty()) {
            log.warn(
                "The configuration key `services.subscription.pre-expiration-notification-schedule` contains some invalid values: {}. Values should be between {} and {} (days).",
                invalidValues.stream().map(Object::toString).collect(Collectors.joining(", ")),
                min,
                max
            );
        }

        return inputDays.stream().filter(isDayValid).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
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
    Set<String> findEmailsToNotify(String subscribedBy, ApplicationEntity application) {
        Set<String> emails = new HashSet<>();
        emails.add(userService.findById(GraviteeContext.getExecutionContext(), subscribedBy).getEmail());
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
