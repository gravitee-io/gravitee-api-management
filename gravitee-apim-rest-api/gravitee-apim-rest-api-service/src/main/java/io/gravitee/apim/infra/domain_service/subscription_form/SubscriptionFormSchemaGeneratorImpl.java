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

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormDefinitionValidationException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxGroupField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.DynamicOptions;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.Field;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.InputField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.RadioField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.SelectField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.TextareaField;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

/**
 * Parses GMD content using Jsoup and extracts validation-relevant field attributes
 * into a {@link SubscriptionFormSchema}.
 *
 * <p>Only the following GMD components are recognized as form fields:
 * {@code gmd-input}, {@code gmd-textarea}, {@code gmd-select}, {@code gmd-radio},
 * {@code gmd-checkbox}, {@code gmd-checkbox-group}.</p>
 *
 * <p>UI-only attributes (label, placeholder, disabled, rows, type) are
 * intentionally not captured in the schema.</p>
 *
 * @author Gravitee.io Team
 */
@Service
public class SubscriptionFormSchemaGeneratorImpl implements SubscriptionFormSchemaGenerator {

    /**
     * Matches options values that contain a Gravitee EL expression: {@code {#expression}:fallback1,fallback2}
     * Group 1 = the Gravitee EL snippet (for example {@code {#api.metadata['envs']}}),
     * group 2 = the fallback list (may be empty).
     */
    private static final Pattern EL_OPTIONS_PATTERN = Pattern.compile("^(\\{#.+}):(.*)$", Pattern.DOTALL);

    private static final Set<String> FORM_FIELD_TAGS = Set.of(
        "gmd-input",
        "gmd-textarea",
        "gmd-select",
        "gmd-radio",
        "gmd-checkbox",
        "gmd-checkbox-group"
    );

    @Override
    public SubscriptionFormSchema generate(GraviteeMarkdown gmdContent) {
        String raw = gmdContent.value();
        if (raw == null || raw.isBlank()) {
            return new SubscriptionFormSchema(List.of());
        }

        List<Field> fields = Jsoup.parseBodyFragment(raw)
            .body()
            .getAllElements()
            .stream()
            .filter(el -> FORM_FIELD_TAGS.contains(el.tagName()))
            .map(this::toFieldSchema)
            .toList();

        return new SubscriptionFormSchema(fields);
    }

