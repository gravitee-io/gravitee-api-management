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
package io.gravitee.apim.core.event.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeploymentNumberTest {

    @Test
    void should_read_the_number_an_event_carries() {
        assertSoftly(softly -> {
            softly.assertThat(DeploymentNumber.of("7").value()).as("number read").isEqualTo(7L);
            softly.assertThat(DeploymentNumber.of("7").asProperty()).as("number written back").isEqualTo("7");
        });
    }

    /** The first deployment of an API is number 1, and an event that carries no number counts as none before it. */
    @Test
    void should_read_an_event_that_carries_no_number_as_the_one_before_the_first() {
        assertSoftly(softly -> {
            softly.assertThat(DeploymentNumber.of(null)).as("number of an event carrying none").isEqualTo(DeploymentNumber.NONE);
            softly.assertThat(DeploymentNumber.NONE.next().asProperty()).as("first deployment number").isEqualTo("1");
        });
    }

    @Test
    void should_open_the_number_after_it() {
        assertSoftly(softly -> {
            softly.assertThat(DeploymentNumber.of("7").next().asProperty()).as("number after 7").isEqualTo("8");
            softly.assertThat(DeploymentNumber.of("7").next().next().asProperty()).as("number after 8").isEqualTo("9");
        });
    }

    /** What is read is handed back to the gateways: a number that cannot be read is a failure, not a guess. */
    @Test
    void should_fail_on_a_number_that_cannot_be_read() {
        assertThatThrownBy(() -> DeploymentNumber.of("not a number"))
            .as("a deployment number that is not a number")
            .isInstanceOf(TechnicalDomainException.class);
    }
}
