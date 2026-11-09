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
package testcases;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployGenericEvents;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventRegistry;
import io.gravitee.gateway.api.genericevent.GenericEventSubscriber;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * The subscriber is registered as soon as the gateway context is available, so it sees every delivery. The tests are ordered because the removal of the events
 * of a test is only visible once that test has ended.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployGenericEvents("/generic-events/class-level-event.json")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnableForGatewayTestingExtensionTesting
public class GenericEventDeploymentTestCase extends AbstractGatewayTest {

    private final List<String> calls = new ArrayList<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        applicationContext
            .getBean(GenericEventRegistry.class)
            .subscribe(
                "feature",
                new GenericEventSubscriber() {
                    @Override
                    public void onUpsert(GenericEvent event) {
                        calls.add("upsert:" + event.id() + ":" + event.payload());
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
    @Order(1)
    @DeployGenericEvents("/generic-events/test-level-event.json")
    void should_deliver_the_events_then_commit_once_per_deployment() {
        assertThat(calls).containsExactly(
            "upsert:class-rule:{\"enabled\":true}",
            "commit",
            "upsert:test-rule:{\"enabled\":false,\"tags\":[\"a\",\"b\"]}",
            "commit"
        );
    }

    @Test
    @Order(2)
    void should_remove_the_events_of_the_previous_test_but_not_the_class_ones() {
        assertThat(calls.subList(4, calls.size())).containsExactly("remove:test-rule", "commit");
    }

    @Test
    @Order(3)
    void should_deploy_and_undeploy_an_event_from_the_test() {
        calls.clear();
        GenericEvent genericEvent = new GenericEvent("feature.rule", "programmatic-rule", "DEFAULT", "DEFAULT", null, Instant.now(), "{}");

        deployGenericEvent(genericEvent);
        undeployGenericEvent(genericEvent);

        assertThat(calls).containsExactly("upsert:programmatic-rule:{}", "commit", "remove:programmatic-rule", "commit");
    }

    @Test
    @Order(4)
    void should_remove_an_event_deployed_from_a_test_when_the_test_ends_without_undeploying_it() {
        calls.clear();
        deployGenericEvent(new GenericEvent("feature.rule", "leaked-rule", "DEFAULT", "DEFAULT", null, Instant.now(), "{}"));
    }

    @Test
    @Order(5)
    void should_have_removed_the_event_the_previous_test_left_deployed() {
        assertThat(calls).containsExactly("upsert:leaked-rule:{}", "commit", "remove:leaked-rule", "commit");
    }
}
