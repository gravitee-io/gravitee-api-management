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
package io.gravitee.apim.infra.performance_target.notification;

import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_PERFORMANCE_TARGET;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.notification.model.hook.HookContext;
import io.gravitee.apim.core.notification.model.hook.HookContextEntry;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.infra.notification.internal.TemplateDataFetcher;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.EmailRecipientsService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.notification.PerformanceTargetHook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import io.gravitee.rest.api.service.notifiers.WebNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sends one performance target report per channel: a console notification, one email to every recipient, one POST per
 * webhook, one Slack message per incoming webhook. For an API subject the recipients and channels are the API's own
 * notification settings, read the way {@link NotifierServiceImpl} reads them for any other hook; a module owning
 * another kind of subject (an agent) hands its recipients over itself.
 *
 * <p>Recipients are resolved and templates rendered once per subject and event, not per rule: the report lists every
 * rule that changed. Posting to a webhook or a Slack address is retried, with backoff, up to the configured number
 * of attempts; emails and console notifications are handed to APIM's own asynchronous senders and are not.
 */
@Service
@CustomLog
public class PerformanceTargetNotificationDispatcher {

    static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(2);

    private final PortalNotificationConfigRepository portalNotificationConfigRepository;
    private final GenericNotificationConfigRepository genericNotificationConfigRepository;
    private final MembershipService membershipService;
    private final PortalNotificationService portalNotificationService;
    private final EmailRecipientsService emailRecipientsService;
    private final EmailNotifierService emailNotifierService;
    private final WebhookNotifierService webhookNotifierService;
    private final WebNotifierService webNotifierService;
    private final NotificationTemplateService notificationTemplateService;
    private final ParameterService parameterService;
    private final TemplateDataFetcher templateDataFetcher;
    private final PerformanceTargetNotificationTemplateDataFactory templateData;
    private final DeliveryRetrier retrier;
    private final int deliveryAttempts;

    public PerformanceTargetNotificationDispatcher(
        PortalNotificationConfigRepository portalNotificationConfigRepository,
        GenericNotificationConfigRepository genericNotificationConfigRepository,
        MembershipService membershipService,
        PortalNotificationService portalNotificationService,
        EmailRecipientsService emailRecipientsService,
        EmailNotifierService emailNotifierService,
        WebhookNotifierService webhookNotifierService,
        WebNotifierService webNotifierService,
        NotificationTemplateService notificationTemplateService,
        ParameterService parameterService,
        TemplateDataFetcher templateDataFetcher,
        PerformanceTargetNotificationTemplateDataFactory templateData,
        @Value("${services.performance-targets.notifications.delivery-attempts:3}") int deliveryAttempts
    ) {
        this(
            portalNotificationConfigRepository,
            genericNotificationConfigRepository,
            membershipService,
            portalNotificationService,
            emailRecipientsService,
            emailNotifierService,
            webhookNotifierService,
            webNotifierService,
            notificationTemplateService,
            parameterService,
            templateDataFetcher,
            templateData,
            DeliveryRetrier.scheduled(),
            deliveryAttempts
        );
    }

    PerformanceTargetNotificationDispatcher(
        PortalNotificationConfigRepository portalNotificationConfigRepository,
        GenericNotificationConfigRepository genericNotificationConfigRepository,
        MembershipService membershipService,
        PortalNotificationService portalNotificationService,
        EmailRecipientsService emailRecipientsService,
        EmailNotifierService emailNotifierService,
        WebhookNotifierService webhookNotifierService,
        WebNotifierService webNotifierService,
        NotificationTemplateService notificationTemplateService,
        ParameterService parameterService,
        TemplateDataFetcher templateDataFetcher,
        PerformanceTargetNotificationTemplateDataFactory templateData,
        DeliveryRetrier retrier,
        int deliveryAttempts
    ) {
        this.portalNotificationConfigRepository = portalNotificationConfigRepository;
        this.genericNotificationConfigRepository = genericNotificationConfigRepository;
        this.membershipService = membershipService;
        this.portalNotificationService = portalNotificationService;
        this.emailRecipientsService = emailRecipientsService;
        this.emailNotifierService = emailNotifierService;
        this.webhookNotifierService = webhookNotifierService;
        this.webNotifierService = webNotifierService;
        this.notificationTemplateService = notificationTemplateService;
        this.parameterService = parameterService;
        this.templateDataFetcher = templateDataFetcher;
        this.templateData = templateData;
        this.retrier = retrier;
        this.deliveryAttempts = Math.max(1, deliveryAttempts);
    }

