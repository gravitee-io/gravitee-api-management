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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiPatchDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiPatchNotAllowedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.apim.core.workflow.query_service.WorkflowQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.ApiServices;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class PatchApiUseCase {

    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_API_VERSION = "apiVersion";
    private static final String FIELD_VISIBILITY = "visibility";
    private static final String FIELD_LIFECYCLE_STATE = "lifecycleState";
    private static final String FIELD_ALLOWED_IN_API_PRODUCTS = "allowedInApiProducts";
    private static final String FIELD_ALLOW_MULTI_JWT_OAUTH2_SUBSCRIPTIONS = "allowMultiJwtOauth2Subscriptions";
    private static final String FIELD_DISABLE_MEMBERSHIP_NOTIFICATIONS = "disableMembershipNotifications";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_RESPONSE_TEMPLATES = "responseTemplates";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "name",
        FIELD_DESCRIPTION,
        FIELD_API_VERSION,
        FIELD_VISIBILITY,
        "labels",
        "tags",
        FIELD_LIFECYCLE_STATE,
        "categories",
        "analytics",
        "failover",
        "flowExecution",
        "services",
        FIELD_ALLOWED_IN_API_PRODUCTS,
        FIELD_ALLOW_MULTI_JWT_OAUTH2_SUBSCRIPTIONS,
        FIELD_DISABLE_MEMBERSHIP_NOTIFICATIONS,
        FIELD_PROPERTIES,
        FIELD_RESPONSE_TEMPLATES
    );

    private static final Set<String> BLOCKED_WITH_HINT = Set.of("state");
    private static final Set<String> BLOCKED_WITHOUT_HINT = Set.of("flows", "endpointGroups", "listeners", "resources");

    private final ApiCrudService apiCrudService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final UpdateApiDomainService updateApiDomainService;
    private final ApiPatchDomainService apiPatchDomainService;
    private final WorkflowQueryService workflowQueryService;
    private final ObjectMapper objectMapper;

    public Output execute(Input input) {
        var existingApi = apiCrudService.get(input.apiId());

        if (!DefinitionVersion.V4.equals(existingApi.getDefinitionVersion())) {
            throw new ApiInvalidDefinitionVersionException(input.apiId());
        }
        if (!ApiType.PROXY.equals(existingApi.getType())) {
            throw new ApiInvalidTypeException(input.apiId(), ApiType.PROXY);
        }

        var currentNode = toJsonNode(existingApi);
        var patchNode = parseBody(input.patchBody());

        enforceAllowList(patchNode);

        var patchedNode = apiPatchDomainService.applyMergePatch(patchNode, currentNode);

        var primaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(input.auditInfo().organizationId(), input.apiId());

        var workflows = workflowQueryService.findAllByApiIdAndType(input.apiId(), Workflow.Type.REVIEW);
        var workflowState = workflows.isEmpty() ? null : workflows.get(0).getState();

        var updatedApi = applyPatchedValues(existingApi, patchedNode, patchNode);

        if (input.dryRun()) {
            var validated = updateApiDomainService.validateV4(updatedApi, input.auditInfo());
            return new Output(validated, primaryOwner, workflowState);
        }

        var persisted = updateApiDomainService.updateV4(updatedApi, input.auditInfo());
        return new Output(persisted, primaryOwner, workflowState);
    }

    private JsonNode toJsonNode(Api api) {
        var view = buildPatchableView(api);
        return objectMapper.valueToTree(view);
    }

    private PatchableView buildPatchableView(Api api) {
        if (!(api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api httpV4)) {
            throw new IllegalStateException("API definition is not an HTTP v4 definition");
        }
        return new PatchableView(
            api.getName(),
            api.getDescription(),
            api.getVersion(),
            api.getVisibility() != null ? api.getVisibility().name() : null,
            api.getLabels(),
            httpV4.getTags(),
            api.getApiLifecycleState() != null ? api.getApiLifecycleState().name() : null,
            api.getCategories(),
            httpV4.getAnalytics(),
            httpV4.getFailover(),
            httpV4.getFlowExecution(),
            httpV4.getServices(),
            httpV4.getAllowedInApiProducts(),
            api.isAllowMultiJwtOauth2Subscriptions(),
            api.isDisableMembershipNotifications(),
            httpV4.getProperties(),
            httpV4.getResponseTemplates()
        );
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

    private void enforceAllowList(JsonNode patchNode) {
        if (!patchNode.isObject()) {
            throw new ValidationDomainException("JSON Merge Patch body must be a JSON object");
        }
        var fieldNames = patchNode.fieldNames();
        while (fieldNames.hasNext()) {
            validateField(fieldNames.next());
        }
    }

    private void validateField(String field) {
        if (BLOCKED_WITH_HINT.contains(field)) {
            throw new ApiPatchNotAllowedException(field, "use /_start or /_stop endpoints to change runtime state");
        }
        if (BLOCKED_WITHOUT_HINT.contains(field)) {
            throw new ApiPatchNotAllowedException(field, "use PUT to update structural fields");
        }
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new ApiPatchNotAllowedException(field, "field is not patchable");
        }
    }

    private Api applyPatchedValues(Api existingApi, JsonNode patchedNode, JsonNode rawPatchNode) {
        if (!(existingApi.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api httpV4)) {
            throw new IllegalStateException("API definition is not an HTTP v4 definition");
        }

        rejectExplicitNullOnNonNullable(rawPatchNode, "name");
        var name = textOrDefault(patchedNode, "name", existingApi.getName());
        rejectExplicitNullOnNonNullable(rawPatchNode, FIELD_DESCRIPTION);
        var description = textOrDefault(patchedNode, FIELD_DESCRIPTION, existingApi.getDescription());
        rejectExplicitNullOnNonNullable(rawPatchNode, FIELD_API_VERSION);
        var apiVersion = textOrDefault(patchedNode, FIELD_API_VERSION, existingApi.getVersion());

        rejectExplicitNullOnNonNullable(rawPatchNode, FIELD_VISIBILITY);
        var visibility = readVisibility(patchedNode, existingApi.getVisibility());
        rejectExplicitNullOnNonNullable(rawPatchNode, FIELD_LIFECYCLE_STATE);
        var lifecycleState = readLifecycleState(patchedNode, existingApi.getApiLifecycleState());

        var labels = resolveNullableList(rawPatchNode, patchedNode, "labels", existingApi.getLabels());
        var categories = resolveNullableSet(rawPatchNode, patchedNode, "categories", existingApi.getCategories());
        var tags = resolveNullableSet(rawPatchNode, patchedNode, "tags", httpV4.getTags());

        var analytics = resolveNullableObject(rawPatchNode, patchedNode, "analytics", Analytics.class, httpV4.getAnalytics());
        var failover = resolveNullableObject(rawPatchNode, patchedNode, "failover", Failover.class, httpV4.getFailover());
        var flowExecution = resolveNullableObject(
            rawPatchNode,
            patchedNode,
            "flowExecution",
            FlowExecution.class,
            httpV4.getFlowExecution()
        );
        var services = resolveNullableObject(rawPatchNode, patchedNode, "services", ApiServices.class, httpV4.getServices());

        rejectExplicitNullOnNonNullable(rawPatchNode, FIELD_ALLOWED_IN_API_PRODUCTS);
        var allowedInApiProducts = readBooleanObject(patchedNode, FIELD_ALLOWED_IN_API_PRODUCTS, httpV4.getAllowedInApiProducts());
        rejectExplicitNullOnNonNullable(rawPatchNode, FIELD_ALLOW_MULTI_JWT_OAUTH2_SUBSCRIPTIONS);
        var allowMultiJwt = readBoolean(
            patchedNode,
            FIELD_ALLOW_MULTI_JWT_OAUTH2_SUBSCRIPTIONS,
            existingApi.isAllowMultiJwtOauth2Subscriptions()
        );
        rejectExplicitNullOnNonNullable(rawPatchNode, FIELD_DISABLE_MEMBERSHIP_NOTIFICATIONS);
        var disableMembershipNotifications = readBoolean(
            patchedNode,
            FIELD_DISABLE_MEMBERSHIP_NOTIFICATIONS,
            existingApi.isDisableMembershipNotifications()
        );

        var properties = httpV4.getProperties();
        if (rawPatchNode.has(FIELD_PROPERTIES)) {
            if (rawPatchNode.get(FIELD_PROPERTIES).isNull()) {
                properties = List.of();
            } else {
                properties = readList(patchedNode, FIELD_PROPERTIES, Property.class, null);
            }
        }

        var responseTemplates = httpV4.getResponseTemplates();
        if (rawPatchNode.has(FIELD_RESPONSE_TEMPLATES)) {
            if (rawPatchNode.get(FIELD_RESPONSE_TEMPLATES).isNull()) {
                responseTemplates = null;
            } else {
                try {
                    responseTemplates = objectMapper.treeToValue(patchedNode.get(FIELD_RESPONSE_TEMPLATES), new TypeReference<>() {});
                } catch (Exception e) {
                    throw new ValidationDomainException("Invalid value for field 'responseTemplates': " + e.getMessage());
                }
            }
        }

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
            .groups(existingApi.getGroups())
            .apiDefinitionValue(updatedDefinition)
            .build();
    }

    private boolean isExplicitNull(JsonNode rawPatchNode, String field) {
        return rawPatchNode.has(field) && rawPatchNode.get(field).isNull();
    }

    private void rejectExplicitNullOnNonNullable(JsonNode rawPatchNode, String field) {
        if (isExplicitNull(rawPatchNode, field)) {
            throw new ValidationDomainException(
                "'" + field + "' cannot be null; omit the field to leave it unchanged, or send an explicit value"
            );
        }
    }

    private List<String> resolveNullableList(JsonNode rawPatchNode, JsonNode patchedNode, String field, List<String> existing) {
        if (isExplicitNull(rawPatchNode, field)) {
            return new ArrayList<>();
        }
        return readStringList(patchedNode, field, existing);
    }

    private Set<String> resolveNullableSet(JsonNode rawPatchNode, JsonNode patchedNode, String field, Set<String> existing) {
        if (isExplicitNull(rawPatchNode, field)) {
            return new HashSet<>();
        }
        return readStringSet(patchedNode, field, existing);
    }

    private <T> T resolveNullableObject(JsonNode rawPatchNode, JsonNode patchedNode, String field, Class<T> type, T existing) {
        if (isExplicitNull(rawPatchNode, field)) {
            return null;
        }
        return readObject(patchedNode, field, type, existing);
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        var fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asText(defaultValue);
    }

    private Api.Visibility readVisibility(JsonNode node, Api.Visibility defaultValue) {
        var fieldNode = node.get(FIELD_VISIBILITY);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        try {
            return Api.Visibility.valueOf(fieldNode.asText());
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException("Invalid visibility value: " + fieldNode.asText());
        }
    }

    private Api.ApiLifecycleState readLifecycleState(JsonNode node, Api.ApiLifecycleState defaultValue) {
        var fieldNode = node.get(FIELD_LIFECYCLE_STATE);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        try {
            return Api.ApiLifecycleState.valueOf(fieldNode.asText());
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException("Invalid lifecycleState value: " + fieldNode.asText());
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
            return objectMapper.convertValue(fieldNode, objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            throw new ValidationDomainException("Invalid value for field '" + field + "': " + e.getMessage());
        }
    }

    public enum PatchType {
        MERGE_PATCH,
    }

    @Builder
    public record Input(String apiId, PatchType patchType, String patchBody, boolean dryRun, AuditInfo auditInfo) {}

    public record Output(Api api, PrimaryOwnerEntity primaryOwner, Workflow.State workflowState) {}

    record PatchableView(
        String name,
        String description,
        String apiVersion,
        String visibility,
        List<String> labels,
        Set<String> tags,
        String lifecycleState,
        Set<String> categories,
        Analytics analytics,
        Failover failover,
        FlowExecution flowExecution,
        ApiServices services,
        Boolean allowedInApiProducts,
        boolean allowMultiJwtOauth2Subscriptions,
        boolean disableMembershipNotifications,
        List<Property> properties,
        Map<String, Map<String, ResponseTemplate>> responseTemplates
    ) {}
}
