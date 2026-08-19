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
package io.gravitee.apim.plugin.gamma.api.automation;

import io.gravitee.rest.api.model.permissions.RolePermission;
import java.util.Objects;

/**
 * A kind of resource a module exposes through the Automation API.
 *
 * @param path the collection path under the module mount, e.g. {@code catalog/mcp-servers}
 * @param permission the permission the Automation API enforces before calling the module; must be
 *     environment-scoped, since automation resources are addressed per environment
 */
public record ResourceKind(String path, RolePermission permission) {
    public ResourceKind {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(permission, "permission");
    }
}
