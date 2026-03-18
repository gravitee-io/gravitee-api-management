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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxGroupField;
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
        var schema = new SubscriptionFormSchema(List.of());
        assertThat(SubscriptionFormConstraintsFactory.fromSchema(schema).isEmpty()).isTrue();
    }

    @Test
    void should_map_readonly_input_to_single_read_only_constraint() {
        var schema = new SubscriptionFormSchema(List.of(new InputField("ref", false, "REF-1", 1, 9, ".*")));
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("ref")).containsExactly(new Constraint.ReadOnly("REF-1"));
    }

    @Test
    void should_ignore_required_and_options_when_radio_is_readonly() {
        var schema = new SubscriptionFormSchema(List.of(new RadioField("plan", true, "Free", List.of("Free", "Pro"))));
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("plan")).containsExactly(new Constraint.ReadOnly("Free"));
    }

    @Test
    void should_stack_input_constraints_in_defined_order() {
        var schema = new SubscriptionFormSchema(List.of(new InputField("code", true, null, 2, 10, "[A-Z]+")));
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
        var schema = new SubscriptionFormSchema(List.of(new TextareaField("bio", true, null, 1, 500)));
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("bio")).containsExactly(
            new Constraint.Required(),
            new Constraint.MinLength(1),
            new Constraint.MaxLength(500)
        );
    }

    @Test
    void should_map_select_optional_with_one_of_only() {
        var schema = new SubscriptionFormSchema(List.of(new SelectField("plan", false, List.of("Free", "Pro"))));
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("plan")).containsExactly(new Constraint.OneOf(List.of("Free", "Pro")));
    }

    @Test
    void should_map_required_checkbox_to_must_be_true() {
        var schema = new SubscriptionFormSchema(List.of(new CheckboxField("terms", true, null)));
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("terms")).containsExactly(new Constraint.MustBeTrue());
    }

    @Test
    void should_map_checkbox_group_required_with_each_of() {
        var schema = new SubscriptionFormSchema(List.of(new CheckboxGroupField("tags", true, List.of("A", "B"))));
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
        assertThat(constraints.byFieldKey().get("tags")).containsExactly(
            new Constraint.NonEmptySelection(),
            new Constraint.EachOf(List.of("A", "B"))
        );
    }
}
