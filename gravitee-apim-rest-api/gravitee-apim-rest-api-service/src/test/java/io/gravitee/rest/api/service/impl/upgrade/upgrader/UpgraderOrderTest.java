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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpgraderOrderTest {

    /**
     * The constraints upgrader derives the validation rules from the inline GMD content and skips rows whose
     * content is no longer inline: it must run before the page content upgrader moves that content out, or a
     * legacy row would never get its constraints.
     */
    @Test
    void should_derive_subscription_form_constraints_before_moving_the_content_out() {
        assertThat(UpgraderOrder.SUBSCRIPTION_FORM_CONSTRAINTS_UPGRADER).isLessThan(UpgraderOrder.SUBSCRIPTION_FORM_PAGE_CONTENT_UPGRADER);
    }
}
