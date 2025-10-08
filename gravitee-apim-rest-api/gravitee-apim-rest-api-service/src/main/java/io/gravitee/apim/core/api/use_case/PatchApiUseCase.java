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
package io.gravitee.apim.core.api.use_case;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiPatchNotAllowedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.json_patch.domain_service.JsonPatchDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.apim.core.workflow.query_service.WorkflowQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.tracing.Tracing;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class PatchApiUseCase {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_API_VERSION = "apiVersion";
    private static final String FIELD_VISIBILITY = "visibility";
    private static final String FIELD_LABELS = "labels";
    private static final String FIELD_TAGS = "tags";
    private static final String FIELD_LIFECYCLE_STATE = "lifecycleState";
    private static final String FIELD_CATEGORIES = "categories";
    private static final String FIELD_ANALYTICS = "analytics";
    private static final String FIELD_FAILOVER = "failover";
    private static final String FIELD_FLOW_EXECUTION = "flowExecution";
    private static final String FIELD_SERVICES = "services";
    private static final String FIELD_ALLOWED_IN_API_PRODUCTS = "allowedInApiProducts";
    private static final String FIELD_ALLOW_MULTI_JWT_OAUTH2_SUBSCRIPTIONS = "allowMultiJwtOauth2Subscriptions";
    private static final String FIELD_DISABLE_MEMBERSHIP_NOTIFICATIONS = "disableMembershipNotifications";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_RESPONSE_TEMPLATES = "responseTemplates";
    private static final String FIELD_FLOWS = "flows";
    private static final String FIELD_RESOURCES = "resources";
    private static final String FIELD_LISTENERS = "listeners";
    private static final String FIELD_ENDPOINT_GROUPS = "endpointGroups";
    private static final String FIELD_GROUPS = "groups";

    private static final String JSON_PATCH_PATH_PREFIX = "/";

    private static final Pattern RESOURCE_CONFIGURATION_DEEP_PATH = Pattern.compile("/resources/(\\d+|-)/configuration/.+");
    private static final String RESOURCE_CONFIGURATION_DEEP_PATH_MESSAGE =
        "operations under /resources/N/configuration/... are not supported; replace the full configuration object at /resources/N/configuration";

    private static final Pattern ENDPOINT_CONFIGURATION_DEEP_PATH = Pattern.compile(
        "/endpointGroups/[^/]+/endpoints/[^/]+/configuration/.+"
    );
    private static final Pattern ENDPOINT_SHARED_CONFIG_OVERRIDE_DEEP_PATH = Pattern.compile(
        "/endpointGroups/[^/]+/endpoints/[^/]+/sharedConfigurationOverride/.+"
    );
    private static final Pattern ENDPOINT_GROUP_SHARED_CONFIG_DEEP_PATH = Pattern.compile("/endpointGroups/[^/]+/sharedConfiguration/.+");
    private static final Pattern ENDPOINT_GROUP_SERVICE_CONFIG_DEEP_PATH = Pattern.compile(
        "/endpointGroups/[^/]+/services/[^/]+/configuration/.+"
    );
    private static final Pattern ENDPOINT_SERVICE_CONFIG_DEEP_PATH = Pattern.compile(
        "/endpointGroups/[^/]+/endpoints/[^/]+/services/[^/]+/configuration/.+"
    );
    private static final String ENDPOINT_GROUP_OPAQUE_DEEP_PATH_MESSAGE =
        "operations under /endpointGroups/N/endpoints/N/configuration/..., /endpointGroups/N/endpoints/N/sharedConfigurationOverride/..., /endpointGroups/N/sharedConfiguration/..., /endpointGroups/N/services/S/configuration/..., or /endpointGroups/N/endpoints/N/services/S/configuration/... are not supported; replace the full configuration object";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
        FIELD_NAME,
        FIELD_DESCRIPTION,
        FIELD_API_VERSION,
        FIELD_VISIBILITY,
        FIELD_LABELS,
        FIELD_TAGS,
        FIELD_LIFECYCLE_STATE,
        FIELD_CATEGORIES,
        FIELD_ANALYTICS,
        FIELD_FAILOVER,
        FIELD_FLOW_EXECUTION,
        FIELD_SERVICES,
        FIELD_ALLOWED_IN_API_PRODUCTS,
        FIELD_ALLOW_MULTI_JWT_OAUTH2_SUBSCRIPTIONS,
        FIELD_DISABLE_MEMBERSHIP_NOTIFICATIONS,
        FIELD_PROPERTIES,
        FIELD_RESPONSE_TEMPLATES,
        FIELD_FLOWS,
        FIELD_RESOURCES,
        FIELD_LISTENERS,
        FIELD_ENDPOINT_GROUPS,
        FIELD_GROUPS
    );

    private static final int MAX_PATCH_OPS = 200;

    private static final Set<String> BLOCKED_WITH_HINT = Set.of("state");

    private static final String NULL_NOT_ALLOWED_MESSAGE_FORMAT =
        "'%s' cannot be null; omit the field to leave it unchanged, or send an explicit value";

    private final ApiCrudService apiCrudService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final UpdateApiDomainService updateApiDomainService;
    private final JsonPatchDomainService jsonPatchDomainService;
    private final WorkflowQueryService workflowQueryService;
    private final ObjectMapper objectMapper;
    private final PropertyDomainService propertyDomainService;
    private final ApiV4Deserializer apiV4Deserializer;
    private final FlowCrudService flowCrudService;

    public Output execute(Input input) {
        var existingApi = apiCrudService.get(input.apiId());

        if (!DefinitionVersion.V4.equals(existingApi.getDefinitionVersion())) {
            throw new ApiInvalidDefinitionVersionException(input.apiId());
        }
        if (!ApiType.PROXY.equals(existingApi.getType())) {
            throw new ApiInvalidTypeException(input.apiId(), ApiType.PROXY);
        }

        var currentNode = apiV4Deserializer.toCurrentStateNode(existingApi);
        var patchNode = parseBody(input.patchBody());

        enforceAllowList(input.patchType(), patchNode);

        var patchedNode = applyPatch(input.patchType(), patchNode, currentNode);

        var primaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(input.auditInfo().organizationId(), input.apiId());

        var workflows = workflowQueryService.findAllByApiIdAndType(input.apiId(), Workflow.Type.REVIEW);
        var workflowState = workflows.isEmpty() ? null : workflows.getFirst().getState();

        var updatedApi = applyPatchedValues(existingApi, patchedNode, input.patchType(), patchNode);

        if (input.dryRun()) {
            var validated = updateApiDomainService.validateV4(updatedApi, input.auditInfo());
            var result = flowsWerePatched(input.patchType(), patchNode) ? stripFlowIds(validated) : validated;
            return new Output(result, primaryOwner, workflowState);
        }

        var persisted = updateApiDomainService.updateV4(updatedApi, input.auditInfo());
        if (
            persisted.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api v4Definition && v4Definition.getFlows() != null
        ) {
            var flows = flowCrudService.getApiV4Flows(persisted.getId());
            return new Output(
                persisted.toBuilder().apiDefinitionValue(v4Definition.toBuilder().flows(flows).build()).build(),
                primaryOwner,
                workflowState
            );
        }
        return new Output(persisted, primaryOwner, workflowState);
    }

    private Api stripFlowIds(Api api) {
        var httpV4 = asHttpV4(api);
        if (httpV4 == null || httpV4.getFlows() == null) {
            return api;
        }
        var stripped = httpV4
            .getFlows()
            .stream()
            .map(f -> f == null ? null : (Flow) f.toBuilder().id(null).build())
            .toList();
        return api.toBuilder().apiDefinitionValue(httpV4.toBuilder().flows(stripped).build()).build();
    }

    private boolean flowsWerePatched(PatchType patchType, JsonNode patchNode) {
        if (patchType == PatchType.MERGE_PATCH) {
            return patchNode.has(FIELD_FLOWS);
        }
        if (patchType == PatchType.JSON_PATCH && patchNode.isArray()) {
            String flowsPrefix = JSON_PATCH_PATH_PREFIX + FIELD_FLOWS;
            for (JsonNode op : patchNode) {
                String path = op.path("path").asText();
                if (path.equals(flowsPrefix) || path.startsWith(flowsPrefix + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static io.gravitee.definition.model.v4.Api asHttpV4(Api api) {
        return api != null && api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api httpV4 ? httpV4 : null;
    }

    private static io.gravitee.definition.model.v4.Api requireHttpV4(Api api) {
        var httpV4 = asHttpV4(api);
        if (httpV4 != null) {
            return httpV4;
        }
        throw new IllegalStateException("API definition is not an HTTP v4 definition");
    }

    private JsonNode parseBody(String body) {
        if (body == null || body.isBlank()) {
            throw new ValidationDomainException("Patch body is required");
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
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
            var pathNode = op.path("path");
            if (pathNode.isMissingNode() || !pathNode.isTextual()) {
                throw new ValidationDomainException("JSON Patch operation at index " + index + " is missing required 'path' field");
            }
            var rawPath = pathNode.asText();
            validateJsonPatchField(index, "path", rawPath);
            if (isResourceConfigurationDeepPath(rawPath)) {
                throw new ApiPatchNotAllowedException(rawPath, RESOURCE_CONFIGURATION_DEEP_PATH_MESSAGE);
            }
            if (isEndpointGroupOpaqueDeepPath(rawPath)) {
                throw new ApiPatchNotAllowedException(rawPath, ENDPOINT_GROUP_OPAQUE_DEEP_PATH_MESSAGE);
            }
            var opType = op.path("op").asText();
            if ("move".equals(opType) || "copy".equals(opType)) {
                validateMoveOrCopyFromField(index, op);
            }
            index++;
        }
    }

    private void validateMoveOrCopyFromField(int opIndex, JsonNode op) {
        var fromNode = op.path("from");
        if (fromNode.isMissingNode()) {
            throw new ValidationDomainException("JSON Patch operation at index " + opIndex + " is missing required 'from' field");
        }
        if (!fromNode.isTextual()) {
            throw new ValidationDomainException("JSON Patch operation at index " + opIndex + " has invalid 'from' field: must be a string");
        }
        var rawFrom = fromNode.asText();
        validateJsonPatchField(opIndex, "from", rawFrom);
        if (isResourceConfigurationDeepPath(rawFrom)) {
            throw new ApiPatchNotAllowedException(rawFrom, RESOURCE_CONFIGURATION_DEEP_PATH_MESSAGE);
        }
        if (isEndpointGroupOpaqueDeepPath(rawFrom)) {
            throw new ApiPatchNotAllowedException(rawFrom, ENDPOINT_GROUP_OPAQUE_DEEP_PATH_MESSAGE);
        }
    }

    private static boolean isResourceConfigurationDeepPath(String path) {
        return path != null && RESOURCE_CONFIGURATION_DEEP_PATH.matcher(path).matches();
    }

    private static boolean isEndpointGroupOpaqueDeepPath(String path) {
        return (
            path != null &&
            (ENDPOINT_CONFIGURATION_DEEP_PATH.matcher(path).matches() ||
                ENDPOINT_SHARED_CONFIG_OVERRIDE_DEEP_PATH.matcher(path).matches() ||
                ENDPOINT_GROUP_SHARED_CONFIG_DEEP_PATH.matcher(path).matches() ||
                ENDPOINT_GROUP_SERVICE_CONFIG_DEEP_PATH.matcher(path).matches() ||
                ENDPOINT_SERVICE_CONFIG_DEEP_PATH.matcher(path).matches())
        );
    }

    private void validateJsonPatchField(int opIndex, String pointerName, String pointer) {
        if (!pointer.isEmpty() && !pointer.startsWith("/")) {
            throw new ValidationDomainException(
                "JSON Patch operation at index " +
                    opIndex +
                    " has '" +
                    pointerName +
                    "' value '" +
                    pointer +
                    "' which is not a valid JSON Pointer: must start with '/'"
            );
        }
        var field = extractTopLevelField(pointer);
        if (field.isEmpty()) {
            throw new ValidationDomainException(
                "JSON Patch operation at index " +
                    opIndex +
                    " has '" +
                    pointerName +
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
        if (jsonPointer == null || jsonPointer.isEmpty()) {
            return "";
        }
        var withoutLeadingSlash = jsonPointer.startsWith("/") ? jsonPointer.substring(1) : jsonPointer;
        var slashIndex = withoutLeadingSlash.indexOf('/');
        return slashIndex < 0 ? withoutLeadingSlash : withoutLeadingSlash.substring(0, slashIndex);
    }

    private void validateField(String field) {
        if (BLOCKED_WITH_HINT.contains(field)) {
            throw new ApiPatchNotAllowedException(field, "use /_start or /_stop endpoints to change runtime state");
        }
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new ApiPatchNotAllowedException(field, "field is not patchable");
        }
    }

    private JsonNode applyPatch(PatchType patchType, JsonNode patchNode, JsonNode currentNode) {
        if (patchType == PatchType.JSON_PATCH) {
            return jsonPatchDomainService.applyJsonPatch(patchNode, currentNode);
        }
        return jsonPatchDomainService.applyMergePatch(patchNode, currentNode);
    }

    private Api applyPatchedValues(Api existingApi, JsonNode patchedNode, PatchType patchType, JsonNode rawPatchNode) {
        var httpV4 = requireHttpV4(existingApi);

        rejectExplicitNullOnNonNullable(patchType, rawPatchNode, FIELD_NAME);
        var name = textOrDefault(patchedNode, FIELD_NAME, existingApi.getName());
        var description = resolveNullableString(patchType, rawPatchNode, patchedNode, FIELD_DESCRIPTION, existingApi.getDescription());
        rejectExplicitNullOnNonNullable(patchType, rawPatchNode, FIELD_API_VERSION);
        var apiVersion = textOrDefault(patchedNode, FIELD_API_VERSION, existingApi.getVersion());

        rejectExplicitNullOnNonNullable(patchType, rawPatchNode, FIELD_VISIBILITY);
        var visibility = readEnum(patchedNode, FIELD_VISIBILITY, Api.Visibility.class, existingApi.getVisibility(), FIELD_VISIBILITY);
        rejectExplicitNullOnNonNullable(patchType, rawPatchNode, FIELD_LIFECYCLE_STATE);
        var lifecycleState = readEnum(
            patchedNode,
            FIELD_LIFECYCLE_STATE,
            Api.ApiLifecycleState.class,
            existingApi.getApiLifecycleState(),
            FIELD_LIFECYCLE_STATE
        );

        var labels = resolveNullableList(patchType, rawPatchNode, patchedNode, FIELD_LABELS, existingApi.getLabels());
        var categories = resolveNullableSet(patchType, rawPatchNode, patchedNode, FIELD_CATEGORIES, existingApi.getCategories());
        var tags = resolveNullableSet(patchType, rawPatchNode, patchedNode, FIELD_TAGS, httpV4.getTags());

        var patchableAnalytics = resolveNullableObject(
            patchType,
            rawPatchNode,
            patchedNode,
            FIELD_ANALYTICS,
            PatchableAnalytics.class,
            PatchableAnalytics.from(httpV4.getAnalytics())
        );
        var analytics = patchableAnalytics != null ? patchableAnalytics.toDomain() : null;
        var failover = resolveNullableObject(patchType, rawPatchNode, patchedNode, FIELD_FAILOVER, Failover.class, httpV4.getFailover());
        var patchableFlowExecution = resolveNullableObject(
            patchType,
            rawPatchNode,
            patchedNode,
            FIELD_FLOW_EXECUTION,
            PatchableFlowExecution.class,
            PatchableFlowExecution.from(httpV4.getFlowExecution())
        );
        var flowExecution = patchableFlowExecution != null ? patchableFlowExecution.toDomain() : null;
        var services = resolveNullableObject(patchType, rawPatchNode, patchedNode, FIELD_SERVICES, ApiServices.class, httpV4.getServices());

        rejectExplicitNullOnNonNullable(patchType, rawPatchNode, FIELD_ALLOWED_IN_API_PRODUCTS);
        var allowedInApiProducts = readBooleanObject(patchedNode, FIELD_ALLOWED_IN_API_PRODUCTS, httpV4.getAllowedInApiProducts());
        rejectExplicitNullOnNonNullable(patchType, rawPatchNode, FIELD_ALLOW_MULTI_JWT_OAUTH2_SUBSCRIPTIONS);
        var allowMultiJwt = readBoolean(
            patchedNode,
            FIELD_ALLOW_MULTI_JWT_OAUTH2_SUBSCRIPTIONS,
            existingApi.isAllowMultiJwtOauth2Subscriptions()
        );
        rejectExplicitNullOnNonNullable(patchType, rawPatchNode, FIELD_DISABLE_MEMBERSHIP_NOTIFICATIONS);
        var disableMembershipNotifications = readBoolean(
            patchedNode,
            FIELD_DISABLE_MEMBERSHIP_NOTIFICATIONS,
            existingApi.isDisableMembershipNotifications()
        );

        var patchableProperties = resolvePatchableList(
            patchType,
            rawPatchNode,
            patchedNode,
            FIELD_PROPERTIES,
            PatchableProperty.class,
            PatchableProperty.fromList(httpV4.getProperties())
        );
        var properties = encryptProperties(patchableProperties);
        var responseTemplates = resolveResponseTemplates(patchType, rawPatchNode, patchedNode, httpV4.getResponseTemplates());

        var apiV4Fields = resolveApiV4Fields(patchType, rawPatchNode, patchedNode, httpV4);

        var updatedDefinition = httpV4
            .toBuilder()
            .name(name)
            .apiVersion(apiVersion)
            .tags(tags)
            .analytics(analytics)
            .failover(failover)
            .flowExecution(flowExecution)
            .services(services)
            .allowedInApiProducts(allowedInApiProducts)
            .properties(properties)
            .responseTemplates(responseTemplates)
            .flows(apiV4Fields.flows())
            .resources(apiV4Fields.resources())
            .listeners(apiV4Fields.listeners())
            .endpointGroups(apiV4Fields.endpointGroups())
            .build();

        return existingApi
            .toBuilder()
            .name(name)
            .description(description)
            .version(apiVersion)
            .visibility(visibility)
            .apiLifecycleState(lifecycleState)
            .labels(labels)
            .categories(categories)
            .allowMultiJwtOauth2Subscriptions(allowMultiJwt)
            .disableMembershipNotifications(disableMembershipNotifications)
            .groups(resolveNullableSet(patchType, rawPatchNode, patchedNode, FIELD_GROUPS, existingApi.getGroups()))
            .apiDefinitionValue(updatedDefinition)
            .build();
    }

    private ApiV4Fields resolveApiV4Fields(
        PatchType patchType,
        JsonNode rawPatchNode,
        JsonNode patchedNode,
        io.gravitee.definition.model.v4.Api httpV4
    ) {
        ApiV4Fields deserialized;
        try {
            deserialized = apiV4Deserializer.fromPatchedNode(patchedNode);
        } catch (IOException e) {
            var field = extractFailedField(e);
            var message = field.isEmpty()
                ? "Invalid patch: " + e.getMessage()
                : "Invalid value for field '" + field + "': " + e.getMessage();
            throw new ValidationDomainException(message, Map.of("location", deserializationLocation(field, e)), "invalidValue");
        }

        var listeners = resolveField(patchType, rawPatchNode, patchedNode, FIELD_LISTENERS, deserialized::listeners, httpV4.getListeners());
        var endpointGroups = resolveField(
            patchType,
            rawPatchNode,
            patchedNode,
            FIELD_ENDPOINT_GROUPS,
            deserialized::endpointGroups,
            httpV4.getEndpointGroups()
        );
        var flows = resolveField(patchType, rawPatchNode, patchedNode, FIELD_FLOWS, deserialized::flows, httpV4.getFlows());
        var resources = resolveField(patchType, rawPatchNode, patchedNode, FIELD_RESOURCES, deserialized::resources, httpV4.getResources());

        return new ApiV4Fields(listeners, endpointGroups, flows, resources);
    }

    private <R> R resolveField(
        PatchType patchType,
        JsonNode rawPatchNode,
        JsonNode patchedNode,
        String field,
        Supplier<R> patched,
        R existing
    ) {
        if (patchType == PatchType.MERGE_PATCH && rawPatchNode.has(field)) {
            return rawPatchNode.get(field).isNull() ? null : patched.get();
        }
        if (patchType == PatchType.JSON_PATCH) {
            if (!isTargetedByJsonPatch(rawPatchNode, field)) {
                return existing;
            }
            if (patchedNode.get(field) == null && isNullifiedByJsonPatch(rawPatchNode, field)) {
                return null;
            }
            if (patchedNode.has(field)) {
                if (patchedNode.get(field).isNull() && isNullifiedByJsonPatch(rawPatchNode, field)) {
                    return null;
                }
                return patched.get();
            }
        }
        return existing;
    }

    private boolean isTargetedByJsonPatch(JsonNode rawPatchNode, String field) {
        if (!rawPatchNode.isArray()) {
            return false;
        }
        String fieldPrefix = JSON_PATCH_PATH_PREFIX + field;
        for (JsonNode op : rawPatchNode) {
            String path = op.path("path").asText();
            if (path.equals(fieldPrefix) || path.startsWith(fieldPrefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private static String extractFailedField(IOException e) {
        if (e instanceof JsonMappingException jme && jme.getPath() != null && !jme.getPath().isEmpty()) {
            var first = jme.getPath().getFirst();
            if (first.getFieldName() != null) {
                return first.getFieldName();
            }
        }
        return "";
    }

    private <T> List<T> resolvePatchableList(
        PatchType patchType,
        JsonNode rawPatchNode,
        JsonNode patchedNode,
        String field,
        Class<T> elementType,
        List<T> existing
    ) {
        return resolveField(
            patchType,
            rawPatchNode,
            patchedNode,
            field,
            () -> readList(patchedNode, field, elementType, existing),
            existing
        );
    }

    private Map<String, Map<String, ResponseTemplate>> resolveResponseTemplates(
        PatchType patchType,
        JsonNode rawPatchNode,
        JsonNode patchedNode,
        Map<String, Map<String, ResponseTemplate>> existing
    ) {
        return resolveField(
            patchType,
            rawPatchNode,
            patchedNode,
            FIELD_RESPONSE_TEMPLATES,
            () -> parseResponseTemplates(patchedNode),
            existing
        );
    }

    private Map<String, Map<String, ResponseTemplate>> parseResponseTemplates(JsonNode patchedNode) {
        try {
            var patchable = objectMapper.treeToValue(
                patchedNode.get(FIELD_RESPONSE_TEMPLATES),
                new TypeReference<Map<String, Map<String, PatchableResponseTemplate>>>() {}
            );
            return fromPatchableResponseTemplates(patchable);
        } catch (JsonProcessingException e) {
            throw new ValidationDomainException("Invalid value for field 'responseTemplates': " + e.getMessage());
        }
    }

    private static Map<String, Map<String, ResponseTemplate>> fromPatchableResponseTemplates(
        Map<String, Map<String, PatchableResponseTemplate>> patchable
    ) {
        if (patchable == null) {
            return null;
        }
        return patchable
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, e ->
                    (e.getValue() == null ? Map.<String, PatchableResponseTemplate>of() : e.getValue()).entrySet()
                        .stream()
                        .filter(inner -> inner.getValue() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, inner -> inner.getValue().toDomain()))
                )
            );
    }

    private boolean isExplicitNull(PatchType patchType, JsonNode rawPatchNode, String field) {
        return patchType == PatchType.MERGE_PATCH && rawPatchNode.has(field) && rawPatchNode.get(field).isNull();
    }

    private boolean isNullifiedByJsonPatch(JsonNode rawPatchNode, String field) {
        if (!rawPatchNode.isArray()) {
            return false;
        }
        String fieldPath = JSON_PATCH_PATH_PREFIX + field;
        for (JsonNode op : rawPatchNode) {
            if (!fieldPath.equals(op.path("path").asText())) {
                continue;
            }
            String opType = op.path("op").asText();
            if ("remove".equals(opType)) {
                return true;
            }
            if ("replace".equals(opType) || "add".equals(opType)) {
                JsonNode value = op.get("value");
                if (value != null && value.isNull()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void rejectExplicitNullOnNonNullable(PatchType patchType, JsonNode rawPatchNode, String field) {
        boolean nullified = switch (patchType) {
            case MERGE_PATCH -> isExplicitNull(patchType, rawPatchNode, field);
            case JSON_PATCH -> isNullifiedByJsonPatch(rawPatchNode, field);
        };
        if (nullified) {
            throw new ValidationDomainException(NULL_NOT_ALLOWED_MESSAGE_FORMAT.formatted(field));
        }
    }

    private boolean shouldNullify(PatchType patchType, JsonNode rawPatchNode, JsonNode patchedNode, String field) {
        if (isExplicitNull(patchType, rawPatchNode, field)) {
            return true;
        }
        var fieldNode = patchedNode.get(field);
        if (fieldNode != null && fieldNode.isNull()) {
            return true;
        }
        // fieldNode == null only when the patch removed the field; the replace-null branch of the scan cannot fire here
        return fieldNode == null && isNullifiedByJsonPatch(rawPatchNode, field);
    }

    private List<String> resolveNullableList(
        PatchType patchType,
        JsonNode rawPatchNode,
        JsonNode patchedNode,
        String field,
        List<String> existing
    ) {
        if (shouldNullify(patchType, rawPatchNode, patchedNode, field)) {
            return new ArrayList<>();
        }
        return readStringList(patchedNode, field, existing);
    }

    private Set<String> resolveNullableSet(
        PatchType patchType,
        JsonNode rawPatchNode,
        JsonNode patchedNode,
        String field,
        Set<String> existing
    ) {
        if (shouldNullify(patchType, rawPatchNode, patchedNode, field)) {
            return new HashSet<>();
        }
        return readStringSet(patchedNode, field, existing);
    }

    private <T> T resolveNullableObject(
        PatchType patchType,
        JsonNode rawPatchNode,
        JsonNode patchedNode,
        String field,
        Class<T> type,
        T existing
    ) {
        if (shouldNullify(patchType, rawPatchNode, patchedNode, field)) {
            return null;
        }
        return readObject(patchedNode, field, type, existing);
    }

    private String resolveNullableString(PatchType patchType, JsonNode rawPatchNode, JsonNode patchedNode, String field, String existing) {
        if (shouldNullify(patchType, rawPatchNode, patchedNode, field)) {
            return null;
        }
        return textOrDefault(patchedNode, field, existing);
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asText(defaultValue);
    }

    private <E extends Enum<E>> E readEnum(JsonNode node, String field, Class<E> type, E defaultValue, String errorLabel) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, fieldNode.asText());
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException("Invalid " + errorLabel + " value: " + fieldNode.asText());
        }
    }

    private List<String> readStringList(JsonNode node, String field, List<String> defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        try {
            return objectMapper.convertValue(fieldNode, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            throw new ValidationDomainException("Invalid value for field '" + field + "': " + e.getMessage());
        }
    }

    private Set<String> readStringSet(JsonNode node, String field, Set<String> defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        try {
            return objectMapper.convertValue(fieldNode, objectMapper.getTypeFactory().constructCollectionType(Set.class, String.class));
        } catch (Exception e) {
            throw new ValidationDomainException("Invalid value for field '" + field + "': " + e.getMessage());
        }
    }

    private <T> T readObject(JsonNode node, String field, Class<T> type, T defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null) {
            return defaultValue;
        }
        if (fieldNode.isNull()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(fieldNode, type);
        } catch (Exception e) {
            throw new ValidationDomainException("Invalid value for field '" + field + "': " + e.getMessage());
        }
    }

    private Boolean readBooleanObject(JsonNode node, String field, Boolean defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asBoolean();
    }

    private boolean readBoolean(JsonNode node, String field, boolean defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asBoolean();
    }

    private <T> List<T> readList(JsonNode node, String field, Class<T> elementType, List<T> defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        try {
            return objectMapper
                .readerFor(objectMapper.getTypeFactory().constructCollectionType(List.class, elementType))
                .readValue(fieldNode);
        } catch (Exception e) {
            throw new ValidationDomainException(
                "Invalid value for field '" + field + "': " + e.getMessage(),
                Map.of("location", deserializationLocation(field, e)),
                "invalidValue"
            );
        }
    }

    private String deserializationLocation(String field, Throwable e) {
        var cause = e instanceof IllegalArgumentException && e.getCause() != null ? e.getCause() : e;
        if (cause instanceof JsonMappingException jme && jme.getPath() != null && !jme.getPath().isEmpty()) {
            var refs = jme.getPath();
            var sb = new StringBuilder("/").append(field);
            int start = (!refs.isEmpty() && field.equals(refs.getFirst().getFieldName())) ? 1 : 0;
            for (int i = start; i < refs.size(); i++) {
                var ref = refs.get(i);
                if (ref.getFieldName() != null) {
                    sb.append("/").append(ref.getFieldName());
                } else if (ref.getIndex() >= 0) {
                    sb.append("/").append(ref.getIndex());
                }
            }
            return sb.toString();
        }
        return "/" + field;
    }

    private List<Property> encryptProperties(List<PatchableProperty> patchableProperties) {
        if (patchableProperties == null) {
            return null;
        }
        return propertyDomainService.encryptProperties(patchableProperties.stream().map(PatchableProperty::toEncryptable).toList());
    }

    record PatchableProperty(String key, String value, boolean encrypted, boolean dynamic, boolean encryptable) {
        static PatchableProperty from(Property p) {
            if (p == null) {
                return null;
            }
            return new PatchableProperty(p.getKey(), p.getValue(), p.isEncrypted(), p.isDynamic(), false);
        }

        static List<PatchableProperty> fromList(List<Property> properties) {
            if (properties == null) {
                return null;
            }
            return properties.stream().map(PatchableProperty::from).toList();
        }

        EncryptableProperty toEncryptable() {
            return EncryptableProperty.builder()
                .key(key)
                .value(value)
                .encrypted(encrypted)
                .dynamic(dynamic)
                .encryptable(encryptable)
                .build();
        }
    }

    public interface ApiV4Deserializer {
        JsonNode toCurrentStateNode(Api api);
        ApiV4Fields fromPatchedNode(JsonNode patchedNode) throws IOException;
    }

    public record ApiV4Fields(List<Listener> listeners, List<EndpointGroup> endpointGroups, List<Flow> flows, List<Resource> resources) {}

    public enum PatchType {
        JSON_PATCH,
        MERGE_PATCH,
    }

    @Builder
    public record Input(String apiId, PatchType patchType, String patchBody, boolean dryRun, AuditInfo auditInfo) {}

    public record Output(Api api, PrimaryOwnerEntity primaryOwner, Workflow.State workflowState) {}

    record PatchableFlowExecution(String mode, boolean matchRequired) {
        static PatchableFlowExecution from(FlowExecution domain) {
            if (domain == null) {
                return null;
            }
            return new PatchableFlowExecution(
                domain.getMode() != null ? domain.getMode().name() : FlowMode.DEFAULT.name(),
                domain.isMatchRequired()
            );
        }

        FlowExecution toDomain() {
            if (mode == null) {
                throw new ValidationDomainException("Invalid flowExecution.mode value: null");
            }
            FlowMode resolvedMode;
            try {
                resolvedMode = FlowMode.valueOf(mode);
            } catch (IllegalArgumentException e) {
                throw new ValidationDomainException("Invalid flowExecution.mode value: " + mode);
            }
            var execution = new FlowExecution();
            execution.setMode(resolvedMode);
            execution.setMatchRequired(matchRequired);
            return execution;
        }
    }

    record PatchableAnalytics(boolean enabled, Sampling sampling, Logging logging, Tracing tracing) {
        static PatchableAnalytics from(Analytics domain) {
            if (domain == null) {
                return null;
            }
            return new PatchableAnalytics(domain.isEnabled(), domain.getMessageSampling(), domain.getLogging(), domain.getTracing());
        }

        Analytics toDomain() {
            return Analytics.builder().enabled(enabled).messageSampling(sampling).logging(logging).tracing(tracing).build();
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PatchableView(
        String name,
        String description,
        String apiVersion,
        String visibility,
        List<String> labels,
        Set<String> tags,
        String lifecycleState,
        Set<String> categories,
        Set<String> groups,
        PatchableAnalytics analytics,
        Failover failover,
        PatchableFlowExecution flowExecution,
        ApiServices services,
        Boolean allowedInApiProducts,
        boolean allowMultiJwtOauth2Subscriptions,
        boolean disableMembershipNotifications,
        List<PatchableProperty> properties,
        Map<String, Map<String, PatchableResponseTemplate>> responseTemplates,
        List<Flow> flows,
        List<Resource> resources,
        List<Listener> listeners,
        List<EndpointGroup> endpointGroups
    ) {
        public static PatchableView from(Api api) {
            var httpV4 = requireHttpV4(api);
            return new PatchableView(
                api.getName(),
                api.getDescription(),
                api.getVersion(),
                api.getVisibility() != null ? api.getVisibility().name() : null,
                api.getLabels(),
                httpV4.getTags(),
                api.getApiLifecycleState() != null ? api.getApiLifecycleState().name() : null,
                api.getCategories(),
                api.getGroups(),
                PatchableAnalytics.from(httpV4.getAnalytics()),
                httpV4.getFailover(),
                PatchableFlowExecution.from(httpV4.getFlowExecution()),
                httpV4.getServices(),
                httpV4.getAllowedInApiProducts(),
                api.isAllowMultiJwtOauth2Subscriptions(),
                api.isDisableMembershipNotifications(),
                PatchableProperty.fromList(httpV4.getProperties()),
                toPatchableResponseTemplates(httpV4.getResponseTemplates()),
                httpV4.getFlows(),
                httpV4.getResources(),
                httpV4.getListeners(),
                httpV4.getEndpointGroups()
            );
        }

        private static Map<String, Map<String, PatchableResponseTemplate>> toPatchableResponseTemplates(
            Map<String, Map<String, ResponseTemplate>> templates
        ) {
            if (templates == null) {
                return null;
            }
            return templates
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(Map.Entry::getKey, e ->
                        (e.getValue() == null ? Map.<String, ResponseTemplate>of() : e.getValue()).entrySet()
                            .stream()
                            .filter(inner -> inner.getValue() != null)
                            .collect(Collectors.toMap(Map.Entry::getKey, inner -> PatchableResponseTemplate.from(inner.getValue())))
                    )
                );
        }
    }

    record PatchableResponseTemplate(Integer statusCode, Map<String, String> headers, String body, Boolean propagateErrorKeyToLogs) {
        static PatchableResponseTemplate from(ResponseTemplate domain) {
            if (domain == null) {
                return null;
            }
            return new PatchableResponseTemplate(
                domain.getStatusCode(),
                domain.getHeaders(),
                domain.getBody(),
                domain.isPropagateErrorKeyToLogs()
            );
        }

        ResponseTemplate toDomain() {
            return ResponseTemplate.builder()
                .statusCode(statusCode != null ? statusCode : 0)
                .headers(headers)
                .body(body)
                .propagateErrorKeyToLogs(propagateErrorKeyToLogs != null && propagateErrorKeyToLogs)
                .build();
        }
    }
}