    /**
     * Tells the API's notification recipients about every rule of the API's targets that changed in one run: one
     * report per kind of change, through the channels the API's settings subscribe to that hook.
     */
    public void notifyApiSubject(Environment environment, Api api, List<PerformanceTargetRuleTransition> transitions) {
        var executionContext = new ExecutionContext(environment.getOrganizationId(), environment.getId());
        var subject = new PerformanceTargetNotificationTemplateDataFactory.Subject(
            "API",
            api.getName(),
            api.getId(),
            templateData.apiTargetsUrl(environment, api.getId())
        );
        byKind(transitions).forEach((kind, changes) -> {
            var hook = PerformanceTargetHook.of(kind);
            var params = new HashMap<>(templateDataFetcher.fetchData(environment.getOrganizationId(), apiContext(hook, api.getId())));
            params.put(PARAM_PERFORMANCE_TARGET, templateData.build(subject, changes));
            notifyApi(executionContext, api.getId(), hook, params);
        });
    }

    /** One report to the API's recipients for {@code hook}, through the channels its notification settings hold. */
    public void notifyApi(ExecutionContext executionContext, String apiId, PerformanceTargetHook hook, Map<String, Object> params) {
        try {
            var portalConfigs = portalNotificationConfigRepository.findByReferenceAndHook(
                hook.name(),
                NotificationReferenceType.API,
                apiId
            );
            var genericConfigs = genericNotificationConfigRepository.findByReferenceAndHook(
                hook.name(),
                NotificationReferenceType.API,
                apiId
            );
            var recipients = new Recipients(
                portalConfigs
                    .stream()
                    .flatMap(config -> consoleUsers(executionContext, config))
                    .distinct()
                    .toList(),
                genericConfigs
                    .stream()
                    .filter(config -> NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID.equals(config.getNotifier()))
                    .map(GenericNotificationConfig::getConfig)
                    .toList(),
                genericConfigs
                    .stream()
                    .filter(config -> NotifierServiceImpl.DEFAULT_WEBHOOK_NOTIFIER_ID.equals(config.getNotifier()))
                    .toList(),
                List.of()
            );
            notify(executionContext, hook, params, recipients);
        } catch (TechnicalException e) {
            log.error("Performance target notification [{}] of API [{}] could not read the API's notification settings", hook, apiId, e);
        }
    }

