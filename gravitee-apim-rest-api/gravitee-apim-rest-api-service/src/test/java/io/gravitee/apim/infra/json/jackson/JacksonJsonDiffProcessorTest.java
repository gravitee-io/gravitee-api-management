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
package io.gravitee.apim.infra.json.jackson;

import static org.junit.jupiter.api.Assertions.*;

import fixtures.core.model.PlanFixtures;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JacksonJsonDiffProcessorTest {

    JacksonJsonDiffProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new JacksonJsonDiffProcessor();
    }

    @Test
    void should_process_diff_of_model_using_JsonRawValue_annotation() {
        var oldPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .planDefinitionHttpV4(
                Plan.builder()
                    .security(
                        PlanSecurity.builder()
                            .type(PlanSecurityType.OAUTH2.getLabel())
                            .configuration(
                                """
                                {"modeStrict": true}"""
                            )
                            .build()
                    )
                    .build()
            )
            .build();

        var newPlan = oldPlan.toBuilder().name("updated name").build();

        var diff = processor.diff(oldPlan, newPlan);

        assertEquals(
            """
            [{"op":"replace","path":"/name","value":"updated name"}]""",
            diff
        );
    }

    @Test
    void should_process_diff_of_array() {
        String[] oldArray = new String[] { "value1", "value2" };
        String[] newArray = new String[] { "value1", "value3", "value4" };

        var diff = processor.diff(oldArray, newArray);

        assertEquals(
            """
            [{"op":"replace","path":"/1","value":"value3"},{"op":"add","path":"/-","value":"value4"}]""",
            diff
        );
    }

    @Test
    void should_process_diff_of_Set() {
        // Using LinkedHashSet to keep insertion order
        var oldSet = new LinkedHashSet<>(List.of("value1", "value2"));
        var newSet = new LinkedHashSet<>(List.of("value3", "value4", "value1"));

        var diff = processor.diff(oldSet, newSet);

        assertEquals(
            """
            [{"op":"replace","path":"/0","value":"value3"},{"op":"replace","path":"/1","value":"value4"},{"op":"add","path":"/-","value":"value1"}]""",
            diff
        );
    }
}
