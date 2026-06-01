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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.gravitee.gateway.services.sync.process.common.model.Deployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Builder
@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public class AuthzEntityReactorDeployable implements Deployable {

    private String entityId;
    private String engineUid;
    private Kind kind;
    /** Engine type name (e.g. "User", "Doc"). Optional — null on legacy publishers, in which
     *  case the kind-default is used to build the engine UID. */
    private String entityType;
    private Map<String, Object> attributes;
    private List<String> parents;
    private SyncAction syncAction;

    @Override
    public String id() {
        return entityId;
    }

    public enum Kind {
        RESOURCE,
        PRINCIPAL,
    }
}