    /**
     * One report through each channel with someone to reach. Email addresses may be templates over {@code params},
     * as the API notification settings allow ({@code ${(api.primaryOwner.email)!''}}).
     */
    public void notify(ExecutionContext executionContext, PerformanceTargetHook hook, Map<String, Object> params, Recipients recipients) {
        if (!recipients.consoleUserIds().isEmpty()) {
            portalNotificationService.create(executionContext, hook, recipients.consoleUserIds(), params);
        }
        if (!recipients.emails().isEmpty()) {
            var addresses = emailRecipientsService.processTemplatedRecipients(recipients.emails(), params);
            var allowed = parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM)
                ? emailRecipientsService.filterRegisteredUser(executionContext, addresses)
                : addresses;
            if (!allowed.isEmpty()) {
                emailNotifierService.trigger(executionContext, hook, params, allowed);
            }
        }
        recipients
            .webhooks()
            .forEach(config -> deliver("webhook " + config.getConfig(), 1, () -> webhookNotifierService.trigger(hook, config, params)));
        if (!recipients.slackWebhookUrls().isEmpty()) {
            var body = slackBody(executionContext.getOrganizationId(), hook, params);
            recipients.slackWebhookUrls().forEach(url -> deliver("Slack " + url, 1, () -> postSlack(url, body)));
        }
    }

    /**
     * Runs a delivery, and on failure schedules it again after a delay that doubles each time, until it succeeds or
     * the attempts are spent. The failure is logged once per attempt, at warn while attempts remain.
     */
    private void deliver(String description, int attempt, Runnable delivery) {
        try {
            delivery.run();
        } catch (RuntimeException e) {
            if (attempt >= deliveryAttempts) {
                log.error("Performance target notification to {} failed {} time(s), giving up", description, attempt, e);
                return;
            }
            var delay = FIRST_RETRY_DELAY.multipliedBy(1L << (attempt - 1));
            log.warn(
                "Performance target notification to {} failed (attempt {} of {}), retrying in {} s: {}",
                description,
                attempt,
                deliveryAttempts,
                delay.toSeconds(),
                e.getMessage()
            );
            retrier.schedule(() -> deliver(description, attempt + 1, delivery), delay);
        }
    }

    /** A Slack incoming webhook takes {@code {"text": ...}}: the console notification's title and message. */
    private String slackBody(String organizationId, Hook hook, Map<String, Object> params) {
        var title = notificationTemplateService.resolveTemplateWithParam(organizationId, hook.getTemplate() + ".PORTAL.TITLE", params);
        var message = notificationTemplateService.resolveTemplateWithParam(organizationId, hook.getTemplate() + ".PORTAL", params);
        return new JsonObject().put("text", (title + "\n" + message).strip()).encode();
    }

    private void postSlack(String url, String body) {
        webNotifierService.request(HttpMethod.POST, url, Map.of("Content-Type", "application/json"), body, false);
    }

    private java.util.stream.Stream<String> consoleUsers(ExecutionContext executionContext, PortalNotificationConfig config) {
        var userIds = new ArrayList<String>();
        if (config.getUser() != null) {
            userIds.add(config.getUser());
        }
        if (config.getGroups() != null && !config.getGroups().isEmpty()) {
            membershipService
                .getMembersByReferencesAndRole(executionContext, MembershipReferenceType.GROUP, List.copyOf(config.getGroups()), null)
                .stream()
                .map(MemberEntity::getId)
                .forEach(userIds::add);
        }
        return userIds.stream();
    }

    private static Map<PerformanceTargetRuleTransition.Kind, List<PerformanceTargetRuleTransition>> byKind(
        List<PerformanceTargetRuleTransition> transitions
    ) {
        return transitions
            .stream()
            .collect(Collectors.groupingBy(PerformanceTargetRuleTransition::kind, LinkedHashMap::new, Collectors.toList()));
    }

    private static HookContext apiContext(Hook hook, String apiId) {
        return new HookContext() {
            @Override
            public Map<HookContextEntry, String> getProperties() {
                return Map.of(HookContextEntry.API_ID, apiId);
            }

            @Override
            public Hook getHook() {
                return hook;
            }
        };
    }

    @PreDestroy
    void shutdown() {
        retrier.shutdown();
    }

    /**
     * Who a report goes to on each channel. The webhooks are notification configs, as the API settings store them
     * (address plus proxy flag); a module builds one per address with {@link #webhook(String)}.
     *
     * @param emails addresses, or templates resolving to addresses over the notification params
     */
    public record Recipients(
        List<String> consoleUserIds,
        List<String> emails,
        List<GenericNotificationConfig> webhooks,
        List<String> slackWebhookUrls
    ) {
        public Recipients {
            consoleUserIds = consoleUserIds == null ? List.of() : List.copyOf(consoleUserIds);
            emails = emails == null ? List.of() : List.copyOf(emails);
            webhooks = webhooks == null ? List.of() : List.copyOf(webhooks);
            slackWebhookUrls = slackWebhookUrls == null ? List.of() : List.copyOf(slackWebhookUrls);
        }

        public static GenericNotificationConfig webhook(String url) {
            var config = new GenericNotificationConfig();
            config.setNotifier(NotifierServiceImpl.DEFAULT_WEBHOOK_NOTIFIER_ID);
            config.setConfig(url);
            return config;
        }
    }

    /** Where a failed delivery is tried again: a scheduler in production, the calling thread in tests. */
    interface DeliveryRetrier {
        void schedule(Runnable retry, Duration delay);

        default void shutdown() {}

        static DeliveryRetrier scheduled() {
            return new DeliveryRetrier() {
                private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                    var thread = new Thread(runnable, "performance-targets-notification-retries");
                    thread.setDaemon(true);
                    return thread;
                });

                @Override
                public void schedule(Runnable retry, Duration delay) {
                    scheduler.schedule(retry, delay.toMillis(), TimeUnit.MILLISECONDS);
                }

                @Override
                public void shutdown() {
                    scheduler.shutdownNow();
                }
            };
        }
    }
}
