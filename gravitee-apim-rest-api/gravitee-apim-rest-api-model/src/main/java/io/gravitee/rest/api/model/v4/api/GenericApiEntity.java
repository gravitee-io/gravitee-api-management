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
package io.gravitee.rest.api.model.v4.api;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.search.Indexable;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GenericApiEntity extends Serializable, Indexable {
    String getName();

    String getDescription();

    String getApiVersion();

    DefinitionVersion getDefinitionVersion();

    Date getDeployedAt();

    Date getCreatedAt();

    Date getUpdatedAt();

    boolean isDisableMembershipNotifications();

    Map<String, Object> getMetadata();

    void setMetadata(Map<String, Object> metadata);

    Set<String> getGroups();

    Lifecycle.State getState();

    Visibility getVisibility();

    List<String> getLabels();

    ApiLifecycleState getLifecycleState();

    Set<String> getTags();

    PrimaryOwnerEntity getPrimaryOwner();

    Set<String> getCategories();

    OriginContext getOriginContext();

    WorkflowState getWorkflowState();

    String getPicture();

    String getBackground();
}
