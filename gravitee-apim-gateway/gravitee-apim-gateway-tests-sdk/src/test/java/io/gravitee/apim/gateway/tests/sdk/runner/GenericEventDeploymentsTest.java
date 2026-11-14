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
package io.gravitee.apim.gateway.tests.sdk.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import io.gravitee.gateway.genericevent.DefaultGenericEventManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GenericEventDeploymentsTest {

    private static final String KIND = "feature.rule";
    private static final Instant UPDATED_AT = Instant.parse("2026-11-10T10:00:00Z");

    private final DefaultGenericEventManager genericEventManager = new DefaultGenericEventManager();
    private final GenericEventDeployments cut = new GenericEventDeployments(() -> genericEventManager);
    private final List<String> calls = new ArrayList<>();

    @BeforeEach
    void setUp() {
        genericEventManager.subscribe(
            "feature",
            new GenericEventSubscriber() {
                @Override
                public void onUpsert(GenericEvent event) {
                    calls.add("upsert:" + event.id());
                }

                @Override
                public void onRemove(GenericEvent event) {
                    calls.add("remove:" + event.id());
                }

                @Override
                public void onCommit() {
                    calls.add("commit");
                }
            }
        );
        calls.clear();
    }

    @Test
    void should_deliver_all_the_events_of_a_class_deployment_then_commit_once() {
        cut.deployForClass(List.of(event("rule-1"), event("rule-2")));

        assertThat(calls).containsExactly("upsert:rule-1", "upsert:rule-2", "commit");
    }

    @Test
    void should_remove_the_events_deployed_at_class_level_then_commit_once() {
        cut.deployForClass(List.of(event("rule-1"), event("rule-2")));
        calls.clear();

        cut.undeployForClass();

        assertThat(calls).containsExactlyInAnyOrder("remove:rule-1", "remove:rule-2", "commit");
        assertThat(calls.getLast()).isEqualTo("commit");
        assertThat(genericEventManager.size()).isZero();
    }

    @Test
    void should_not_commit_when_there_is_nothing_to_undeploy() {
        cut.undeployForTest();

        assertThat(calls).isEmpty();
    }

    @Test
    void should_fail_when_deploying_the_same_event_twice_at_class_level() {
        assertThatThrownBy(() -> cut.deployForClass(List.of(event("rule-1"), event("rule-1"))))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessageContaining(KIND)
            .hasMessageContaining("rule-1");
    }

    @Test
    void should_fail_when_deploying_at_test_level_an_event_deployed_at_class_level() {
        cut.deployForClass(List.of(event("rule-1")));

        assertThatThrownBy(() -> cut.deployForTest(List.of(event("rule-1")))).isInstanceOf(PreconditionViolationException.class);
    }

    @Test
    void should_only_remove_the_events_deployed_at_test_level() {
        cut.deployForClass(List.of(event("rule-1")));
        cut.deployForTest(List.of(event("rule-2")));
        calls.clear();

        cut.undeployForTest();

        assertThat(calls).containsExactly("remove:rule-2", "commit");
        assertThat(genericEventManager.size()).isEqualTo(1);
    }

    @Test
    void should_deliver_and_commit_an_event_deployed_from_the_test() {
        cut.deployFromTest(event("rule-1"));

        assertThat(calls).containsExactly("upsert:rule-1", "commit");
    }

    @Test
    void should_replace_an_event_the_test_already_deployed() {
        cut.deployFromTest(event("rule-1", UPDATED_AT));
        calls.clear();

        cut.deployFromTest(event("rule-1", UPDATED_AT.plusMillis(1)));

        assertThat(calls).containsExactly("upsert:rule-1", "commit");
        assertThat(genericEventManager.size()).isEqualTo(1);
    }

    /** The manager ignores a copy that is not newer than the one it holds, so the test would run against the old one. */
    @Test
    void should_fail_when_the_test_replaces_an_event_with_a_copy_that_is_not_newer() {
        cut.deployFromTest(event("rule-1", UPDATED_AT));
        calls.clear();

        assertThatThrownBy(() -> cut.deployFromTest(event("rule-1", UPDATED_AT)))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessageContaining(KIND)
            .hasMessageContaining("rule-1");
        assertThat(calls).isEmpty();
    }

    @Test
    void should_deploy_nothing_at_class_level_when_one_event_of_the_batch_is_a_duplicate() {
        assertThatThrownBy(() -> cut.deployForClass(List.of(event("rule-1"), event("rule-2"), event("rule-1")))).isInstanceOf(
            PreconditionViolationException.class
        );

        assertThat(calls).isEmpty();
        cut.deployForClass(List.of(event("rule-1"), event("rule-2")));
        assertThat(calls).containsExactly("upsert:rule-1", "upsert:rule-2", "commit");
    }

    @Test
    void should_deploy_nothing_at_test_level_when_one_event_of_the_batch_is_deployed_at_class_level() {
        cut.deployForClass(List.of(event("rule-2")));
        calls.clear();

        assertThatThrownBy(() -> cut.deployForTest(List.of(event("rule-1"), event("rule-2")))).isInstanceOf(
            PreconditionViolationException.class
        );

        assertThat(calls).isEmpty();
        cut.deployForTest(List.of(event("rule-1")));
        assertThat(calls).containsExactly("upsert:rule-1", "commit");
    }

    @Test
    void should_remove_and_commit_an_event_undeployed_from_the_test() {
        cut.deployFromTest(event("rule-1"));
        calls.clear();

        cut.undeployFromTest(event("rule-1"));

        assertThat(calls).containsExactly("remove:rule-1", "commit");
    }

    @Test
    void should_fail_when_the_test_deploys_an_event_deployed_at_class_level() {
        cut.deployForClass(List.of(event("rule-1")));

        assertThatThrownBy(() -> cut.deployFromTest(event("rule-1")))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessageContaining("class level");
    }

    @Test
    void should_fail_when_the_test_undeploys_an_event_deployed_at_class_level() {
        cut.deployForClass(List.of(event("rule-1")));

        assertThatThrownBy(() -> cut.undeployFromTest(event("rule-1")))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessageContaining("class level");
    }

    private static GenericEvent event(String id) {
        return event(id, UPDATED_AT);
    }

    private static GenericEvent event(String id, Instant updatedAt) {
        return new GenericEvent(KIND, id, "DEFAULT", "DEFAULT", null, updatedAt, "{}");
    }
}
