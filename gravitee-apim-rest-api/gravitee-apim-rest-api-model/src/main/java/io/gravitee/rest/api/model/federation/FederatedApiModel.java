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
package io.gravitee.rest.api.model.federation;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class FederatedApiModel implements GenericApiModel {

    private String id;
    private String name;
    private String apiVersion;
    private DefinitionVersion definitionVersion;
    private Date deployedAt;
    private Date createdAt;
    private Date updatedAt;
    private String description;
    private Set<String> tags = new HashSet<>();
    private Set<String> groups;
    private Visibility visibility;
    private Lifecycle.State state;
    private PrimaryOwnerEntity primaryOwner;
    private String picture;
    private Set<String> categories;
    private Map<String, String> metadata = new HashMap<>();
    private ApiLifecycleState lifecycleState;
    private boolean disableMembershipNotifications;
    private String accessPoint;

    @Override
    public String getVersion() {
        return apiVersion;
    }
}
