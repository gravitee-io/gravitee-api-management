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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.AbstractObjectAssert;

public class ApiAssert extends AbstractObjectAssert<ApiAssert, Api> {

    public ApiAssert(Api api) {
        super(api, ApiAssert.class);
    }

    public ApiAssert hasId(String id) {
        isNotNull();
        if (!actual.getId().equals(id)) {
            failWithMessage("Expected api id to be <%s> but was <%s>", id, actual.getId());
        }
        return this;
    }

    public ApiAssert hasNoCrossId() {
        isNotNull();
        if (actual.getCrossId() != null) {
            failWithMessage("Expected api cross id to be null but was <%s>", actual.getCrossId());
        }
        return this;
    }

    public ApiAssert hasCrossId(String crossId) {
        isNotNull();
        if (!actual.getCrossId().equals(crossId)) {
            failWithMessage("Expected api cross id to be <%s> but was <%s>", crossId, actual.getCrossId());
        }
        return this;
    }

    public ApiAssert hasVersion(String apiVersion) {
        isNotNull();
        if (!actual.getVersion().equals(apiVersion)) {
            failWithMessage("Expected api version to be <%s> but was <%s>", apiVersion, actual.getVersion());
        }
        return this;
    }

    public ApiAssert hasDefinitionVersion(DefinitionVersion definitionVersion) {
        isNotNull();
        if (!actual.getDefinitionVersion().equals(definitionVersion)) {
            failWithMessage("Expected api definition version to be <%s> but was <%s>", definitionVersion, actual.getDefinitionVersion());
        }
        return this;
    }

    public ApiAssert hasName(String name) {
        isNotNull();
        if (!actual.getName().equals(name)) {
            failWithMessage("Expected api name to be <%s> but was <%s>", name, actual.getName());
        }
        return this;
    }

    public ApiAssert hasType(ApiType type) {
        isNotNull();
        if (!actual.getType().equals(type)) {
            failWithMessage("Expected api type to be <%s> but was <%s>", type, actual.getType());
        }
        return this;
    }

    public ApiAssert hasDescription(String description) {
        isNotNull();
        if (!actual.getDescription().equals(description)) {
            failWithMessage("Expected api description to be <%s> but was <%s>", description, actual.getDescription());
        }
        return this;
    }

    public ApiAssert hasOnlyTags(Set<String> tags) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have tags:  <%s> but was: <%s>";

        if (!actual.getTags().containsAll(tags)) {
            failWithMessage(assertjErrorMessage, tags, actual.getTags());
        }

        return this;
    }

    public ApiAssert hasOnlyGroups(Set<String> groups) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have groups:  <%s> but was: <%s>";

        if (!actual.getGroups().equals(groups)) {
            failWithMessage(assertjErrorMessage, groups, actual.getGroups());
        }

        return this;
    }

    public ApiAssert hasNoGroup() {
        isNotNull();
        if (actual.getGroups() != null && !actual.getGroups().isEmpty()) {
            failWithMessage("Expected api to have no group but was <%s>", actual.getGroups());
        }
        return this;
    }

    public ApiAssert hasVisibility(Api.Visibility visibility) {
        isNotNull();
        if (!actual.getVisibility().equals(visibility)) {
            failWithMessage("Expected api visibility to be <%s> but was <%s>", visibility, actual.getVisibility());
        }
        return this;
    }

    public ApiAssert hasPicture(String picture) {
        isNotNull();
        if (!actual.getPicture().equals(picture)) {
            failWithMessage("Expected api picture to be <%s> but was <%s>", picture, actual.getPicture());
        }
        return this;
    }

    public ApiAssert hasOnlyCategories(Set<String> categories) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have categories:  <%s> but was: <%s>";

        if (!actual.getCategories().equals(categories)) {
            failWithMessage(assertjErrorMessage, categories, actual.getCategories());
        }

        return this;
    }

    public ApiAssert hasOnlyLabels(List<String> labels) {
        isNotNull();
        var assertjErrorMessage = "Expecting api to have labels:  <%s> but was: <%s>";

        if (!actual.getLabels().equals(labels)) {
            failWithMessage(assertjErrorMessage, labels, actual.getLabels());
        }

        return this;
    }

    public ApiAssert hasDefinitionContext(DefinitionContext definitionContext) {
        isNotNull();
        if (!actual.getDefinitionContext().equals(definitionContext)) {
            failWithMessage("Expected api definition context to be <%s> but was <%s>", definitionContext, actual.getDefinitionContext());
        }
        return this;
    }

    public ApiAssert hasState(Api.LifecycleState state) {
        isNotNull();
        if (!actual.getLifecycleState().equals(state)) {
            failWithMessage("Expected api state to be <%s> but was <%s>", state, actual.getLifecycleState());
        }
        return this;
    }

    public ApiAssert hasApiLifecycleState(Api.ApiLifecycleState state) {
        isNotNull();
        if (!actual.getApiLifecycleState().equals(state)) {
            failWithMessage("Expected api lifecycle state to be <%s> but was <%s>", state, actual.getLifecycleState());
        }
        return this;
    }

    public ApiAssert hasDisableMembershipNotifications(boolean disableMembershipNotifications) {
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

    public ApiAssert hasBackground(String background) {
        isNotNull();
        if (!actual.getBackground().equals(background)) {
            failWithMessage("Expected api background to be <%s> but was <%s>", background, actual.getBackground());
        }
        return this;
    }
}
