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
package io.gravitee.apim.core.subscription_form.domain_service;

import static fixtures.core.model.SubscriptionFormSchemaFixtures.schema;
import static io.gravitee.apim.core.subscription_form.model.Constraint.MaxLength.INPUT_MAX_LENGTH;
import static io.gravitee.apim.core.subscription_form.model.Constraint.MaxLength.TEXTAREA_MAX_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxGroupField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.DynamicOptions;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.InputField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.RadioField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.SelectField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.TextareaField;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionFormConstraintsFactoryTest {

    @Test
    void should_return_empty_when_schema_is_null() {
        assertThat(SubscriptionFormConstraintsFactory.fromSchema(null).isEmpty()).isTrue();
    }

    @Test
    void should_return_empty_when_schema_has_no_fields() {
        var schema = schema();
        assertThat(SubscriptionFormConstraintsFactory.fromSchema(schema).isEmpty()).isTrue();
    }

    @Test
    void should_map_readonly_input_to_single_read_only_constraint() {
        var schema = schema(
            InputField.builder().fieldKey("ref").required(false).readonlyValue("REF-1").minLength(1).maxLength(9).pattern(".*").build()
        );
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("ref")).containsExactly(new Constraint.ReadOnly("REF-1"));
    }

    @Test
    void should_ignore_required_and_options_when_radio_is_readonly() {
        var schema = schema(
            RadioField.builder().fieldKey("plan").required(true).readonlyValue("Free").options(List.of("Free", "Pro")).build()
        );
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("plan")).containsExactly(new Constraint.ReadOnly("Free"));
    }

    @Test
    void should_stack_input_constraints_in_defined_order() {
        var schema = schema(InputField.builder().fieldKey("code").required(true).minLength(2).maxLength(10).pattern("[A-Z]+").build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("code")).containsExactly(
            new Constraint.Required(),
            new Constraint.MinLength(2),
            new Constraint.MaxLength(10),
            new Constraint.MatchesPattern("[A-Z]+")
        );
    }

    @Test
    void should_map_textarea_without_pattern() {
        var schema = schema(TextareaField.builder().fieldKey("bio").required(true).minLength(1).maxLength(500).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("bio")).containsExactly(
            new Constraint.Required(),
            new Constraint.MinLength(1),
            new Constraint.MaxLength(500)
        );
    }

    @Test
    void should_map_select_optional_with_one_of_only() {
        var schema = schema(SelectField.builder().fieldKey("plan").required(false).options(List.of("Free", "Pro")).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("plan")).containsExactly(new Constraint.OneOf(List.of("Free", "Pro")));
    }

    @Test
    void should_map_required_checkbox_to_must_be_true() {
        var schema = schema(CheckboxField.builder().fieldKey("terms").required(true).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("terms")).containsExactly(new Constraint.MustBeTrue());
    }

    @Test
    void should_apply_system_max_length_to_input_when_no_user_max_defined() {
        var schema = schema(InputField.builder().fieldKey("name").required(false).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("name")).containsExactly(new Constraint.MaxLength(INPUT_MAX_LENGTH));
    }

    @Test
    void should_apply_system_max_length_to_textarea_when_no_user_max_defined() {
        var schema = schema(TextareaField.builder().fieldKey("bio").required(false).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("bio")).containsExactly(new Constraint.MaxLength(TEXTAREA_MAX_LENGTH));
    }

    @Test
    void should_cap_input_max_length_at_system_limit_when_user_exceeds_it() {
        var schema = schema(InputField.builder().fieldKey("name").required(false).maxLength(1000).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("name")).containsExactly(new Constraint.MaxLength(INPUT_MAX_LENGTH));
    }

    @Test
    void should_cap_textarea_max_length_at_system_limit_when_user_exceeds_it() {
        var schema = schema(TextareaField.builder().fieldKey("notes").required(false).maxLength(9999).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("notes")).containsExactly(new Constraint.MaxLength(TEXTAREA_MAX_LENGTH));
    }

    @Test
    void should_preserve_user_max_length_when_within_system_limit() {
        var schema = schema(InputField.builder().fieldKey("code").required(false).maxLength(50).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("code")).containsExactly(new Constraint.MaxLength(50));
    }

    @Test
    void should_map_checkbox_group_required_with_each_of() {
        var schema = schema(CheckboxGroupField.builder().fieldKey("tags").required(true).options(List.of("A", "B")).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("tags")).containsExactly(
            new Constraint.NonEmptySelection(),
            new Constraint.EachOf(List.of("A", "B"))
        );
    }

    @Test
    void should_emit_dynamic_one_of_for_select_with_dynamic_options() {
        var dynamic = new DynamicOptions("{#api.metadata['envs']}", List.of("Prod", "Test"));
        var schema = schema(SelectField.builder().fieldKey("env").required(false).dynamicOptions(dynamic).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("env")).containsExactly(
            Constraint.OneOf.dynamic("{#api.metadata['envs']}", List.of("Prod", "Test"))
        );
    }

    @Test
    void should_emit_dynamic_one_of_for_radio_with_dynamic_options() {
        var dynamic = new DynamicOptions("{#api.metadata['plans']}", List.of("Free"));
        var schema = schema(RadioField.builder().fieldKey("plan").required(true).dynamicOptions(dynamic).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("plan")).containsExactly(
            new Constraint.Required(),
            Constraint.OneOf.dynamic("{#api.metadata['plans']}", List.of("Free"))
        );
    }

    @Test
    void should_emit_dynamic_each_of_for_checkbox_group_with_dynamic_options() {
        var dynamic = new DynamicOptions("{#api.metadata['tags']}", List.of("Alpha", "Beta"));
        var schema = schema(CheckboxGroupField.builder().fieldKey("tags").required(true).dynamicOptions(dynamic).build());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("tags")).containsExactly(
            new Constraint.NonEmptySelection(),
            Constraint.EachOf.dynamic("{#api.metadata['tags']}", List.of("Alpha", "Beta"))
        );
    }
}
