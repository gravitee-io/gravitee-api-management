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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalRuleEventSubscriberTest {

    private static final String RULE = """
        {"id":"rule-1","environmentId":"env-1","toolPattern":"issue_refund","priority":100,
         "createdAt":"2026-11-01T10:00:00Z","updatedAt":"2026-11-02T10:00:00Z",
         "argumentConditions":[{"path":"amount","operator":"GT","value":"1000"}]}
        """;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private License license;

    private final MemoryAppender logs = new MemoryAppender();
    private ApprovalRuleManagerImpl manager;
    private ApprovalRuleEventSubscriber subscriber;

    @BeforeEach
    void setUp() {
        lenient().when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
        lenient().when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(true);
        manager = new ApprovalRuleManagerImpl(new ApprovalFeature(licenseManager, true));
        subscriber = new ApprovalRuleEventSubscriber(manager, new ObjectMapper());
        logs.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logs.start();
        subscriberLogger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        subscriberLogger().detachAppender(logs);
        logs.stop();
    }

    @Test
    void should_deploy_a_readable_rule_of_the_event_organization_on_commit() {
        subscriber.onUpsert(event("rule-1", "env-1", RULE));
        assertThat(manager.hasRules("env-1")).isFalse();

        subscriber.onCommit();

        assertThat(manager.candidates("env-1", null, "issue_refund", null))
            .singleElement()
            .satisfies(rule -> {
                assertThat(rule.id()).isEqualTo("rule-1");
                assertThat(rule.organizationId()).isEqualTo("org-1");
                assertThat(rule.priority()).isEqualTo(100);
                assertThat(rule.createdAt()).isEqualTo(Instant.parse("2026-11-01T10:00:00Z").toEpochMilli());
                assertThat(rule.updatedAt()).isEqualTo(Instant.parse("2026-11-02T10:00:00Z").toEpochMilli());
                assertThat(rule.argumentConditions())
                    .singleElement()
                    .satisfies(condition -> assertThat(condition.operator()).isEqualTo("GT"));
            });
        assertThat(manager.receivesRules("org-1", "env-1")).isTrue();
    }

    @Test
    void should_remove_a_rule_on_commit_when_its_event_is_removed() {
        subscriber.onUpsert(event("rule-1", "env-1", RULE));
        subscriber.onCommit();

        subscriber.onRemove(event("rule-1", "env-1", null));
        subscriber.onCommit();

        assertThat(manager.hasRules("env-1")).isFalse();
    }

    @Test
    void should_ignore_the_properties_a_newer_control_plane_adds() {
        subscriber.onUpsert(event("rule-1", "env-1", RULE.replace("\"priority\":100", "\"priority\":100,\"addedLater\":{\"x\":1}")));
        subscriber.onCommit();

        assertThat(manager.hasRules("env-1")).isTrue();
    }

    @Test
    void should_deploy_the_valid_tool_bindings_and_drop_the_invalid_ones() {
        subscriber.onUpsert(
            event(
                "rule-1",
                "env-1",
                RULE.replace(
                    "\"priority\":100",
                    "\"priority\":100,\"toolBindings\":[{\"apiId\":\"api-1\",\"toolName\":\"refund\"},{\"apiId\":\"\",\"toolName\":\"x\"},{\"apiId\":\"api-2\"}]"
                )
            )
        );
        subscriber.onCommit();

        assertThat(manager.candidates("env-1", "api-1", "refund", null))
            .singleElement()
            .satisfies(rule -> assertThat(rule.toolBindings()).containsExactly(new ToolBinding("api-1", "refund")));
        assertThat(manager.receivesRules("org-1", "env-1")).isTrue();
    }

    @Test
    void should_deploy_a_rule_anchored_only_on_catalog_tools() {
        subscriber.onUpsert(
            event("rule-1", "env-1", RULE.replace("\"toolPattern\":\"issue_refund\"", "\"toolEntityIds\":[\"payments.refund\",\"\"]"))
        );
        subscriber.onCommit();

        assertThat(manager.candidates("env-1", null, "any", "payments.refund"))
            .extracting(DeployedApprovalRule::id)
            .containsExactly("rule-1");
    }

    @Test
    void should_refuse_the_environment_when_the_payload_is_not_json() {
        subscriber.onUpsert(event("rule-1", "env-1", "{not json"));
        subscriber.onCommit();

        assertThat(manager.receivesRules("org-1", "env-1")).isFalse();
        assertThat(manager.receivesRules("org-1", "env-2")).isTrue();
    }

    @Test
    void should_refuse_the_environment_when_the_event_has_no_payload() {
        subscriber.onUpsert(event("rule-1", "env-1", null));
        subscriber.onCommit();

        assertThat(manager.receivesRules("org-1", "env-1")).isFalse();
    }

    @Test
    void should_refuse_the_environment_when_a_required_field_is_missing_or_invalid() {
        for (String broken : new String[] {
            RULE.replace("\"priority\":100,", ""),
            RULE.replace("\"createdAt\":\"2026-11-01T10:00:00Z\"", "\"createdAt\":\"yesterday\""),
            RULE.replace("\"updatedAt\":\"2026-11-02T10:00:00Z\"", "\"updatedAt\":\"\""),
            RULE.replace("\"toolPattern\":\"issue_refund\"", "\"toolPattern\":\" \""),
        }) {
            manager = new ApprovalRuleManagerImpl(new ApprovalFeature(licenseManager, true));
            subscriber = new ApprovalRuleEventSubscriber(manager, new ObjectMapper());

            subscriber.onUpsert(event("rule-1", "env-1", broken));
            subscriber.onCommit();

            assertThat(manager.receivesRules("org-1", "env-1")).as(broken).isFalse();
            assertThat(manager.hasRules("env-1")).as(broken).isFalse();
        }
    }

    @Test
    void should_refuse_the_environment_when_an_argument_condition_is_null() {
        subscriber.onUpsert(
            event("rule-1", "env-1", RULE.replace("[{\"path\":\"amount\",\"operator\":\"GT\",\"value\":\"1000\"}]", "[null]"))
        );
        subscriber.onCommit();

        assertThat(manager.receivesRules("org-1", "env-1")).isFalse();
        assertThat(manager.hasRules("env-1")).isFalse();
    }

    @Test
    void should_refuse_the_environment_when_an_argument_condition_has_no_path_or_no_operator() {
        for (String broken : new String[] {
            RULE.replace("\"path\":\"amount\",", ""),
            RULE.replace("\"path\":\"amount\"", "\"path\":\" \""),
            RULE.replace("\"operator\":\"GT\",", ""),
        }) {
            manager = new ApprovalRuleManagerImpl(new ApprovalFeature(licenseManager, true));
            subscriber = new ApprovalRuleEventSubscriber(manager, new ObjectMapper());

            subscriber.onUpsert(event("rule-1", "env-1", broken));
            subscriber.onCommit();

            assertThat(manager.receivesRules("org-1", "env-1")).as(broken).isFalse();
            assertThat(manager.hasRules("env-1")).as(broken).isFalse();
        }
    }

    @Test
    void should_log_which_rule_could_not_be_read_and_why() {
        subscriber.onUpsert(
            event(
                "rule-1",
                "env-1",
                RULE.replace(
                    "{\"path\":\"amount\",\"operator\":\"GT\",\"value\":\"1000\"}",
                    "{\"path\":\"amount\",\"operator\":\"GT\",\"value\":\"1000\"},null"
                )
            )
        );

        assertThat(logs.getLoggedEvents())
            .singleElement()
            .satisfies(logged -> {
                assertThat(logged.getLevel()).isEqualTo(Level.ERROR);
                assertThat(logged.getFormattedMessage()).contains("[rule-1]", "[env-1]", "argumentConditions");
            });
    }

    /** The values of a rule's conditions are the customer's data: an unreadable rule is logged by its ids and the fields at fault only. */
    @Test
    void should_never_log_the_values_of_an_unreadable_payload() {
        subscriber.onUpsert(event("rule-1", "env-1", RULE.replace("\"priority\":100", "\"priority\":\"4111-1111\"")));
        subscriber.onUpsert(event("rule-1", "env-1", RULE.replace("\"value\":\"1000\"}]", "\"value\":\"4111-1111\"},null]")));

        assertThat(logs.getLoggedEvents())
            .hasSize(2)
            .allSatisfy(logged -> assertThat(loggedText(logged)).doesNotContain("4111"));
    }

    @Test
    void should_refuse_the_environment_when_the_payload_names_another_rule_or_environment_than_the_event() {
        subscriber.onUpsert(event("rule-9", "env-1", RULE));
        subscriber.onUpsert(event("rule-1", "env-2", RULE));
        subscriber.onCommit();

        assertThat(manager.receivesRules("org-1", "env-1")).isFalse();
        assertThat(manager.receivesRules("org-1", "env-2")).isFalse();
        assertThat(manager.hasRules("env-1")).isFalse();
        assertThat(manager.hasRules("env-2")).isFalse();
    }

    @Test
    void should_serve_the_environment_again_when_a_readable_copy_arrives() {
        subscriber.onUpsert(event("rule-1", "env-1", "{not json"));
        subscriber.onCommit();

        subscriber.onUpsert(event("rule-1", "env-1", RULE));
        subscriber.onCommit();

        assertThat(manager.receivesRules("org-1", "env-1")).isTrue();
        assertThat(manager.hasRules("env-1")).isTrue();
    }

    @Test
    void should_serve_the_environment_again_when_the_unreadable_rule_is_removed() {
        subscriber.onUpsert(event("rule-1", "env-1", "{not json"));
        subscriber.onCommit();

        subscriber.onRemove(event("rule-1", "env-1", null));
        subscriber.onCommit();

        assertThat(manager.receivesRules("org-1", "env-1")).isTrue();
    }

    private static Logger subscriberLogger() {
        return (Logger) LoggerFactory.getLogger(ApprovalRuleEventSubscriber.class);
    }

    /** The message of a log event and of every exception it carries. */
    private static String loggedText(ILoggingEvent logged) {
        List<String> text = new ArrayList<>(List.of(logged.getFormattedMessage()));
        for (IThrowableProxy thrown = logged.getThrowableProxy(); thrown != null; thrown = thrown.getCause()) {
            text.add(thrown.getMessage());
        }
        return String.join("\n", text);
    }

    private static GenericEvent event(String id, String environmentId, String payload) {
        return new GenericEvent("approval.rule", id, environmentId, "org-1", null, Instant.parse("2026-11-02T10:00:00Z"), payload);
    }
}
