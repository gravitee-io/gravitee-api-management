/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.builder;

import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositoryApiBuilder {

    private final Api api = new Api();

    public RepositoryApiBuilder id(String id) {
        this.api.setId(id);
        return this;
    }

    public RepositoryApiBuilder updatedAt(Date updatedAt) {
        this.api.setUpdatedAt(updatedAt);
        this.api.setDeployedAt(updatedAt);
        return this;
    }

    public RepositoryApiBuilder definition(String definition) {
        this.api.setDefinition(definition);
        return this;
    }

    public RepositoryApiBuilder lifecycleState(LifecycleState lifecycleState) {
        this.api.setLifecycleState(lifecycleState);
        return this;
    }

    public RepositoryApiBuilder environment(String environmentId) {
        this.api.setEnvironmentId(environmentId);
        return this;
    }

    public Api build() {
        return this.api;
    }
}
