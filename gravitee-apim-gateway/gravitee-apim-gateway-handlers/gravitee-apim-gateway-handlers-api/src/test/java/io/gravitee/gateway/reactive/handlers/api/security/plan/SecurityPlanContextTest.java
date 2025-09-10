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
package io.gravitee.gateway.reactive.handlers.api.security.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.v4.plan.AbstractPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SecurityPlanContextTest {

    @Mock
    private Plan v2Plan;

    @Mock
    private AbstractPlan v4Plan;

    @Nested
    @DisplayName("Builder.fromV2")
    class BuilderFromV2Tests {

        @Test
        @DisplayName("Should create context from V2 plan")
        void shouldCreateContextFromV2Plan() {
            // Given
            when(v2Plan.getId()).thenReturn("plan-id");
            when(v2Plan.getName()).thenReturn("Plan Name");
            when(v2Plan.getSelectionRule()).thenReturn("{#selection-rule}");

            // When
            SecurityPlanContext context = SecurityPlanContext.builder().fromV2(v2Plan).build();

            // Then
            assertThat(context.planId()).isEqualTo("plan-id");
            assertThat(context.planName()).isEqualTo("Plan Name");
            assertThat(context.selectionRule()).isEqualTo("{#selection-rule}");
        }
    }

    @Nested
    @DisplayName("Builder.fromV4")
    class BuilderFromV4Tests {

        @Test
        @DisplayName("Should create context from V4 plan")
        void shouldCreateContextFromV4Plan() {
            // Given
            when(v4Plan.getId()).thenReturn("plan-id");
            when(v4Plan.getName()).thenReturn("Plan Name");
            when(v4Plan.getSelectionRule()).thenReturn("#selection-rule");

            // When
            SecurityPlanContext context = SecurityPlanContext.builder().fromV4(v4Plan).build();

            // Then
            assertThat(context.planId()).isEqualTo("plan-id");
            assertThat(context.planName()).isEqualTo("Plan Name");
            assertThat(context.selectionRule()).isEqualTo("#selection-rule");
        }
    }
}
