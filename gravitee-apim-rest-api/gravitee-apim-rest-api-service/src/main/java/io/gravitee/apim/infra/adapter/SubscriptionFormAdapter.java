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
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct adapter for converting between SubscriptionForm (domain) and repository model.
 *
 * @author Gravitee.io Team
 */
@Mapper
public interface SubscriptionFormAdapter {
    SubscriptionFormAdapter INSTANCE = Mappers.getMapper(SubscriptionFormAdapter.class);

    ObjectMapper FIELD_CONSTRAINTS_JSON = new ObjectMapper().findAndRegisterModules();

    static String writeFieldConstraintsJson(SubscriptionFormFieldConstraints constraints) {
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

    static SubscriptionFormFieldConstraints parseFieldConstraintsJson(String json) {
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

    @Mapping(target = "id", source = "id", qualifiedByName = "idToSubscriptionFormId")
    @Mapping(target = "gmdContent", source = "gmdContent", qualifiedByName = "stringToGraviteeMarkdown")
    @Mapping(target = "validationConstraints", source = "validationConstraints", qualifiedByName = "jsonToFieldConstraints")
    SubscriptionForm toEntity(io.gravitee.repository.management.model.SubscriptionForm subscriptionForm);

    @Mapping(target = "id", source = "id", qualifiedByName = "subscriptionFormIdToId")
    @Mapping(target = "gmdContent", source = "gmdContent", qualifiedByName = "graviteeMarkdownToString")
    @Mapping(target = "validationConstraints", source = "validationConstraints", qualifiedByName = "fieldConstraintsToJson")
    io.gravitee.repository.management.model.SubscriptionForm toRepository(SubscriptionForm subscriptionForm);

    @Named("idToSubscriptionFormId")
    default SubscriptionFormId idToSubscriptionFormId(String id) {
        return id != null ? SubscriptionFormId.of(id) : null;
    }

    @Named("subscriptionFormIdToId")
    default String subscriptionFormIdToId(SubscriptionFormId id) {
        return id != null ? id.toString() : null;
    }

    @Named("stringToGraviteeMarkdown")
    default GraviteeMarkdown stringToGraviteeMarkdown(String value) {
        return value != null ? GraviteeMarkdown.of(value) : null;
    }

    @Named("graviteeMarkdownToString")
    default String graviteeMarkdownToString(GraviteeMarkdown gmd) {
        return gmd != null ? gmd.value() : null;
    }

    @Named("jsonToFieldConstraints")
    default SubscriptionFormFieldConstraints jsonToFieldConstraints(String json) {
        return parseFieldConstraintsJson(json);
    }

    @Named("fieldConstraintsToJson")
    default String fieldConstraintsToJson(SubscriptionFormFieldConstraints constraints) {
        return writeFieldConstraintsJson(constraints);
    }
}
