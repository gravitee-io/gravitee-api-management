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
package io.gravitee.gamma.rest.core.guardian.model;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianDefinition;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianDeploymentTest {

    private static final Instant SUB_MILLISECOND = Instant.parse("2026-11-09T10:00:00.123456789Z");
    private static final Instant MILLISECOND = Instant.parse("2026-11-09T10:00:00.123Z");

    @Test
    void should_keep_the_dates_of_a_deployment_at_the_precision_the_guardian_store_keeps() {
        var published = new GuardianDeployment.Published(aGuardianDefinition().build(), SUB_MILLISECOND);
        var withdrawn = new GuardianDeployment.Withdrawn(SUB_MILLISECOND, SUB_MILLISECOND);

        assertSoftly(softly -> {
            softly.assertThat(published.at()).as("publication date").isEqualTo(MILLISECOND);
            softly.assertThat(withdrawn.at()).as("withdrawal date").isEqualTo(MILLISECOND);
            softly.assertThat(withdrawn.deployedAt()).as("withdrawn deployment date").isEqualTo(MILLISECOND);
        });
    }

    @Test
    void should_accept_a_withdrawal_that_does_not_carry_its_deployment_date() {
        assertSoftly(softly ->
            softly.assertThat(new GuardianDeployment.Withdrawn(null, SUB_MILLISECOND).deployedAt()).as("unknown deployment date").isNull()
        );
    }
}
