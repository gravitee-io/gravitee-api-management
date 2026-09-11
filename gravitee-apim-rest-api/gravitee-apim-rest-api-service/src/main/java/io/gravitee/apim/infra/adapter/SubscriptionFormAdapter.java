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
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
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
 * <p>The form definition (GMD) lives in a {@code PortalPageContent} referenced by
 * {@code portalPageContentId}; the repository row's inline {@code gmdContent} only survives on rows not
 * migrated yet. That column is therefore never written by {@link #toRepository}, and
 * {@link #toEntity(io.gravitee.repository.management.model.SubscriptionForm, GraviteeMarkdown)}
 * takes the already-loaded content as a separate argument.</p>
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

    @Mapping(target = "id", source = "form.id", qualifiedByName = "idToSubscriptionFormId")
    @Mapping(target = "environmentId", source = "form.environmentId")
    @Mapping(target = "name", source = "form.name")
    @Mapping(target = "portalPageContentId", source = "form.portalPageContentId", qualifiedByName = "idToPortalPageContentId")
    @Mapping(target = "gmdContent", source = "content")
    @Mapping(target = "enabled", source = "form.enabled")
    @Mapping(target = "defaultForm", source = "form.defaultForm")
    @Mapping(target = "validationConstraints", source = "form.validationConstraints", qualifiedByName = "jsonToFieldConstraints")
    SubscriptionForm toEntity(io.gravitee.repository.management.model.SubscriptionForm form, GraviteeMarkdown content);

    @Mapping(target = "id", source = "id", qualifiedByName = "subscriptionFormIdToId")
    @Mapping(target = "portalPageContentId", source = "portalPageContentId", qualifiedByName = "portalPageContentIdToId")
    @Mapping(target = "gmdContent", ignore = true)
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

    @Named("idToPortalPageContentId")
    default PortalPageContentId idToPortalPageContentId(String id) {
        return id != null ? PortalPageContentId.of(id) : null;
    }

    @Named("portalPageContentIdToId")
    default String portalPageContentIdToId(PortalPageContentId id) {
        return id != null ? id.toString() : null;
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
