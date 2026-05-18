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
package io.gravitee.gamma.authorization.api;

import jakarta.validation.constraints.NotBlank;

public record AuthzCallerContext(@NotBlank String organizationId, @NotBlank String environmentId, @NotBlank String userId) {
    public static final String SYSTEM_USER = "system";

    public AuthzCallerContext {
        Validators.validateCtor(AuthzCallerContext.class, organizationId, environmentId, userId);
    }

    public static AuthzCallerContext ofUser(String organizationId, String environmentId, String userId) {
        return new AuthzCallerContext(organizationId, environmentId, userId);
    }

    public static AuthzCallerContext system(String environmentId) {
        return new AuthzCallerContext(SYSTEM_USER, environmentId, SYSTEM_USER);
    }

    public boolean isSystem() {
        return SYSTEM_USER.equals(userId);
    }
}
