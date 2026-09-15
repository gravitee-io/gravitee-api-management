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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PerformanceTargetFixtures;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.notification.model.PerformanceTargetNotificationTemplateData;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition.Kind;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.infra.notification.internal.TemplateDataFetcher;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.service.EmailRecipientsService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.notification.PerformanceTargetHook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import io.gravitee.rest.api.service.notifiers.WebNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PerformanceTargetNotificationDispatcherTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final Environment ENVIRONMENT = Environment.builder()
        .id(PerformanceTargetFixtures.ENVIRONMENT_ID)
        .organizationId(ORGANIZATION_ID)
        .hrids(List.of("dev"))
        .build();
    private static final ExecutionContext CONTEXT = new ExecutionContext(ORGANIZATION_ID, PerformanceTargetFixtures.ENVIRONMENT_ID);

    PortalNotificationConfigRepository portalConfigs = mock(PortalNotificationConfigRepository.class);
    GenericNotificationConfigRepository genericConfigs = mock(GenericNotificationConfigRepository.class);
    MembershipService membershipService = mock(MembershipService.class);
    PortalNotificationService portalNotificationService = mock(PortalNotificationService.class);
    EmailRecipientsService emailRecipientsService = mock(EmailRecipientsService.class);
    EmailNotifierService emailNotifierService = mock(EmailNotifierService.class);
    WebhookNotifierService webhookNotifierService = mock(WebhookNotifierService.class);
    WebNotifierService webNotifierService = mock(WebNotifierService.class);
    NotificationTemplateService notificationTemplateService = mock(NotificationTemplateService.class);
    ParameterService parameterService = mock(ParameterService.class);
    TemplateDataFetcher templateDataFetcher = mock(TemplateDataFetcher.class);
    InstallationAccessQueryService installationAccess = mock(InstallationAccessQueryService.class);
    List<Duration> retryDelays = new ArrayList<>();

    PerformanceTargetNotificationDispatcher dispatcher = new PerformanceTargetNotificationDispatcher(
        portalConfigs,
        genericConfigs,
        membershipService,
        portalNotificationService,
        emailRecipientsService,
        emailNotifierService,
        webhookNotifierService,
        webNotifierService,
        notificationTemplateService,
        parameterService,
        templateDataFetcher,
        new PerformanceTargetNotificationTemplateDataFactory(new AnalyticsDefinitionYAMLQueryService(), installationAccess),
        (retry, delay) -> {
            retryDelays.add(delay);
            retry.run();
        },
        3
    );

    @BeforeEach
    void setUp() throws Exception {
        when(emailRecipientsService.processTemplatedRecipients(anyList(), anyMap())).thenAnswer(invocation ->
            Set.copyOf(invocation.getArgument(0))
        );
        when(portalConfigs.findByReferenceAndHook(anyString(), any(), anyString())).thenReturn(List.of());
        when(genericConfigs.findByReferenceAndHook(anyString(), any(), anyString())).thenReturn(List.of());
        when(templateDataFetcher.fetchData(anyString(), any())).thenReturn(Map.of("api", "api-data"));
        when(notificationTemplateService.resolveTemplateWithParam(anyString(), anyString(), any())).thenAnswer(invocation ->
            invocation.getArgument(1).toString().endsWith(".TITLE") ? "[my-api] Performance target missed" : "The API is missing a target"
        );
    }

    @Nested
    class ApiSubject {

        private final io.gravitee.apim.core.api.model.Api api = ApiFixtures.anA2AProxyApiV4()
            .toBuilder()
            .id(PerformanceTargetFixtures.A2A_API_ID)
            .name("my-api")
            .environmentId(PerformanceTargetFixtures.ENVIRONMENT_ID)
            .build();

        @Test
        void should_send_one_report_per_kind_of_change_through_the_api_settings() throws Exception {
            var portalConfig = new PortalNotificationConfig();
            portalConfig.setUser("owner");
            portalConfig.setGroups(Set.of("group-1"));
            var member = new MemberEntity();
            member.setId("member-1");
            when(portalConfigs.findByReferenceAndHook("RULE_MISSED", NotificationReferenceType.API, api.getId())).thenReturn(
                List.of(portalConfig)
            );
            when(
                membershipService.getMembersByReferencesAndRole(
                    eq(CONTEXT),
                    eq(MembershipReferenceType.GROUP),
                    eq(List.of("group-1")),
                    any()
                )
            ).thenReturn(Set.of(member));
            var email = genericConfig(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID, "${(api.primaryOwner.email)!''}");
            var webhook = genericConfig(NotifierServiceImpl.DEFAULT_WEBHOOK_NOTIFIER_ID, "https://hooks.example.com/apis");
            when(genericConfigs.findByReferenceAndHook("RULE_MISSED", NotificationReferenceType.API, api.getId())).thenReturn(
                List.of(email, webhook)
            );
            when(emailRecipientsService.processTemplatedRecipients(anyList(), anyMap())).thenReturn(Set.of("owner@example.com"));

            dispatcher.notifyApiSubject(
                ENVIRONMENT,
                api,
                List.of(
                    transition(Kind.RULE_MISSED, "target-1"),
                    transition(Kind.RULE_MISSED, "target-2"),
                    transition(Kind.RULE_RECOVERED, "target-1")
                )
            );

            var params = ArgumentCaptor.forClass(Map.class);
            verify(portalNotificationService).create(
                eq(CONTEXT),
                eq(PerformanceTargetHook.RULE_MISSED),
                eq(List.of("owner", "member-1")),
                params.capture()
            );
            var data = (PerformanceTargetNotificationTemplateData) params.getValue().get(PARAM_PERFORMANCE_TARGET);
            assertThat(data.getSubjectKind()).isEqualTo("A2A proxy");
            assertThat(data.getSubjectName()).isEqualTo("my-api");
            assertThat(data.getRules())
                .extracting(PerformanceTargetNotificationTemplateData.RuleChange::getTargetId)
                .containsExactly("target-1", "target-2");
            assertThat(params.getValue()).containsEntry("api", "api-data");
            verify(emailNotifierService).trigger(
                eq(CONTEXT),
                eq(PerformanceTargetHook.RULE_MISSED),
                anyMap(),
                eq(Set.of("owner@example.com"))
            );
            verify(webhookNotifierService).trigger(eq(PerformanceTargetHook.RULE_MISSED), eq(webhook), anyMap());
            // the recovery has no subscriber on this API: nothing goes out for it
            verify(portalNotificationService, never()).create(any(), eq(PerformanceTargetHook.RULE_RECOVERED), anyList(), any());
            verify(emailNotifierService, never()).trigger(any(), eq(PerformanceTargetHook.RULE_RECOVERED), anyMap(), any());
        }

        @Test
        void should_link_to_the_targets_page_of_the_subject_in_the_gamma_console() throws Exception {
            when(installationAccess.getGammaUrl(ORGANIZATION_ID)).thenReturn("https://gamma.example.com/");
            when(portalConfigs.findByReferenceAndHook(anyString(), any(), anyString())).thenAnswer(invocation -> {
                var config = new PortalNotificationConfig();
                config.setUser("owner");
                return List.of(config);
            });

            dispatcher.notifyApiSubject(ENVIRONMENT, api, List.of(transition(Kind.RULE_MISSED, "target-1")));

            var params = ArgumentCaptor.forClass(Map.class);
            verify(portalNotificationService).create(any(), any(), anyList(), params.capture());
            var data = (PerformanceTargetNotificationTemplateData) params.getValue().get(PARAM_PERFORMANCE_TARGET);
            assertThat(data.getSubjectUrl()).isEqualTo(
                "https://gamma.example.com/environments/dev/aim/agent-runtime/" + api.getId() + "/targets"
            );
        }

        private static GenericNotificationConfig genericConfig(String notifier, String config) {
            var generic = new GenericNotificationConfig();
            generic.setNotifier(notifier);
            generic.setConfig(config);
            return generic;
        }
    }

    @Nested
    class ExplicitRecipients {

        private final Map<String, Object> params = Map.of(PARAM_PERFORMANCE_TARGET, "data");

        @Test
        void should_post_a_report_to_a_plain_webhook_and_the_console_text_to_a_slack_incoming_webhook() {
            var webhook = PerformanceTargetNotificationDispatcher.Recipients.webhook("https://hooks.example.com/agents");
            var slack = PerformanceTargetNotificationDispatcher.Recipients.webhook("https://hooks.slack.com/services/T0/B0/x");

            dispatcher.notify(
                CONTEXT,
                PerformanceTargetHook.RULE_MISSED,
                params,
                new PerformanceTargetNotificationDispatcher.Recipients(
                    List.of("owner"),
                    List.of("owner@example.com"),
                    List.of(webhook, slack)
                )
            );

            verify(portalNotificationService).create(CONTEXT, PerformanceTargetHook.RULE_MISSED, List.of("owner"), params);
            verify(emailNotifierService).trigger(CONTEXT, PerformanceTargetHook.RULE_MISSED, params, Set.of("owner@example.com"));
            // the Slack address is not among them: GenericNotificationConfig compares by id, so assert on the address
            var delivered = ArgumentCaptor.forClass(GenericNotificationConfig.class);
            verify(webhookNotifierService).trigger(eq(PerformanceTargetHook.RULE_MISSED), delivered.capture(), eq(params));
            assertThat(delivered.getValue().getConfig()).isEqualTo("https://hooks.example.com/agents");
            var body = ArgumentCaptor.forClass(String.class);
            verify(webNotifierService).request(
                eq(HttpMethod.POST),
                eq("https://hooks.slack.com/services/T0/B0/x"),
                eq(Map.of("Content-Type", "application/json")),
                body.capture(),
                eq(false)
            );
            assertThat(new JsonObject(body.getValue()).getString("text")).isEqualTo(
                "[my-api] Performance target missed\nThe API is missing a target"
            );
        }

        @Test
        void should_retry_a_failing_webhook_with_a_growing_delay_and_give_up_after_the_configured_attempts() {
            var webhook = PerformanceTargetNotificationDispatcher.Recipients.webhook("https://hooks.example.com/down");
            doThrow(new TechnicalManagementException("connection refused"))
                .when(webhookNotifierService)
                .trigger(any(), eq(webhook), anyMap());

            dispatcher.notify(
                CONTEXT,
                PerformanceTargetHook.RULE_MISSED,
                params,
                new PerformanceTargetNotificationDispatcher.Recipients(List.of(), List.of(), List.of(webhook))
            );

            verify(webhookNotifierService, times(3)).trigger(PerformanceTargetHook.RULE_MISSED, webhook, params);
            assertThat(retryDelays).containsExactly(
                PerformanceTargetNotificationDispatcher.FIRST_RETRY_DELAY,
                PerformanceTargetNotificationDispatcher.FIRST_RETRY_DELAY.multipliedBy(2)
            );
        }

        @Test
        void should_send_nothing_on_a_channel_without_recipients() {
            dispatcher.notify(
                CONTEXT,
                PerformanceTargetHook.RULE_RECOVERED,
                params,
                new PerformanceTargetNotificationDispatcher.Recipients(List.of(), List.of(), List.of())
            );

            verify(portalNotificationService, never()).create(any(), any(), anyList(), any());
            verify(emailNotifierService, never()).trigger(any(), any(), anyMap(), any());
            verify(webhookNotifierService, never()).trigger(any(), any(), anyMap());
            verify(webNotifierService, never()).request(any(), anyString(), anyMap(), anyString(), eq(false));
        }
    }

    private static PerformanceTargetRuleTransition transition(Kind kind, String targetId) {
        var target = PerformanceTargetFixtures.aTarget(targetId);
        var evaluation = PerformanceTargetFixtures.anEvaluation(
            "evaluation-" + targetId,
            targetId,
            PerformanceTargetEvaluation.Status.BREACH
        );
        return new PerformanceTargetRuleTransition(kind, target, target.rules().getFirst(), evaluation.rules().getFirst(), evaluation);
    }
}
