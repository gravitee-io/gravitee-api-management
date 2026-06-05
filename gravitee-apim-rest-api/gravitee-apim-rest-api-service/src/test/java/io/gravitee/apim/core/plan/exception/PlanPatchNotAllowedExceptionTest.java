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
package io.gravitee.apim.core.plan.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanPatchNotAllowedExceptionTest {

    @Test
    void exposes_field_name_in_parameters_and_includes_reason_in_message() {
        var ex = new PlanPatchNotAllowedException("status", "use lifecycle endpoints");

        assertThat(ex.getParameters()).containsEntry("field", "status");
        assertThat(ex.getMessage()).contains("status").contains("use lifecycle endpoints");
        assertThat(ex.getTechnicalCode()).isEqualTo("plan.patch.fieldNotAllowed");
    }
}
