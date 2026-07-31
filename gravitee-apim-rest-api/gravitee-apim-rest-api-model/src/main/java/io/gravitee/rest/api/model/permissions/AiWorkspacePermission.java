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
package io.gravitee.rest.api.model.permissions;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An AI Workspace is stored as an API Product, but is granted independently: holding an API Product
 * permission must not imply access to a workspace. {@code PLAN} keeps the platform-wide name even
 * though the workspace UI calls it a tier, so a role matrix reads consistently across scopes.
 */
@Schema(enumAsRef = true)
public enum AiWorkspacePermission implements Permission {
    DEFINITION("DEFINITION", 1000),
    PLAN("PLAN", 1100),
    USER("USER", 1200),
    MEMBER("MEMBER", 1300);

    final String name;
    final int mask;

    AiWorkspacePermission(String name, int mask) {
        this.name = name;
        this.mask = mask;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMask() {
        return mask;
    }
}
