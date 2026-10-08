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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.property.Property;
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

    @Test
    void should_not_diff_the_value_of_an_encrypted_property() {
        var before = List.of(new Property("plain", "old", false, false), new Property("secret", "OLD-CIPHER", true, false));
        var after = List.of(new Property("plain", "new", false, false), new Property("secret", "NEW-CIPHER", true, false));

        var diff = processor.diff(before, after);

        assertEquals(
            """
            [{"op":"replace","path":"/0/value","value":"new"}]""",
            diff
        );
    }

    @Test
    void should_record_an_added_encrypted_property_by_key_without_its_value() {
        var diff = processor.diff(List.of(), List.of(new Property("secret", "CIPHER", true, false)));

        assertThat(diff).doesNotContain("CIPHER").contains("\"key\":\"secret\"").contains("\"encrypted\":true");
    }

    @Test
    void should_record_a_plain_property_becoming_encrypted_without_any_value() {
        var diff = processor.diff(
            List.of(new Property("secret", "was-plain", false, false)),
            List.of(new Property("secret", "CIPHER", true, false))
        );

        assertThat(diff).doesNotContain("was-plain").doesNotContain("CIPHER").contains("/0/encrypted");
    }

    @Test
    void should_not_diff_the_value_of_an_encrypted_property_of_an_api() {
        var before = ApiFixtures.aProxyApiV4();
        before.getApiDefinitionHttpV4().setProperties(List.of(new Property("secret", "OLD-CIPHER", true, false)));
        var after = ApiFixtures.aProxyApiV4();
        after.getApiDefinitionHttpV4().setProperties(List.of(new Property("secret", "NEW-CIPHER", true, false)));

        var diff = processor.diff(before, after);

        assertThat(diff).doesNotContain("OLD-CIPHER").doesNotContain("NEW-CIPHER");
    }

    @Test
    void should_not_change_the_audited_object() {
        var before = ApiFixtures.aProxyApiV4();
        var after = ApiFixtures.aProxyApiV4();
        after.getApiDefinitionHttpV4().setProperties(List.of(new Property("secret", "CIPHER", true, false)));

        processor.diff(before, after);

        assertThat(after.getApiDefinitionHttpV4().getProperties()).containsExactly(new Property("secret", "CIPHER", true, false));
    }
}
