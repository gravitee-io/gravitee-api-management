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
package io.gravitee.apim.core.plan.use_case;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.json_patch.domain_service.JsonPatchDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.exception.PlanForApiNotFoundException;
import io.gravitee.apim.core.plan.exception.PlanPatchNotAllowedException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class PatchPlanUseCase {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_SECURITY_CONFIGURATION = "security";
    private static final String FIELD_VALIDATION = "validation";
    private static final String FIELD_SELECTION_RULE = "selectionRule";
    private static final String FIELD_TAGS = "tags";
    private static final String FIELD_EXCLUDED_GROUPS = "excludedGroups";
    private static final String FIELD_CHARACTERISTICS = "characteristics";
    private static final String FIELD_COMMENT_REQUIRED = "commentRequired";
    private static final String FIELD_GENERAL_CONDITIONS = "generalConditions";
    private static final String FIELD_FLOWS = "flows";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
        FIELD_NAME,
        FIELD_DESCRIPTION,
        FIELD_SECURITY_CONFIGURATION,
        FIELD_VALIDATION,
        FIELD_SELECTION_RULE,
        FIELD_TAGS,
        FIELD_EXCLUDED_GROUPS,
        FIELD_CHARACTERISTICS,
        FIELD_COMMENT_REQUIRED,
        FIELD_GENERAL_CONDITIONS,
        FIELD_FLOWS
    );

    private static final Set<String> NON_NULLABLE_FIELDS = Set.of(
        FIELD_NAME,
        FIELD_VALIDATION,
        FIELD_SECURITY_CONFIGURATION,
        FIELD_COMMENT_REQUIRED
    );

    static final int MAX_PATCH_OPS = 200;

    private final ApiCrudService apiCrudService;
    private final PlanCrudService planCrudService;
    private final FlowCrudService flowCrudService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final JsonPatchDomainService jsonPatchDomainService;
    private final ObjectMapper objectMapper;
    private final PlanFlowsConverter planFlowsConverter;

    public interface PlanFlowsConverter {
        JsonNode toCurrentFlowsNode(@Nonnull List<Flow> flows);

        List<Flow> fromPatchedFlowsNode(JsonNode flowsNode);
    }

    public Output execute(Input input) {
        if (input.patchType() == null) {
            throw new ValidationDomainException("A patchType must be provided");
        }

        var plan = planCrudService
            .findByPlanIdAndReferenceIdAndReferenceType(input.planId(), input.apiId(), GenericPlanEntity.ReferenceType.API.name())
            .orElseThrow(() -> new PlanForApiNotFoundException(input.planId(), input.apiId()));

        var api = apiCrudService.get(input.apiId());

        if (!DefinitionVersion.V4.equals(api.getDefinitionVersion())) {
            throw new ValidationDomainException("Plan PATCH is only supported for V4 API plans");
        }
        if (!ApiType.PROXY.equals(api.getType())) {
            throw new ValidationDomainException("Plan PATCH is only supported for HTTP Proxy API plans");
        }

        var currentFlows = flowCrudService.getPlanV4Flows(plan.getId());

        var currentNode = toJsonNode(plan, currentFlows);
        var patchNode = parseBody(input.patchBody());

        enforceAllowList(input.patchType(), patchNode);

        var explicitNulls = collectExplicitNulls(input.patchType(), patchNode);
        rejectNullOnNonNullableFields(explicitNulls);

        var patchedNode = applyPatch(input.patchType(), patchNode, currentNode, input.planId(), input.apiId());

        var patched = applyPatchedValues(plan, patchedNode, explicitNulls);
        var patchedFlows = extractFlows(patchedNode, currentFlows, explicitNulls);

        if (input.dryRun()) {
            updatePlanDomainService.validate(patched, patchedFlows, api, input.auditInfo());
            return new Output(new PlanWithFlows(patched, patchedFlows));
        }

        var saved = updatePlanDomainService.update(patched, patchedFlows, null, api, input.auditInfo());
        return new Output(new PlanWithFlows(saved, patchedFlows));
    }

    private JsonNode toJsonNode(Plan plan, List<Flow> currentFlows) {
        var node = objectMapper.createObjectNode();
        var def = plan.getPlanDefinitionHttpV4();
        node.put(FIELD_NAME, plan.getName());
        node.put(FIELD_DESCRIPTION, plan.getDescription());
        node.put(FIELD_VALIDATION, plan.getValidation() != null ? plan.getValidation().name() : null);
        node.put(FIELD_COMMENT_REQUIRED, plan.isCommentRequired());
        node.put(FIELD_GENERAL_CONDITIONS, plan.getGeneralConditions());
        if (def != null) {
            node.put(FIELD_SELECTION_RULE, def.getSelectionRule());
            if (def.getSecurity() != null) {
                node.set(FIELD_SECURITY_CONFIGURATION, objectMapper.valueToTree(def.getSecurity()));
            }
            if (def.getTags() != null) {
                node.set(FIELD_TAGS, objectMapper.valueToTree(def.getTags()));
            }
        }
        if (currentFlows != null) {
            try {
                node.set(FIELD_FLOWS, planFlowsConverter.toCurrentFlowsNode(currentFlows));
            } catch (RuntimeException e) {
                throw new ValidationDomainException("Failed to serialise current flows for patch merge", e);
            }
        }
        if (plan.getExcludedGroups() != null) {
            node.set(FIELD_EXCLUDED_GROUPS, objectMapper.valueToTree(plan.getExcludedGroups()));
        }
        if (plan.getCharacteristics() != null) {
            node.set(FIELD_CHARACTERISTICS, objectMapper.valueToTree(plan.getCharacteristics()));
        }
        return node;
    }

    private JsonNode parseBody(String body) {
        if (body == null || body.isBlank()) {
            throw new ValidationDomainException("Patch body is required");
        }
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new ValidationDomainException("Invalid patch body: " + e.getMessage(), e);
        }
    }

    private void enforceAllowList(PatchType patchType, JsonNode patchNode) {
        if (patchType == PatchType.JSON_PATCH) {
            enforceAllowListJsonPatch(patchNode);
        } else {
            enforceAllowListMergePatch(patchNode);
        }
    }

    private void enforceAllowListJsonPatch(JsonNode patchArray) {
        if (!patchArray.isArray()) {
            throw new ValidationDomainException("JSON Patch body must be a JSON array");
        }
        if (patchArray.size() > MAX_PATCH_OPS) {
            throw new ValidationDomainException("JSON Patch request exceeds maximum of " + MAX_PATCH_OPS + " operations");
        }
        var index = 0;
        for (JsonNode op : patchArray) {
            // Allow-list applies to every op, including the read-only RFC 6902 `test` op, so non-patchable pointers cannot be probed.
            var pathNode = op.path("path");
            if (pathNode.isMissingNode() || !pathNode.isTextual()) {
                throw new ValidationDomainException("JSON Patch operation at index " + index + " is missing required 'path' field");
            }
            var rawPath = pathNode.asText();
            validateJsonPatchField(index, "path", rawPath);

            var opName = op.path("op").asText();
            if ("move".equals(opName) || "copy".equals(opName)) {
                var fromNode = op.path("from");
                if (fromNode.isMissingNode() || !fromNode.isTextual()) {
                    throw new ValidationDomainException("JSON Patch operation at index " + index + " is missing required 'from' field");
                }
                validateJsonPatchField(index, "from", fromNode.asText());
            }
            index++;
        }
    }

    private void validateJsonPatchField(int opIndex, String fieldLabel, String pointer) {
        if (!pointer.isEmpty() && !pointer.startsWith("/")) {
            throw new ValidationDomainException(
                "JSON Patch operation at index " +
                    opIndex +
                    " has '" +
                    fieldLabel +
                    "' value '" +
                    pointer +
                    "' which is not a valid JSON Pointer"
            );
        }
        var field = extractTopLevelField(pointer);
        if (field.isEmpty()) {
            throw new ValidationDomainException(
                "JSON Patch operation at index " +
                    opIndex +
                    " has '" +
                    fieldLabel +
                    "' value '" +
                    pointer +
                    "' which does not target a specific field"
            );
        }
        validateField(field);
    }

    private void enforceAllowListMergePatch(JsonNode mergePatchNode) {
        if (!mergePatchNode.isObject()) {
            throw new ValidationDomainException("JSON Merge Patch body must be a JSON object");
        }
        var fieldNames = mergePatchNode.fieldNames();
        while (fieldNames.hasNext()) {
            validateField(fieldNames.next());
        }
    }

    private String extractTopLevelField(String jsonPointer) {
        if (jsonPointer.isEmpty()) {
            return "";
        }
        var withoutLeadingSlash = jsonPointer.startsWith("/") ? jsonPointer.substring(1) : jsonPointer;
        var slashIndex = withoutLeadingSlash.indexOf('/');
        return slashIndex < 0 ? withoutLeadingSlash : withoutLeadingSlash.substring(0, slashIndex);
    }

    private Set<String> collectExplicitNulls(PatchType patchType, JsonNode patchNode) {
        var explicitNulls = new HashSet<String>();
        if (patchType == PatchType.JSON_PATCH) {
            for (JsonNode op : patchNode) {
                var opName = op.path("op").asText();
                var pointer = op.path("path").asText();
                var topLevelField = extractTopLevelField(pointer);
                boolean clearsField;
                if ("remove".equals(opName)) {
                    clearsField = true;
                } else if ("add".equals(opName) || "replace".equals(opName)) {
                    clearsField = op.path("value").isNull();
                } else {
                    clearsField = false;
                }
                if (!clearsField) {
                    continue;
                }
                if (pointer.equals("/" + topLevelField)) {
                    explicitNulls.add(topLevelField);
                }
            }
        } else {
            var fields = patchNode.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (entry.getValue().isNull()) {
                    explicitNulls.add(entry.getKey());
                }
            }
        }
        return explicitNulls;
    }

    private void rejectNullOnNonNullableFields(Set<String> explicitNulls) {
        for (String field : explicitNulls) {
            if (NON_NULLABLE_FIELDS.contains(field)) {
                throw new ValidationDomainException("Field '" + field + "' cannot be set to null");
            }
        }
    }

    private void validateField(String field) {
        if ("status".equals(field)) {
            throw new PlanPatchNotAllowedException(field, "use plan lifecycle endpoints to change plan status");
        }
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new PlanPatchNotAllowedException(field, "field is not patchable");
        }
    }

    private JsonNode applyPatch(PatchType patchType, JsonNode patchNode, JsonNode currentNode, String planId, String apiId) {
        try {
            if (patchType == PatchType.JSON_PATCH) {
                return jsonPatchDomainService.applyJsonPatch(patchNode, currentNode);
            }
            return jsonPatchDomainService.applyMergePatch(patchNode, currentNode);
        } catch (ValidationDomainException e) {
            throw new ValidationDomainException(
                "Failed to apply patch on plan [" + planId + "] of API [" + apiId + "]: " + e.getMessage(),
                e
            );
        }
    }

    private Plan applyPatchedValues(Plan existing, JsonNode patchedNode, Set<String> explicitNulls) {
        var name = textOrDefault(patchedNode, FIELD_NAME, explicitNulls, existing.getName());
        if (name == null || name.isBlank()) {
            throw new ValidationDomainException("'name' cannot be blank");
        }

        var description = textOrDefault(patchedNode, FIELD_DESCRIPTION, explicitNulls, existing.getDescription());
        var commentRequired = booleanOrDefault(patchedNode, FIELD_COMMENT_REQUIRED, existing.isCommentRequired());
        var generalConditions = textOrDefault(patchedNode, FIELD_GENERAL_CONDITIONS, explicitNulls, existing.getGeneralConditions());

        var validationNode = patchedNode.get(FIELD_VALIDATION);
        var validation = existing.getValidation();
        if (validationNode != null && !validationNode.isNull()) {
            try {
                validation = Plan.PlanValidationType.valueOf(validationNode.asText());
            } catch (IllegalArgumentException e) {
                throw new ValidationDomainException("Invalid validation value: " + validationNode.asText(), e);
            }
        }

        var excludedGroups = stringListOrDefault(patchedNode, FIELD_EXCLUDED_GROUPS, explicitNulls, existing.getExcludedGroups());
        var characteristics = stringListOrDefault(patchedNode, FIELD_CHARACTERISTICS, explicitNulls, existing.getCharacteristics());

        var updatedPlan = existing
            .toBuilder()
            .name(name)
            .description(description)
            .commentRequired(commentRequired)
            .generalConditions(generalConditions)
            .validation(validation)
            .excludedGroups(excludedGroups)
            .characteristics(characteristics)
            .build();

        var def = existing.getPlanDefinitionHttpV4();
        if (def != null) {
            var selectionRule = textOrDefault(patchedNode, FIELD_SELECTION_RULE, explicitNulls, def.getSelectionRule());

            var existingSecurityType = def.getSecurity() != null ? def.getSecurity().getType() : null;
            var securityNode = patchedNode.get(FIELD_SECURITY_CONFIGURATION);
            PlanSecurity security;
            if (securityNode != null && !securityNode.isNull()) {
                var convertedSecurity = convertField(securityNode, PlanSecurity.class, FIELD_SECURITY_CONFIGURATION);
                if (convertedSecurity == null) {
                    throw new ValidationDomainException("Invalid value for field 'security'");
                }
                security = convertedSecurity.withType(existingSecurityType);
            } else {
                security = def.getSecurity();
            }

            var tags = explicitNulls.contains(FIELD_TAGS) ? null : tagsOrDefault(patchedNode, def.getTags());

            var updatedDef = def.toBuilder().name(name).selectionRule(selectionRule).security(security).tags(tags).build();
            updatedPlan = updatedPlan.toBuilder().planDefinitionHttpV4(updatedDef).build();
        }

        return updatedPlan;
    }

    private List<Flow> extractFlows(JsonNode patchedNode, List<Flow> currentFlows, Set<String> explicitNulls) {
        if (explicitNulls.contains(FIELD_FLOWS)) {
            return List.of();
        }
        var flowsNode = patchedNode.get(FIELD_FLOWS);
        if (flowsNode == null || flowsNode.isNull()) {
            return currentFlows;
        }
        return planFlowsConverter.fromPatchedFlowsNode(flowsNode);
    }

    private <T> T convertField(JsonNode node, Class<T> type, String fieldName) {
        try {
            return objectMapper.convertValue(node, type);
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException("Invalid value for field '" + fieldName + "': " + e.getMessage(), e);
        }
    }

    private <T> T convertField(JsonNode node, TypeReference<T> type, String fieldName) {
        try {
            return objectMapper.convertValue(node, type);
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException("Invalid value for field '" + fieldName + "': " + e.getMessage(), e);
        }
    }

    private String textOrDefault(JsonNode node, String field, Set<String> explicitNulls, String defaultValue) {
        if (explicitNulls.contains(field)) {
            return null;
        }
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        if (!fieldNode.isTextual()) {
            throw new ValidationDomainException("Invalid value for field '" + field + "'");
        }
        return fieldNode.asText(defaultValue);
    }

    private List<String> stringListOrDefault(JsonNode node, String field, Set<String> explicitNulls, List<String> defaultValue) {
        if (explicitNulls.contains(field)) {
            return null;
        }
        if (!node.has(field)) {
            return defaultValue;
        }
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        return convertField(fieldNode, new TypeReference<List<String>>() {}, field);
    }

    private Set<String> tagsOrDefault(JsonNode node, Set<String> defaultValue) {
        var fieldNode = node.get(FIELD_TAGS);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return convertField(fieldNode, new TypeReference<Set<String>>() {}, FIELD_TAGS);
    }

    private boolean booleanOrDefault(JsonNode node, String field, boolean defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        if (!fieldNode.isBoolean()) {
            throw new ValidationDomainException("Invalid value for field '" + field + "'");
        }
        return fieldNode.asBoolean(defaultValue);
    }

    public enum PatchType {
        JSON_PATCH,
        MERGE_PATCH,
    }

    @Builder
    public record Input(String apiId, String planId, PatchType patchType, String patchBody, boolean dryRun, AuditInfo auditInfo) {
        public Input {
            Objects.requireNonNull(apiId, "apiId must not be null");
            Objects.requireNonNull(planId, "planId must not be null");
            Objects.requireNonNull(auditInfo, "auditInfo must not be null");
        }
    }

    public record Output(PlanWithFlows plan) {}
}
