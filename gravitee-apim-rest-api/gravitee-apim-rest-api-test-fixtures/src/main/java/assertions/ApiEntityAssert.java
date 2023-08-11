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
package assertions;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.AbstractObjectAssert;

public class ApiEntityAssert extends AbstractObjectAssert<ApiEntityAssert, ApiEntity> {

    public ApiEntityAssert(ApiEntity apiEntity) {
        super(apiEntity, ApiEntityAssert.class);
    }

    public ApiEntityAssert hasId(String id) {
        isNotNull();
        if (!actual.getId().equals(id)) {
            failWithMessage("Expected api id to be <%s> but was <%s>", id, actual.getId());
        }
        return this;
    }

    public ApiEntityAssert hasNoCrossId() {
        isNotNull();
        if (actual.getCrossId() != null) {
            failWithMessage("Expected api cross id to be null but was <%s>", actual.getCrossId());
        }
        return this;
    }

    public ApiEntityAssert hasCrossId(String crossId) {
        isNotNull();
        if (!actual.getCrossId().equals(crossId)) {
            failWithMessage("Expected api cross id to be <%s> but was <%s>", crossId, actual.getCrossId());
        }
        return this;
    }

    public ApiEntityAssert hasApiVersion(String apiVersion) {
        isNotNull();
        if (!actual.getApiVersion().equals(apiVersion)) {
            failWithMessage("Expected api version to be <%s> but was <%s>", apiVersion, actual.getApiVersion());
        }
        return this;
    }

    public ApiEntityAssert hasDefinitionVersion(DefinitionVersion definitionVersion) {
        isNotNull();
        if (!actual.getDefinitionVersion().equals(definitionVersion)) {
            failWithMessage("Expected api definition version to be <%s> but was <%s>", definitionVersion, actual.getDefinitionVersion());
        }
        return this;
    }

    public ApiEntityAssert hasName(String name) {
        isNotNull();
        if (!actual.getName().equals(name)) {
            failWithMessage("Expected api name to be <%s> but was <%s>", name, actual.getName());
        }
        return this;
    }

    public ApiEntityAssert hasType(ApiType type) {
        isNotNull();
        if (!actual.getType().equals(type)) {
            failWithMessage("Expected api type to be <%s> but was <%s>", type, actual.getType());
        }
        return this;
    }

    public ApiEntityAssert hasDescription(String description) {
        isNotNull();
        if (!actual.getDescription().equals(description)) {
            failWithMessage("Expected api description to be <%s> but was <%s>", description, actual.getDescription());
        }
        return this;
    }

    public ApiEntityAssert hasOnlyTags(Set<String> tags) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have tags:  <%s> but was: <%s>";

        if (!actual.getTags().containsAll(tags)) {
            failWithMessage(assertjErrorMessage, tags, actual.getTags());
        }

