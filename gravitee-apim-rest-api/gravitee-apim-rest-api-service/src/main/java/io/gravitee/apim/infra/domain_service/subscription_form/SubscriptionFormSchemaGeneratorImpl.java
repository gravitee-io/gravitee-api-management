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
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxGroupField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.Field;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.InputField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.RadioField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.SelectField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.TextareaField;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
            throw new IllegalArgumentException("GMD form field is missing required 'fieldkey' attribute for element: " + el.tagName());
        }
        boolean required = Boolean.parseBoolean(el.attr("required"));

        return switch (el.tagName()) {
            case "gmd-input" -> new InputField(
                fieldKey,
                required,
                parseReadonlyValue(el, fieldKey),
                parseNullableInt(el, "minlength"),
                parseNullableInt(el, "maxlength"),
                el.hasAttr("pattern") ? el.attr("pattern").trim() : null
            );
            case "gmd-textarea" -> new TextareaField(
                fieldKey,
                required,
                parseReadonlyValue(el, fieldKey),
                parseNullableInt(el, "minlength"),
                parseNullableInt(el, "maxlength")
            );
            case "gmd-select" -> new SelectField(fieldKey, required, parseOptions(el));
            case "gmd-radio" -> new RadioField(fieldKey, required, parseReadonlyValue(el, fieldKey), parseOptions(el));
            case "gmd-checkbox" -> new CheckboxField(fieldKey, required, parseReadonlyValue(el, fieldKey));
            case "gmd-checkbox-group" -> new CheckboxGroupField(fieldKey, required, parseOptions(el));
            default -> throw new IllegalArgumentException("Unknown GMD form field tag: " + el.tagName());
        };
    }

    private String parseReadonlyValue(Element el, String fieldKey) {
        boolean readonly = Boolean.parseBoolean(el.attr("readonly"));
        if (readonly && !el.hasAttr("value")) {
            throw new IllegalArgumentException("Readonly field '" + fieldKey + "' (" + el.tagName() + ") must have a 'value' attribute");
        }
        return readonly ? el.attr("value").trim() : null;
    }

    private List<String> parseOptions(Element el) {
        return el.hasAttr("options") ? parseCommaList(el.attr("options")) : null;
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