    private Field toFieldSchema(Element el) {
        String fieldKey = el.attr("fieldkey").trim();
        if (fieldKey.isEmpty()) {
            throw invalidDefinition("GMD form field is missing required 'fieldkey' attribute for element: " + el.tagName());
        }
        boolean required = Boolean.parseBoolean(el.attr("required"));

        return switch (el.tagName()) {
            case "gmd-input" -> InputField.builder()
                .fieldKey(fieldKey)
                .required(required)
                .readonlyValue(parseReadonlyValue(el, fieldKey))
                .minLength(parseNullableInt(el, "minlength"))
                .maxLength(parseNullableInt(el, "maxlength"))
                .pattern(el.hasAttr("pattern") ? el.attr("pattern").trim() : null)
                .build();
            case "gmd-textarea" -> TextareaField.builder()
                .fieldKey(fieldKey)
                .required(required)
                .readonlyValue(parseReadonlyValue(el, fieldKey))
                .minLength(parseNullableInt(el, "minlength"))
                .maxLength(parseNullableInt(el, "maxlength"))
                .build();
            case "gmd-select" -> {
                var parsed = parseOptions(el, fieldKey);
                yield switch (parsed) {
                    case ParsedOptions.NoOptions ignored -> SelectField.builder().fieldKey(fieldKey).required(required).build();
                    case ParsedOptions.StaticOptions(var values) -> SelectField.builder()
                        .fieldKey(fieldKey)
                        .required(required)
                        .options(values)
                        .build();
                    case ParsedOptions.DynamicOptionsParsed(var dynamicOptions) -> SelectField.builder()
                        .fieldKey(fieldKey)
                        .required(required)
                        .dynamicOptions(dynamicOptions)
                        .build();
                };
            }
            case "gmd-radio" -> {
                var parsed = parseOptions(el, fieldKey);
                var readonlyValue = parseReadonlyValue(el, fieldKey);
                yield switch (parsed) {
                    case ParsedOptions.NoOptions ignored -> RadioField.builder()
                        .fieldKey(fieldKey)
                        .required(required)
                        .readonlyValue(readonlyValue)
                        .build();
                    case ParsedOptions.StaticOptions(var values) -> RadioField.builder()
                        .fieldKey(fieldKey)
                        .required(required)
                        .readonlyValue(readonlyValue)
                        .options(values)
                        .build();
                    case ParsedOptions.DynamicOptionsParsed(var dynamicOptions) -> RadioField.builder()
                        .fieldKey(fieldKey)
                        .required(required)
                        .readonlyValue(readonlyValue)
                        .dynamicOptions(dynamicOptions)
                        .build();
                };
            }
            case "gmd-checkbox" -> CheckboxField.builder()
                .fieldKey(fieldKey)
                .required(required)
                .readonlyValue(parseReadonlyValue(el, fieldKey))
                .build();
            case "gmd-checkbox-group" -> {
                var parsed = parseOptions(el, fieldKey);
                yield switch (parsed) {
                    case ParsedOptions.NoOptions ignored -> CheckboxGroupField.builder().fieldKey(fieldKey).required(required).build();
                    case ParsedOptions.StaticOptions(var values) -> CheckboxGroupField.builder()
                        .fieldKey(fieldKey)
                        .required(required)
                        .options(values)
                        .build();
                    case ParsedOptions.DynamicOptionsParsed(var dynamicOptions) -> CheckboxGroupField.builder()
                        .fieldKey(fieldKey)
                        .required(required)
                        .dynamicOptions(dynamicOptions)
                        .build();
                };
            }
            default -> throw invalidDefinition("Unknown GMD form field tag: " + el.tagName());
        };
    }

    private String parseReadonlyValue(Element el, String fieldKey) {
        boolean readonly = Boolean.parseBoolean(el.attr("readonly"));
        if (readonly && !el.hasAttr("value")) {
            throw invalidDefinition("Readonly field '" + fieldKey + "' (" + el.tagName() + ") must have a 'value' attribute");
        }
        return readonly ? el.attr("value").trim() : null;
    }

    private sealed interface ParsedOptions
        permits ParsedOptions.NoOptions, ParsedOptions.StaticOptions, ParsedOptions.DynamicOptionsParsed {
        record NoOptions() implements ParsedOptions {}

        record StaticOptions(List<String> values) implements ParsedOptions {}

        record DynamicOptionsParsed(DynamicOptions value) implements ParsedOptions {}
    }

    private ParsedOptions parseOptions(Element el, String fieldKey) {
        if (!el.hasAttr("options")) {
            return new ParsedOptions.NoOptions();
        }
        String raw = el.attr("options").trim();

        // Check if the whole value starts with {# — could be EL or a misconfigured EL without fallback.
        if (raw.startsWith("{#")) {
            Matcher m = EL_OPTIONS_PATTERN.matcher(raw);
            if (!m.matches()) {
                throw invalidDefinition(
                    "Field '" +
                        fieldKey +
                        "': EL expression in 'options' requires a fallback list. " +
                        "Use the format: {#expression}:fallback1,fallback2"
                );
            }
            String expression = m.group(1).trim();
            List<String> fallback = parseCommaList(m.group(2));
            return new ParsedOptions.DynamicOptionsParsed(new DynamicOptions(expression, fallback));
        }

        return new ParsedOptions.StaticOptions(parseCommaList(raw));
    }

    private SubscriptionFormDefinitionValidationException invalidDefinition(String message) {
        return new SubscriptionFormDefinitionValidationException(List.of(message));
    }

    private Integer parseNullableInt(Element el, String attrName) {
        if (!el.hasAttr(attrName)) {
            return null;
        }
        try {
            return Integer.parseInt(el.attr(attrName).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> parseCommaList(String raw) {
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
