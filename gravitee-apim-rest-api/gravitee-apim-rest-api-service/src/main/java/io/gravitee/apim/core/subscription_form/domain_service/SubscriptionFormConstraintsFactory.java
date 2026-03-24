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

import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxGroupField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.Field;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.InputField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.MinLengthAttribute;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.OptionsAttribute;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.PatternAttribute;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.ReadOnlyValueAttribute;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.TextareaField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link SubscriptionFormFieldConstraints} from a parsed {@link SubscriptionFormSchema}.
 *
 * <p>System-level length limits are always enforced regardless of user configuration.
 * See {@link Constraint.MaxLength#INPUT_MAX_LENGTH} and {@link Constraint.MaxLength#TEXTAREA_MAX_LENGTH}.</p>
 *
 * @author Gravitee.io Team
 */
public final class SubscriptionFormConstraintsFactory {

    private SubscriptionFormConstraintsFactory() {}

    public static SubscriptionFormFieldConstraints fromSchema(SubscriptionFormSchema schema) {
        if (schema == null || schema.isEmpty()) {
            return SubscriptionFormFieldConstraints.empty();
        }
        Map<String, List<Constraint>> map = new LinkedHashMap<>();
        for (Field field : schema.fields()) {
            map.put(field.fieldKey(), List.copyOf(constraintsFor(field)));
        }
        return new SubscriptionFormFieldConstraints(map);
    }

    private static List<Constraint> constraintsFor(Field field) {
        if (field instanceof ReadOnlyValueAttribute ro && ro.readonlyValue() != null) {
            return List.of(new Constraint.ReadOnly(ro.readonlyValue()));
        }

        var out = new ArrayList<Constraint>();

        if (field.required()) {
            if (field instanceof CheckboxField) {
                out.add(new Constraint.MustBeTrue());
            } else if (field instanceof CheckboxGroupField) {
                out.add(new Constraint.NonEmptySelection());
            } else {
                out.add(new Constraint.Required());
            }
        }

        if (field instanceof MinLengthAttribute min && min.minLength() != null) {
            out.add(new Constraint.MinLength(min.minLength()));
        }
        if (field instanceof InputField input) {
            out.add(input.maxLength() != null ? Constraint.MaxLength.forInput(input.maxLength()) : Constraint.MaxLength.forInput());
        } else if (field instanceof TextareaField textarea) {
            out.add(
                textarea.maxLength() != null ? Constraint.MaxLength.forTextarea(textarea.maxLength()) : Constraint.MaxLength.forTextarea()
            );
        }
        if (field instanceof PatternAttribute pat && pat.pattern() != null) {
            out.add(new Constraint.MatchesPattern(pat.pattern()));
        }

        if (field instanceof OptionsAttribute opt && hasOptions(opt.options())) {
            if (field instanceof CheckboxGroupField) {
                out.add(new Constraint.EachOf(opt.options()));
            } else {
                out.add(new Constraint.OneOf(opt.options()));
            }
        }

        return out;
    }

    private static boolean hasOptions(List<String> options) {
        return options != null && !options.isEmpty();
    }
}
