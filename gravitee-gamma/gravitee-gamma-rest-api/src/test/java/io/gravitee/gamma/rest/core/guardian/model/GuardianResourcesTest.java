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

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.resource.Resource;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianResourcesTest {

    private static final Resource ENABLED = new Resource("judge", "ai-model-decision", "{}", true);
    private static final Resource DISABLED = new Resource("judge", "ai-model-decision-v2", "{}", false);

    @Test
    void should_find_the_enabled_resource_of_a_name() {
        var guardian = guardianHolding(DISABLED, ENABLED);

        assertThat(guardian.enabledResourceNamed("judge")).contains(ENABLED);
        assertThat(guardian.holdsResourceNamed("judge")).isTrue();
    }

    /** The gateway only loads the enabled resources: a disabled one is held, but not handed to a policy. */
    @Test
    void should_hold_a_disabled_resource_without_offering_it() {
        var guardian = guardianHolding(DISABLED);

        assertThat(guardian.enabledResourceNamed("judge")).isEmpty();
        assertThat(guardian.holdsResourceNamed("judge")).isTrue();
    }

    @Test
    void should_hold_no_resource_of_another_name() {
        var guardian = guardianHolding(ENABLED);

        assertThat(guardian.enabledResourceNamed("missing")).isEmpty();
        assertThat(guardian.holdsResourceNamed("missing")).isFalse();
    }

    @Test
    void should_hold_nothing_without_resources() {
        var guardian = aGuardian(definition -> definition.resources(null)).build();

        assertThat(guardian.enabledResourceNamed("judge")).isEmpty();
        assertThat(guardian.holdsResourceNamed("judge")).isFalse();
    }

    private static Guardian guardianHolding(Resource... resources) {
        return aGuardian(definition -> definition.resources(List.of(resources))).build();
    }
}