        return this;
    }

    public ApiEntityAssert hasOnlyEndpointGroups(List<EndpointGroup> endpointGroups) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have endpoint groups:  <%s> but was: <%s>";

        if (!actual.getEndpointGroups().equals(endpointGroups)) {
            failWithMessage(assertjErrorMessage, endpointGroups, actual.getEndpointGroups());
        }

        return this;
    }

    public ApiEntityAssert hasAnalytics(Analytics analytics) {
        isNotNull();
        if (!actual.getAnalytics().equals(analytics)) {
            failWithMessage("Expected api analytics to be <%s> but was <%s>", analytics, actual.getAnalytics());
        }
        return this;
    }

    public ApiEntityAssert hasOnlyProperties(List<Property> properties) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have properties:  <%s> but was: <%s>";

        if (!actual.getProperties().equals(properties)) {
            failWithMessage(assertjErrorMessage, properties, actual.getProperties());
        }

        return this;
    }

    public ApiEntityAssert hasOnlyResources(List<Resource> resources) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have resources:  <%s> but was: <%s>";

        if (!actual.getResources().equals(resources)) {
            failWithMessage(assertjErrorMessage, resources, actual.getResources());
        }

        return this;
    }

    public ApiEntityAssert hasNoPlan() {
        isNotNull();
        if (actual.getPlans() != null && !actual.getPlans().isEmpty()) {
            failWithMessage("Expected api to have no plan but was <%s>", actual.getPlans());
        }
        return this;
    }

    public ApiEntityAssert hasOnlyPlans(Set<PlanEntity> plans) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have plans:  <%s> but was: <%s>";

        if (!actual.getPlans().equals(plans)) {
            failWithMessage(assertjErrorMessage, plans, actual.getPlans());
        }

        return this;
    }

    public ApiEntityAssert hasFlowExecution(FlowExecution flowExecution) {
        isNotNull();
        if (!actual.getFlowExecution().equals(flowExecution)) {
            failWithMessage("Expected api flow execution to be <%s> but was <%s>", flowExecution, actual.getFlowExecution());
        }
        return this;
    }

    public ApiEntityAssert hasOnlyFlows(List<Flow> flows) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have flows:  <%s> but was: <%s>";

        if (!actual.getFlows().equals(flows)) {
            failWithMessage(assertjErrorMessage, flows, actual.getFlows());
        }

        return this;
    }

    public ApiEntityAssert hasOnlyResponseTemplatesKeys(Set<String> responseTemplateKeys) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have responseTemplate:  <%s> but was: <%s>";

        if (!actual.getResponseTemplates().keySet().equals(responseTemplateKeys)) {
            failWithMessage(assertjErrorMessage, responseTemplateKeys, actual.getResponseTemplates().keySet());
        }

        return this;
    }

    public ApiEntityAssert hasServices(ApiServices services) {
        isNotNull();
        if (!actual.getServices().equals(services)) {
            failWithMessage("Expected api services to be <%s> but was <%s>", services, actual.getServices());
        }
        return this;
    }

    public ApiEntityAssert hasOnlyGroups(Set<String> groups) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have groups:  <%s> but was: <%s>";

        if (!actual.getGroups().equals(groups)) {
            failWithMessage(assertjErrorMessage, groups, actual.getGroups());
        }

        return this;
    }

    public ApiEntityAssert hasNoGroup() {
        isNotNull();
        if (actual.getGroups() != null && !actual.getGroups().isEmpty()) {
            failWithMessage("Expected api to have no group but was <%s>", actual.getGroups());
        }
        return this;
    }

    public ApiEntityAssert hasVisibility(Visibility visibility) {
        isNotNull();
        if (!actual.getVisibility().equals(visibility)) {
            failWithMessage("Expected api visibility to be <%s> but was <%s>", visibility, actual.getVisibility());
        }
        return this;
    }

    public ApiEntityAssert hasState(Lifecycle.State state) {
        isNotNull();
        if (!actual.getState().equals(state)) {
            failWithMessage("Expected api state to be <%s> but was <%s>", state, actual.getState());
        }
        return this;
    }

    public ApiEntityAssert hasPrimaryOwner(PrimaryOwnerEntity primaryOwnerEntity) {
        isNotNull();
        if (!actual.getPrimaryOwner().equals(primaryOwnerEntity)) {
            failWithMessage("Expected api primary owner to be <%s> but was <%s>", primaryOwnerEntity, actual.getPrimaryOwner());
        }
        return this;
    }

    public ApiEntityAssert hasPicture(String picture) {
        isNotNull();
        if (!actual.getPicture().equals(picture)) {
            failWithMessage("Expected api picture to be <%s> but was <%s>", picture, actual.getPicture());
        }
        return this;
    }

    public ApiEntityAssert hasPictureUrl(String pictureUrl) {
        isNotNull();
        if (!actual.getPictureUrl().equals(pictureUrl)) {
            failWithMessage("Expected api picture url to be <%s> but was <%s>", pictureUrl, actual.getPictureUrl());
        }
        return this;
    }

    public ApiEntityAssert hasOnlyCategories(Set<String> categories) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have categories:  <%s> but was: <%s>";

        if (!actual.getCategories().equals(categories)) {
            failWithMessage(assertjErrorMessage, categories, actual.getCategories());
        }

        return this;
    }

    public ApiEntityAssert hasOnlyLabels(List<String> labels) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have labels:  <%s> but was: <%s>";

        if (!actual.getLabels().equals(labels)) {
            failWithMessage(assertjErrorMessage, labels, actual.getLabels());
        }

        return this;
    }

    public ApiEntityAssert hasDefinitionContext(DefinitionContext definitionContext) {
        isNotNull();
        if (!actual.getDefinitionContext().equals(definitionContext)) {
            failWithMessage("Expected api definition context to be <%s> but was <%s>", definitionContext, actual.getDefinitionContext());
        }
        return this;
    }

    public ApiEntityAssert hasOnlyMetadataKeys(Set<String> metadataKeys) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have metadata:  <%s> but was: <%s>";

        if (!actual.getMetadata().keySet().equals(metadataKeys)) {
            failWithMessage(assertjErrorMessage, metadataKeys, actual.getMetadata().keySet());
        }

        return this;
    }

    public ApiEntityAssert hasLifecycleState(ApiLifecycleState state) {
        isNotNull();
        if (!actual.getLifecycleState().equals(state)) {
            failWithMessage("Expected api lifecycle state to be <%s> but was <%s>", state, actual.getLifecycleState());
        }
        return this;
    }

    public ApiEntityAssert hasWorkflowState(WorkflowState state) {
        isNotNull();
        if (!actual.getWorkflowState().equals(state)) {
            failWithMessage("Expected api workflow state to be <%s> but was <%s>", state, actual.getWorkflowState());
        }
        return this;
    }

    public ApiEntityAssert hasDisableMembershipNotifications(boolean disableMembershipNotifications) {
        isNotNull();
        if (!actual.isDisableMembershipNotifications() == disableMembershipNotifications) {
            failWithMessage(
                "Expected api disable membership notifications to be <%s> but was <%s>",
                disableMembershipNotifications,
                actual.isDisableMembershipNotifications()
            );
        }
        return this;
    }

    public ApiEntityAssert hasBackground(String background) {
        isNotNull();
        if (!actual.getBackground().equals(background)) {
            failWithMessage("Expected api background to be <%s> but was <%s>", background, actual.getBackground());
        }
        return this;
    }

    public ApiEntityAssert hasBackgroundUrl(String backgroundUrl) {
        isNotNull();
        if (!actual.getBackgroundUrl().equals(backgroundUrl)) {
            failWithMessage("Expected api background url to be <%s> but was <%s>", backgroundUrl, actual.getBackgroundUrl());
        }
        return this;
    }
}
