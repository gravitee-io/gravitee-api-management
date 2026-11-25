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
package io.gravitee.gateway.genericevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalCaller;
import io.gravitee.gamma.definition.approval.ApprovalEventKinds;
import io.gravitee.gamma.definition.approval.ApprovalOpenKey;
import io.gravitee.gamma.definition.approval.ApprovalRule;
import io.gravitee.gamma.definition.approval.ApprovalState;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalFeature;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalRuleEventSubscriber;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalRuleManagerImpl;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateEventSubscriber;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateManagerImpl;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * What the control plane publishes reaches what the gateway decides with. The payloads are built from the wire model
 * the publisher serializes ({@link ApprovalRule}, {@link ApprovalState}), not from hand-written JSON, and go through the
 * real generic event manager, subscribers and managers: only the license is a double. The kind of mistake it catches is
 * a field the publisher writes and the gateway no longer reads, or a change that reaches one manager and not the other.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalEventsIntegrationTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final String API = "api-1";
    private static final String TOOL = "issue_refund";
    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant RULE_SAVED = NOW.minus(Duration.ofHours(2));
    private static final Instant RULE_EDITED = NOW.minus(Duration.ofHours(1));
    private static final String DIGEST = "digest-1";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private License license;

    private DefaultGenericEventManager events;
    private ApprovalRuleManagerImpl rules;
    private ApprovalStateManagerImpl states;

    @BeforeEach
    void subscribeTheManagersTheWayTheGatewayDoes() {
        lenient().when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
        lenient().when(license.isFeatureEnabled("gamma-aim-hitl")).thenReturn(true);
        events = new DefaultGenericEventManager(clock);
        rules = new ApprovalRuleManagerImpl(new ApprovalFeature(licenseManager, true));
        states = new ApprovalStateManagerImpl(clock);
        events.subscribe(ApprovalEventKinds.RULE, new ApprovalRuleEventSubscriber(rules, mapper));
        events.subscribe(ApprovalEventKinds.STATE, new ApprovalStateEventSubscriber(states, mapper));
    }

    @Test
    void a_rule_saved_in_the_control_plane_holds_the_calls_it_names_once_the_gateway_commits() throws JsonProcessingException {
        events.upsert(ruleEvent(rule(TOOL, RULE_SAVED)));

        assertThat(rules.candidates(ENV, API, TOOL, null)).as("nothing is served before the commit").isEmpty();

        events.commit();

        assertThat(rules.candidates(ENV, API, TOOL, null))
            .extracting(DeployedApprovalRule::id, DeployedApprovalRule::updatedAt)
            .containsExactly(tuple("rule-1", RULE_SAVED.toEpochMilli()));
        assertThat(rules.candidates(ENV, API, "another_tool", null)).isEmpty();
    }

    @Test
    void a_rule_saved_again_replaces_the_copy_the_gateway_holds() throws JsonProcessingException {
        events.upsert(ruleEvent(rule(TOOL, RULE_SAVED)));
        events.commit();

        events.upsert(ruleEvent(rule("issue_credit", RULE_EDITED)));
        events.commit();

        assertThat(rules.candidates(ENV, API, TOOL, null)).as("the old pattern no longer holds").isEmpty();
        assertThat(rules.candidates(ENV, API, "issue_credit", null))
            .extracting(DeployedApprovalRule::updatedAt)
            .containsExactly(RULE_EDITED.toEpochMilli());
    }

    @Test
    void a_rule_deleted_in_the_control_plane_stops_holding_its_calls() throws JsonProcessingException {
        events.upsert(ruleEvent(rule(TOOL, RULE_SAVED)));
        events.commit();
        assertThat(rules.candidates(ENV, API, TOOL, null)).isNotEmpty();

        events.remove(ENV, ApprovalEventKinds.RULE, "rule-1");
        events.commit();

        assertThat(rules.candidates(ENV, API, TOOL, null)).isEmpty();
        assertThat(rules.hasRules(ENV)).isFalse();
    }

    @Test
    void a_rule_the_gateway_cannot_read_refuses_the_environment_until_a_readable_copy_arrives() throws JsonProcessingException {
        ApprovalRule withoutPriority = rule(TOOL, RULE_SAVED);
        withoutPriority.setPriority(null);
        events.upsert(ruleEvent(withoutPriority));
        events.commit();

        assertThat(rules.receivesRules(ORG, ENV)).as("a rule that cannot be read may cover any call").isFalse();

        events.upsert(ruleEvent(rule(TOOL, RULE_EDITED)));
        events.commit();

        assertThat(rules.receivesRules(ORG, ENV)).isTrue();
        assertThat(rules.candidates(ENV, API, TOOL, null)).hasSize(1);
    }

    @Test
    void a_pending_approval_is_found_by_its_open_key_and_wakes_the_call_waiting_for_its_decision() throws JsonProcessingException {
        TestScheduler scheduler = new TestScheduler();
        events.upsert(stateEvent(state("PENDING", 1L, null)));
        events.commit();
        assertThat(states.byOpenKey(openKey()).map(DeployedApprovalState::id)).contains("ar-1");
        TestObserver<Void> waiting = states.awaitChange("ar-1", Duration.ofSeconds(20), scheduler).test();
        waiting.assertNotComplete();

        events.upsert(stateEvent(state("REJECTED", 2L, null)));
        scheduler.triggerActions();

        waiting.assertComplete();
        assertThat(states.byId("ar-1").map(DeployedApprovalState::status)).contains("REJECTED");
    }

    @Test
    void a_time_based_grant_covers_identical_calls_until_it_is_ended() throws JsonProcessingException {
        events.upsert(stateEvent(grant(NOW.plus(Duration.ofMinutes(30)), 1L)));
        events.commit();
        assertThat(grantFor(RULE_SAVED.toEpochMilli()).map(DeployedApprovalState::id)).contains("ar-1");

        events.upsert(stateEvent(grant(NOW, 2L)));
        events.commit();

        assertThat(grantFor(RULE_SAVED.toEpochMilli())).as("End now: the next identical call is held again").isEmpty();
    }

    @Test
    void a_time_based_grant_stops_covering_calls_once_its_rule_is_saved_again() throws JsonProcessingException {
        events.upsert(ruleEvent(rule(TOOL, RULE_SAVED)));
        events.upsert(stateEvent(grant(NOW.plus(Duration.ofMinutes(30)), 1L)));
        events.commit();
        long ruleVersionTheCallIsHeldUnder = rules.candidates(ENV, API, TOOL, null).getFirst().updatedAt();
        assertThat(grantFor(ruleVersionTheCallIsHeldUnder)).isPresent();

        events.upsert(ruleEvent(rule(TOOL, RULE_EDITED)));
        events.commit();
        long editedRuleVersion = rules.candidates(ENV, API, TOOL, null).getFirst().updatedAt();

        assertThat(editedRuleVersion).isNotEqualTo(ruleVersionTheCallIsHeldUnder);
        assertThat(grantFor(editedRuleVersion)).as("the rule changed under the grant").isEmpty();
    }

    private Optional<DeployedApprovalState> grantFor(long ruleVersion) {
        return states.activeGrant(openKey(), ruleVersion);
    }

    private static String openKey() {
        return ApprovalOpenKey.of(ENV, API, "rule-1", "app-1", "alice", DIGEST);
    }

    private static ApprovalRule rule(String toolPattern, Instant updatedAt) {
        return ApprovalRule.builder()
            .id("rule-1")
            .environmentId(ENV)
            .toolPattern(toolPattern)
            .argumentConditions(List.of(new ApprovalRule.ArgumentCondition("amount", "GT", "100")))
            .priority(10)
            .createdAt(RULE_SAVED.toString())
            .updatedAt(updatedAt.toString())
            .build();
    }

    private static ApprovalState state(String status, long version, ApprovalState.Grant grant) {
        return ApprovalState.builder()
            .approvalRequestId("ar-1")
            .environmentId(ENV)
            .apiId(API)
            .ruleId("rule-1")
            .ruleUpdatedAt(RULE_SAVED.toString())
            .openKey(openKey())
            .status(status)
            .createdAt(NOW.minus(Duration.ofMinutes(5)).toString())
            .keepUntil(NOW.plus(Duration.ofMinutes(30)).toString())
            .updatedAt(NOW.plusMillis(version).toString())
            .grant(grant)
            .version(version)
            .build();
    }

    private static ApprovalState grant(Instant coversUntil, long version) {
        return state(
            "APPROVED",
            version,
            ApprovalState.Grant.builder()
                .scope("TIMED")
                .coversUntil(coversUntil.toString())
                .caller(new ApprovalCaller("app-1", "alice"))
                .argumentsDigest(DIGEST)
                .build()
        );
    }

    private GenericEvent ruleEvent(ApprovalRule rule) throws JsonProcessingException {
        return new GenericEvent(
            ApprovalEventKinds.RULE,
            rule.getId(),
            rule.getEnvironmentId(),
            ORG,
            null,
            Instant.parse(rule.getUpdatedAt()),
            mapper.writeValueAsString(rule)
        );
    }

    private GenericEvent stateEvent(ApprovalState state) throws JsonProcessingException {
        return new GenericEvent(
            ApprovalEventKinds.STATE,
            state.getApprovalRequestId(),
            state.getEnvironmentId(),
            ORG,
            Instant.parse(state.getKeepUntil()),
            Instant.parse(state.getUpdatedAt()),
            mapper.writeValueAsString(state)
        );
    }
}
