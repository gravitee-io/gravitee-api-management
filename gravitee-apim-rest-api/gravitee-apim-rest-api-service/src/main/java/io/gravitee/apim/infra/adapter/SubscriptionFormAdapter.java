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
package io.gravitee.apim.infra.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import java.util.List;
import java.util.Map;

/**
 * (De)serialization of {@link SubscriptionFormFieldConstraints} to/from the JSON shape shared by
 * the legacy {@code subscription_forms} table and the {@code validationConstraints} entry of a
 * SUBSCRIPTION_FORM navigation item's {@code configuration} blob.
 *
 * @author Gravitee.io Team
 */
public final class SubscriptionFormAdapter {

    public static final ObjectMapper FIELD_CONSTRAINTS_JSON = new ObjectMapper().findAndRegisterModules();

    private SubscriptionFormAdapter() {}

    public static String writeFieldConstraintsJson(SubscriptionFormFieldConstraints constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return "{}";
        }
        try {
            return FIELD_CONSTRAINTS_JSON.writerFor(new TypeReference<Map<String, List<Constraint>>>() {}).writeValueAsString(
                constraints.byFieldKey()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize subscription form field constraints", e);
        }
    }

    public static SubscriptionFormFieldConstraints parseFieldConstraintsJson(String json) {
        if (json == null || json.isBlank()) {
            return SubscriptionFormFieldConstraints.empty();
        }
        try {
            Map<String, List<Constraint>> map = FIELD_CONSTRAINTS_JSON.readValue(json, new TypeReference<>() {});
            return map.isEmpty() ? SubscriptionFormFieldConstraints.empty() : new SubscriptionFormFieldConstraints(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize subscription form field constraints", e);
        }
    }
}
