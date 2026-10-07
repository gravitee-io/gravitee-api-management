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
package io.gravitee.apim.core.plan.model;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PlanFixtures;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanTest {

    @Test
    void update_should_keep_existing_validation_when_updated_plan_has_none() {
        var existing = PlanFixtures.HttpV4.anApiKey().toBuilder().validation(Plan.PlanValidationType.AUTO).build();
        var incoming = PlanFixtures.HttpV4.anApiKey().toBuilder().validation(null).build();

        var result = existing.update(incoming);

        assertThat(result.getValidation()).isEqualTo(Plan.PlanValidationType.AUTO);
    }

    @Test
    void update_should_apply_validation_when_updated_plan_has_one() {
        var existing = PlanFixtures.HttpV4.anApiKey().toBuilder().validation(Plan.PlanValidationType.AUTO).build();
        var incoming = PlanFixtures.HttpV4.anApiKey().toBuilder().validation(Plan.PlanValidationType.MANUAL).build();

        var result = existing.update(incoming);

        assertThat(result.getValidation()).isEqualTo(Plan.PlanValidationType.MANUAL);
    }
}
