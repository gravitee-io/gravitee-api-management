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

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PerformanceTargetFixtures;
import inmemory.EnvironmentCrudServiceInMemory;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition.Kind;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetSubjectTransitions;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetTransitionEvent;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerformanceTargetTransitionPublisherImplTest {

    private static final String ORGANIZATION_ID = "organization-id";

    EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    EventManager eventManager = new EventManagerImpl();
    List<Event<PerformanceTargetTransitionEvent, PerformanceTargetSubjectTransitions>> events = new ArrayList<>();

    PerformanceTargetTransitionPublisherImpl publisher = new PerformanceTargetTransitionPublisherImpl(
        environmentCrudService,
        eventManager,
        Runnable::run
    );

    @BeforeEach
    void setUp() {
        environmentCrudService.initWith(
            List.of(Environment.builder().id(PerformanceTargetFixtures.ENVIRONMENT_ID).organizationId(ORGANIZATION_ID).build())
        );
        EventListener<PerformanceTargetTransitionEvent, PerformanceTargetSubjectTransitions> listener = events::add;
        eventManager.subscribeForEvents(listener, PerformanceTargetTransitionEvent.class);
    }

    @Test
    void should_publish_one_event_per_subject_with_every_rule_of_the_subject_that_changed() {
        var apiTarget = PerformanceTargetFixtures.aTarget("api-target");
        var otherApiTarget = PerformanceTargetFixtures.aTarget("api-target-2");
        var agentTarget = PerformanceTargetFixtures.aTarget("agent-target")
            .toBuilder()
            .subject(new PerformanceTarget.Subject(List.of("llm-api"), "agent-42"))
            .build();

        publisher.publish(
            List.of(
                transition(Kind.RULE_MISSED, apiTarget),
                transition(Kind.RULE_MISSED, agentTarget),
                transition(Kind.RULE_RECOVERED, otherApiTarget)
            )
        );

        assertThat(events)
            .extracting(Event::type, event -> event.content().reference(), event -> event.content().transitions().size())
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    PerformanceTargetTransitionEvent.RULES_CHANGED,
                    PerformanceTargetFixtures.A2A_API_ID,
                    2
                ),
                org.assertj.core.groups.Tuple.tuple(PerformanceTargetTransitionEvent.RULES_CHANGED, "agent-42", 1)
            );
        assertThat(events.getFirst().content())
            .extracting(PerformanceTargetSubjectTransitions::organizationId, PerformanceTargetSubjectTransitions::environmentId)
            .containsExactly(ORGANIZATION_ID, PerformanceTargetFixtures.ENVIRONMENT_ID);
        assertThat(events.getFirst().content().of(Kind.RULE_RECOVERED))
            .extracting(t -> t.target().id())
            .containsExactly("api-target-2");
    }

    @Test
    void should_still_publish_the_other_subjects_when_one_cannot_be_resolved() {
        var unknownEnvironment = PerformanceTargetFixtures.aTarget("lost").toBuilder().environmentId("unknown-environment").build();

        publisher.publish(
            List.of(transition(Kind.RULE_MISSED, unknownEnvironment), transition(Kind.RULE_MISSED, PerformanceTargetFixtures.aTarget()))
        );

        assertThat(events)
            .singleElement()
            .extracting(event -> event.content().reference())
            .isEqualTo(PerformanceTargetFixtures.A2A_API_ID);
    }

    @Test
    void should_publish_nothing_for_an_empty_run() {
        publisher.publish(List.of());

        assertThat(events).isEmpty();
    }

    private static PerformanceTargetRuleTransition transition(Kind kind, PerformanceTarget target) {
        var evaluation = PerformanceTargetFixtures.anEvaluation(
            "evaluation-" + target.id(),
            target.id(),
            PerformanceTargetEvaluation.Status.BREACH
        )
            .toBuilder()
            .environmentId(target.environmentId())
            .reference(target.subject().reference())
            .build();
        return new PerformanceTargetRuleTransition(kind, target, target.rules().getFirst(), evaluation.rules().getFirst(), evaluation);
    }
}
