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
package io.gravitee.apim.infra.domain_service.subscription_form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormDefinitionValidationException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxGroupField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.DynamicOptions;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.InputField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.RadioField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.SelectField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.TextareaField;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SubscriptionFormSchemaGeneratorImplTest {

    SubscriptionFormSchemaGeneratorImpl generator = new SubscriptionFormSchemaGeneratorImpl();

    @ParameterizedTest(name = "content={0}")
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void should_return_empty_schema_for_blank_or_null_content(String content) {
        assertThat(generator.generate(GraviteeMarkdown.of(content)).fields()).isEmpty();
    }

    @Test
    void should_return_empty_schema_when_no_form_fields() {
        assertThat(generator.generate(GraviteeMarkdown.of("<p>No form fields here</p>")).fields()).isEmpty();
    }

    @Test
    void should_parse_gmd_input_with_all_validation_attributes() {
        String gmd = """
            <gmd-input fieldKey="company" required="true" minLength="2" maxLength="100" pattern="[A-Za-z]+"/>
            """;

        SubscriptionFormSchema schema = generator.generate(GraviteeMarkdown.of(gmd));

        assertThat(schema.fields()).hasSize(1);
        assertThat(schema.fields().getFirst())
            .isInstanceOf(InputField.class)
            .satisfies(f -> {
                InputField field = (InputField) f;
                assertThat(field.fieldKey()).isEqualTo("company");
                assertThat(field.required()).isTrue();
                assertThat(field.minLength()).isEqualTo(2);
                assertThat(field.maxLength()).isEqualTo(100);
                assertThat(field.pattern()).isEqualTo("[A-Za-z]+");
                assertThat(field.readonlyValue()).isNull();
            });
    }

    @Test
    void should_parse_gmd_input_with_missing_optional_attributes() {
        assertThat(generator.generate(GraviteeMarkdown.of("<gmd-input fieldKey=\"email\" required=\"false\"/>")).fields().getFirst())
            .isInstanceOf(InputField.class)
            .satisfies(f -> {
                InputField field = (InputField) f;
                assertThat(field.fieldKey()).isEqualTo("email");
                assertThat(field.required()).isFalse();
                assertThat(field.minLength()).isNull();
                assertThat(field.maxLength()).isNull();
                assertThat(field.pattern()).isNull();
                assertThat(field.readonlyValue()).isNull();
            });
    }

    @Test
    void should_parse_gmd_textarea_with_length_constraints() {
        assertThat(
            generator
                .generate(GraviteeMarkdown.of("<gmd-textarea fieldKey=\"use_case\" required=\"true\" minLength=\"20\" maxLength=\"500\"/>"))
                .fields()
                .getFirst()
        )
            .isInstanceOf(TextareaField.class)
            .satisfies(f -> {
                TextareaField field = (TextareaField) f;
                assertThat(field.fieldKey()).isEqualTo("use_case");
                assertThat(field.required()).isTrue();
                assertThat(field.minLength()).isEqualTo(20);
                assertThat(field.maxLength()).isEqualTo(500);
            });
    }

    @ParameterizedTest(name = "<{0}> → {1}")
    @MethodSource("optionsBearingComponents")
    void should_parse_options_bearing_component_with_comma_separated_options(
        String tagName,
        Class<? extends SubscriptionFormSchema.Field> expectedType
    ) {
        String gmd = "<" + tagName + " fieldKey=\"field\" required=\"true\" options=\"Free Tier,Starter,Professional\"/>";

        SubscriptionFormSchema.Field field = generator.generate(GraviteeMarkdown.of(gmd)).fields().getFirst();

        assertThat(field).isInstanceOf(expectedType);
        assertThat(field.fieldKey()).isEqualTo("field");
        assertThat(field.required()).isTrue();
        assertThat(optionsOf(field)).containsExactly("Free Tier", "Starter", "Professional");
    }

    static Stream<Arguments> optionsBearingComponents() {
        return Stream.of(
            Arguments.of("gmd-select", SelectField.class),
            Arguments.of("gmd-radio", RadioField.class),
            Arguments.of("gmd-checkbox-group", CheckboxGroupField.class)
        );
    }

    @Test
    void should_parse_gmd_checkbox_as_simple_boolean_field() {
        assertThat(generator.generate(GraviteeMarkdown.of("<gmd-checkbox fieldKey=\"terms\" required=\"true\"/>")).fields().getFirst())
            .isInstanceOf(CheckboxField.class)
            .satisfies(f -> {
                CheckboxField field = (CheckboxField) f;
                assertThat(field.fieldKey()).isEqualTo("terms");
                assertThat(field.required()).isTrue();
            });
    }

    @Test
    void should_trim_whitespace_in_options() {
        SubscriptionFormSchema.Field field = generator
            .generate(GraviteeMarkdown.of("<gmd-select fieldKey=\"env\" options=\" Production , Staging , Development \"/>"))
            .fields()
            .getFirst();

        assertThat(optionsOf(field)).containsExactly("Production", "Staging", "Development");
    }

    @Test
    void should_return_null_options_when_options_attribute_is_absent() {
        SubscriptionFormSchema.Field field = generator
            .generate(GraviteeMarkdown.of("<gmd-select fieldKey=\"plan\" required=\"true\"/>"))
            .fields()
            .getFirst();

        assertThat(optionsOf(field)).isNull();
    }

    @Test
    void should_capture_readonly_value() {
        assertThat(
            generator.generate(GraviteeMarkdown.of("<gmd-input fieldKey=\"ref\" readonly=\"true\" value=\"REF-123\"/>")).fields().getFirst()
        )
            .isInstanceOf(InputField.class)
            .satisfies(f -> assertThat(((InputField) f).readonlyValue()).isEqualTo("REF-123"));
    }

    @Test
    void should_throw_when_readonly_field_has_no_value_attribute() {
        assertThatThrownBy(() -> generator.generate(GraviteeMarkdown.of("<gmd-input fieldKey=\"ref\" readonly=\"true\"/>")))
            .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
            .hasMessageContaining("ref")
            .hasMessageContaining("value");
    }

    @Test
    void should_throw_when_fieldkey_attribute_is_absent() {
        assertThatThrownBy(() -> generator.generate(GraviteeMarkdown.of("<gmd-input required=\"true\"/>")))
            .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
            .hasMessageContaining("fieldkey")
            .hasMessageContaining("gmd-input");
    }

    @Test
    void should_throw_when_fieldkey_attribute_is_blank() {
        assertThatThrownBy(() -> generator.generate(GraviteeMarkdown.of("<gmd-input fieldKey=\"   \" required=\"true\"/>")))
            .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
            .hasMessageContaining("fieldkey")
            .hasMessageContaining("gmd-input");
    }

    @Test
    void should_parse_multiple_fields_preserving_order() {
        String gmd = """
            <gmd-input fieldKey="company" required="true"/>
            <gmd-select fieldKey="plan" required="true" options="Free,Pro"/>
            <gmd-checkbox fieldKey="terms" required="true"/>
            """;

        List<SubscriptionFormSchema.Field> fields = generator.generate(GraviteeMarkdown.of(gmd)).fields();

        assertThat(fields).hasSize(3);
        assertThat(fields).extracting(SubscriptionFormSchema.Field::fieldKey).containsExactly("company", "plan", "terms");
    }

    @Test
    void should_ignore_non_form_elements() {
        String gmd = """
            <p>Some paragraph</p>
            <gmd-input fieldKey="name" required="true"/>
            <div class="wrapper">text</div>
            """;

        List<SubscriptionFormSchema.Field> fields = generator.generate(GraviteeMarkdown.of(gmd)).fields();

        assertThat(fields).hasSize(1);
        assertThat(fields.getFirst().fieldKey()).isEqualTo("name");
    }

    @ParameterizedTest(name = "<{0}> → dynamic options")
    @MethodSource("optionsBearingComponents")
    void should_parse_el_expression_with_fallback_as_dynamic_options(
        String tagName,
        Class<? extends SubscriptionFormSchema.Field> expectedType
    ) {
        String gmd = "<" + tagName + " fieldKey=\"env\" options=\"{#api.metadata['envs']}:Prod,Test\"/>";

        SubscriptionFormSchema.Field field = generator.generate(GraviteeMarkdown.of(gmd)).fields().getFirst();

        assertThat(field).isInstanceOf(expectedType);
        assertThat(dynamicOptionsOf(field)).isNotNull();
        assertThat(dynamicOptionsOf(field).expression()).isEqualTo("{#api.metadata['envs']}");
        assertThat(dynamicOptionsOf(field).fallback()).containsExactly("Prod", "Test");
        assertThat(optionsOf(field)).isNull();
    }

    @ParameterizedTest(name = "<{0}> → EL without fallback throws")
    @MethodSource("optionsBearingComponents")
    void should_throw_when_el_expression_has_no_fallback_separator(
        String tagName,
        Class<? extends SubscriptionFormSchema.Field> expectedType
    ) {
        String gmd = "<" + tagName + " fieldKey=\"env\" options=\"{#api.metadata['envs']}\"/>";
        assertThatThrownBy(() -> generator.generate(GraviteeMarkdown.of(gmd)))
            .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
            .hasMessageContaining("env")
            .hasMessageContaining("fallback");
    }

    @Test
    void should_parse_el_expression_with_empty_fallback() {
        String gmd = "<gmd-select fieldKey=\"env\" options=\"{#api.metadata['envs']}:\"/>";
        SubscriptionFormSchema.Field field = generator.generate(GraviteeMarkdown.of(gmd)).fields().getFirst();
        assertThat(dynamicOptionsOf(field).fallback()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<String> optionsOf(SubscriptionFormSchema.Field field) {
        return switch (field) {
            case SelectField f -> f.options();
            case RadioField f -> f.options();
            case CheckboxGroupField f -> f.options();
            default -> throw new IllegalArgumentException("Field type has no options: " + field.getClass().getSimpleName());
        };
    }

    private DynamicOptions dynamicOptionsOf(SubscriptionFormSchema.Field field) {
        return switch (field) {
            case SelectField f -> f.dynamicOptions();
            case RadioField f -> f.dynamicOptions();
            case CheckboxGroupField f -> f.dynamicOptions();
            default -> throw new IllegalArgumentException("Field type has no dynamicOptions: " + field.getClass().getSimpleName());
        };
    }
}
